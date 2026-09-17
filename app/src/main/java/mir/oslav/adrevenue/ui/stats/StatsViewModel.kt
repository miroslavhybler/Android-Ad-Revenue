package mir.oslav.adrevenue.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mir.oslav.adrevenue.core.network.NetworkMonitor
import mir.oslav.adrevenue.data.AdMobRepository
import mir.oslav.adrevenue.data.auth.AdMobAuthSession
import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot
import mir.oslav.adrevenue.data.cache.CachedRevenueTarget
import mir.oslav.adrevenue.data.cache.CachedStatisticsReport
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.network.PlayStoreIconResolver
import mir.oslav.adrevenue.ui.UiMessage
import mir.oslav.adrevenue.ui.UiMessageKind

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: AdMobRepository,
    private val authSession: AdMobAuthSession,
    networkMonitor: NetworkMonitor,
    private val iconResolver: PlayStoreIconResolver,
) : ViewModel() {
    private val selection = MutableStateFlow(StatisticsSelection())
    private val operationState = MutableStateFlow(StatisticsOperationState())
    private val isPaneActive = MutableStateFlow(false)
    private val isOnline = networkMonitor.isOnline
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
        )

    private val initialUiState = StatsUiState(
        period = selection.value.toPeriod(),
    )
    private val baseUiState = combine(
        flow = repository.snapshot,
        flow2 = isOnline,
        flow3 = selection,
        flow4 = operationState,
    ) { snapshot, online, selectedPeriod, operation ->
        val period = selectedPeriod.toPeriod()
        val cachedReport = snapshot.findStatisticsReport(period = period)
        val reportRows = cachedReport?.report?.rows.orEmpty()
        val appMetadata = snapshot.apps.associateBy { app -> app.appId }
        val appContributions = reportRows
            .asSequence()
            .filter { row -> row.appName != null && (row.estimatedEarningsMicros ?: 0L) > 0L }
            .groupBy { row -> row.appName!! }
            .map { (appName, rows) ->
                StatisticsAppContribution(
                    appName = appName,
                    appStoreId = rows.firstNotNullOfOrNull { row -> appMetadata[row.appId]?.appStoreId },
                    iconUrl = rows.firstNotNullOfOrNull { row -> snapshot.appIconUrls[row.appId] },
                    estimatedEarningsMicros = rows.sumOf { row ->
                        row.estimatedEarningsMicros ?: 0L
                    },
                )
            }
            .sortedByDescending { contribution -> contribution.estimatedEarningsMicros }
            .toList()
        val currencyCode = cachedReport?.report?.currencyCode
            ?.takeIf { code -> code.isNotBlank() }
            ?: snapshot.selectedAccount?.currencyCode.orEmpty()
        StatsUiState(
            timeRange = selectedPeriod.timeRange,
            periodOffset = selectedPeriod.periodOffset,
            period = period,
            showTrend = selectedPeriod.showTrend,
            isOnline = online,
            isLoading = operation.isLoading,
            needsAuthorization = operation.needsAuthorization,
            message = operation.message,
            accountName = snapshot.selectedAccountName,
            currencyCode = currencyCode,
            report = cachedReport?.report,
            refreshedAtEpochMillis = cachedReport?.refreshedAtEpochMillis,
            chartBars = reportRows
                .takeIf { rows -> rows.isNotEmpty() }
                ?.let { nonEmptyRows ->
                    buildStatisticsChartBars(
                        rows = nonEmptyRows,
                        timeRange = selectedPeriod.timeRange,
                        period = period,
                    )
                }
                .orEmpty(),
            chartTimeRange = selectedPeriod.timeRange.takeIf { reportRows.isNotEmpty() },
            chartPeriod = period.takeIf { reportRows.isNotEmpty() },
            chartCurrencyCode = currencyCode,
            appContributions = appContributions,
            revenueTarget = snapshot.findRevenueTarget(),
        )
    }

    val uiState = baseUiState
        .runningFold(initial = initialUiState) { previous, current ->
            current.retainChartWhileLoading(previous = previous)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = initialUiState,
        )

    init {
        viewModelScope.launch {
            isPaneActive
                .collect { active ->
                    if (!active) {
                        operationState.update {
                            it.copy(
                                isLoading = false,
                                needsAuthorization = false,
                            )
                        }
                    }
                }
        }

        viewModelScope.launch {
            combine(
                selection
                    .map { selectedPeriod -> selectedPeriod.toPeriod() }
                    .distinctUntilChanged(),
                repository.snapshot
                    .map { snapshot -> snapshot.selectedAccountName }
                    .distinctUntilChanged(),
                authSession.accessToken,
                isOnline,
                isPaneActive,
            ) { period, accountName, accessToken, online, paneActive ->
                StatisticsLoadRequest(
                    period = period,
                    accountName = accountName,
                    accessToken = accessToken,
                    isOnline = online,
                    isPaneActive = paneActive,
                )
            }
                .filter { request -> request.isPaneActive }
                .distinctUntilChanged()
                .collectLatest { request ->
                    when {
                        request.accountName == null -> operationState.value =
                            StatisticsOperationState()

                        !request.isOnline -> operationState.update {
                            it.copy(
                                isLoading = false,
                                needsAuthorization = false,
                                message = null,
                            )
                        }

                        request.accessToken.isNullOrBlank() -> operationState.update {
                            it.copy(
                                isLoading = it.isAuthorizing,
                                needsAuthorization = !it.isAuthorizing,
                                message = null,
                            )
                        }

                        else -> refreshReport(request = request)
                    }
                }
        }
    }

    fun setPaneActive(active: Boolean) {
        isPaneActive.value = active
    }

    fun selectTimeRange(timeRange: StatisticsTimeRange) {
        if (selection.value.timeRange == timeRange && selection.value.periodOffset == 0) return
        operationState.update { it.copy(isLoading = isOnline.value, message = null) }
        selection.update {
            it.copy(
                timeRange = timeRange,
                periodOffset = 0,
                anchorEpochMillis = System.currentTimeMillis(),
            )
        }
    }

    fun showPreviousPeriod() {
        operationState.update { it.copy(isLoading = isOnline.value, message = null) }
        selection.update { it.copy(periodOffset = it.periodOffset - 1) }
    }

    fun showNextPeriod() {
        if (selection.value.periodOffset >= 0) return
        operationState.update { it.copy(isLoading = isOnline.value, message = null) }
        selection.update { it.copy(periodOffset = it.periodOffset + 1) }
    }

    fun setShowTrend(showTrend: Boolean) {
        selection.update { it.copy(showTrend = showTrend) }
    }

    fun setRevenueTarget(amountMicros: Long, rangeDays: Int) {
        val accountName = repository.snapshot.value.selectedAccountName ?: return
        if (amountMicros <= 0L || rangeDays !in
            MIN_REVENUE_TARGET_RANGE_DAYS..MAX_REVENUE_TARGET_RANGE_DAYS
        ) {
            return
        }

        viewModelScope.launch {
            repository.setRevenueTarget(
                accountName = accountName,
                amountMicros = amountMicros,
                rangeDays = rangeDays,
            )
        }
    }

    fun clearRevenueTarget() {
        val accountName = repository.snapshot.value.selectedAccountName ?: return
        viewModelScope.launch {
            repository.clearRevenueTarget(accountName = accountName)
        }
    }

    fun authorizationStarted() {
        operationState.update {
            it.copy(
                isLoading = true,
                isAuthorizing = true,
                needsAuthorization = false,
                message = null,
            )
        }
    }

    fun accessTokenAvailable(token: String) {
        authSession.setAccessToken(accessToken = token)
        operationState.update {
            it.copy(
                isLoading = true,
                isAuthorizing = false,
                needsAuthorization = false,
                message = null,
            )
        }
    }

    fun authorizationFailed(throwable: Throwable) {
        operationState.update {
            it.copy(
                isLoading = false,
                isAuthorizing = false,
                needsAuthorization = false,
                message = UiMessage.fromThrowable(
                    throwable = throwable,
                    fallbackKind = UiMessageKind.AuthorizationFailed,
                ),
            )
        }
    }

    private suspend fun refreshReport(request: StatisticsLoadRequest) {
        val accountName = checkNotNull(value = request.accountName)
        val accessToken = checkNotNull(value = request.accessToken)
        operationState.update {
            it.copy(
                isLoading = true,
                isAuthorizing = false,
                needsAuthorization = false,
                message = null,
            )
        }

        try {
            request.accountName.let { account ->
                repository.snapshot.value.apps
                    .filter { app ->
                        val hasStoreId = app.appStoreId != null
                        val isUrlNull = repository.snapshot.value.appIconUrls[app.appId] == null
                        hasStoreId && isUrlNull
                    }
                    .forEach { app ->
                        iconResolver.resolve(app.appStoreId!!)
                            ?.let { url -> repository.saveAppIconUrl(app.appId, url) }
                    }
            }
            withContext(Dispatchers.IO) {
                repository.refreshStatisticsReport(
                    accessToken = accessToken,
                    accountName = accountName,
                    startDateEpochMillis = request.period.startDateEpochMillis,
                    endDateEpochMillis = request.period.endDateEpochMillis,
                )
            }
            operationState.update {
                it.copy(
                    isLoading = false,
                    isAuthorizing = false,
                    message = null,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            operationState.update {
                it.copy(
                    isLoading = false,
                    isAuthorizing = false,
                    message = UiMessage.fromThrowable(
                        throwable = throwable,
                        fallbackKind = UiMessageKind.RefreshFailed,
                    ),
                )
            }
        }
    }
}

data class StatsUiState(
    val timeRange: StatisticsTimeRange = StatisticsTimeRange.Week,
    val periodOffset: Int = 0,
    val period: StatisticsPeriod,
    val showTrend: Boolean = false,
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val needsAuthorization: Boolean = false,
    val message: UiMessage? = null,
    val accountName: String? = null,
    val currencyCode: String = "",
    val report: AdMobNetworkReport? = null,
    val refreshedAtEpochMillis: Long? = null,
    val chartBars: List<StatisticsChartBar> = emptyList(),
    val chartTimeRange: StatisticsTimeRange? = null,
    val chartPeriod: StatisticsPeriod? = null,
    val chartCurrencyCode: String = "",
    val revenueTarget: CachedRevenueTarget? = null,
    val appContributions: List<StatisticsAppContribution> = emptyList(),
    val isShowingPreviousChart: Boolean = false,
) {
    val canShowNextPeriod: Boolean
        get() = periodOffset < 0

    val hasCachedData: Boolean
        get() = report != null

    val hasVisibleChart: Boolean
        get() = chartBars.isNotEmpty() && chartTimeRange != null && chartPeriod != null

    val shouldRequestAuthorization: Boolean
        get() = needsAuthorization && isOnline && accountName != null

    val shouldShowOfflineEmpty: Boolean
        get() = !isOnline && !hasCachedData

    val shouldShowCachedWarning: Boolean
        get() = !isOnline && hasCachedData
}

data class StatisticsAppContribution(
    val appName: String,
    val appStoreId: String? = null,
    val iconUrl: String? = null,
    val estimatedEarningsMicros: Long,
)

private data class StatisticsSelection(
    val timeRange: StatisticsTimeRange = StatisticsTimeRange.Week,
    val periodOffset: Int = 0,
    val anchorEpochMillis: Long = System.currentTimeMillis(),
    val showTrend: Boolean = false,
) {
    fun toPeriod(): StatisticsPeriod = calculateStatisticsPeriod(
        timeRange = timeRange,
        periodOffset = periodOffset,
        anchorEpochMillis = anchorEpochMillis,
    )
}

private data class StatisticsOperationState(
    val isLoading: Boolean = false,
    val isAuthorizing: Boolean = false,
    val needsAuthorization: Boolean = false,
    val message: UiMessage? = null,
)

private data class StatisticsLoadRequest(
    val period: StatisticsPeriod,
    val accountName: String?,
    val accessToken: String?,
    val isOnline: Boolean,
    val isPaneActive: Boolean,
)

private fun CachedAdMobSnapshot.findStatisticsReport(
    period: StatisticsPeriod,
): CachedStatisticsReport? {
    val accountName = selectedAccountName ?: return null
    return statisticsReports.firstOrNull { cached ->
        cached.accountName == accountName &&
                cached.startDateEpochMillis == period.startDateEpochMillis &&
                cached.endDateEpochMillis == period.endDateEpochMillis
    }
}

private fun CachedAdMobSnapshot.findRevenueTarget(): CachedRevenueTarget? {
    val accountName = selectedAccountName ?: return null
    return revenueTargets.firstOrNull { target -> target.accountName == accountName }
}

internal fun StatsUiState.retainChartWhileLoading(previous: StatsUiState): StatsUiState {
    if (
        !isLoading ||
        hasVisibleChart ||
        !previous.hasVisibleChart ||
        accountName == null ||
        accountName != previous.accountName
    ) {
        return this
    }

    return copy(
        chartBars = previous.chartBars,
        chartTimeRange = previous.chartTimeRange,
        chartPeriod = previous.chartPeriod,
        chartCurrencyCode = previous.chartCurrencyCode,
        isShowingPreviousChart = true,
    )
}
