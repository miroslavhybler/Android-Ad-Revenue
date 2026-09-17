package mir.oslav.adrevenue.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot
import mir.oslav.adrevenue.data.cache.CachedRevenueTarget
import mir.oslav.adrevenue.data.cache.CachedStatisticsReport
import mir.oslav.adrevenue.data.cache.CacheConstants
import mir.oslav.adrevenue.data.cache.PersistentAdMobCache
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.SignedInUser
import mir.oslav.adrevenue.data.network.AdMobApiClient

@Singleton
class AdMobRepository @Inject constructor(
    private val apiClient: AdMobApiClient,
    private val cache: PersistentAdMobCache,
) {
    private val _snapshot = MutableStateFlow(CachedAdMobSnapshot())
    val snapshot: StateFlow<CachedAdMobSnapshot> = _snapshot.asStateFlow()

    suspend fun restore() {
        saveInMemory(cache.read().withNormalizedSelection())
    }

    suspend fun discoverAccounts(
        signedInUser: SignedInUser,
        accessToken: String,
    ) {
        val accounts = apiClient.listAccounts(accessToken)
        val apps = accounts.flatMap { account ->
            apiClient.listApps(
                accessToken = accessToken,
                accountName = account.name,
            )
        }

        savePersistent(
            snapshot = CachedAdMobSnapshot(
                signedInUser = signedInUser,
                accounts = accounts,
                apps = apps,
                selectedAccountName = null,
                report = null,
                refreshedAtEpochMillis = null,
            ),
        )
    }

    suspend fun refresh(accessToken: String) {
        val current = _snapshot.value
        val signedInUser =
            current.signedInUser ?: throw IllegalStateException("The user is not signed in.")
        val accounts = apiClient.listAccounts(accessToken)
        val selectedAccount = chooseSelectedAccount(
            accounts = accounts,
            preferredAccountName = current.selectedAccountName,
        )
        val apps = selectedAccount?.let {
            apiClient.listApps(
                accessToken = accessToken,
                accountName = it.name
            )
        }.orEmpty()
        val report = selectedAccount?.let { account ->
            apiClient.generateNetworkReport(
                accessToken = accessToken,
                accountName = account.name,
            )
        }

        savePersistent(
            snapshot = current.copy(
                signedInUser = signedInUser,
                accounts = accounts,
                apps = apps,
                selectedAccountName = selectedAccount?.name,
                report = report,
                refreshedAtEpochMillis = report?.let { System.currentTimeMillis() },
            ),
        )
    }

    suspend fun selectAccount(
        accountName: String,
        accessToken: String,
    ) {
        val current = _snapshot.value
        val account = current.accounts.firstOrNull { it.name == accountName }
            ?: throw IllegalArgumentException("Unknown AdMob account.")
        val report = apiClient.generateNetworkReport(
            accessToken = accessToken,
            accountName = account.name,
        )
        val apps = apiClient.listApps(accessToken = accessToken, accountName = account.name)

        savePersistent(
            snapshot = current.copy(
                selectedAccountName = account.name,
                apps = apps,
                report = report,
                refreshedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun refreshStatisticsReport(
        accessToken: String,
        accountName: String,
        startDateEpochMillis: Long,
        endDateEpochMillis: Long,
    ): CachedStatisticsReport {
        val accountExists = _snapshot.value.accounts.any { account -> account.name == accountName }
        require(value = accountExists) { "Unknown AdMob account." }

        val report = apiClient.generateNetworkReport(
            accessToken = accessToken,
            accountName = accountName,
            startDateEpochMillis = startDateEpochMillis,
            endDateEpochMillis = endDateEpochMillis,
            includeAppDimension = true,
        )
        val cachedReport = CachedStatisticsReport(
            accountName = accountName,
            startDateEpochMillis = startDateEpochMillis,
            endDateEpochMillis = endDateEpochMillis,
            report = report,
            refreshedAtEpochMillis = System.currentTimeMillis(),
        )
        val current = _snapshot.value
        val updatedReports = buildList {
            add(element = cachedReport)
            addAll(
                current.statisticsReports.filterNot { cached ->
                    cached.accountName == accountName &&
                            cached.startDateEpochMillis == startDateEpochMillis &&
                            cached.endDateEpochMillis == endDateEpochMillis
                },
            )
        }.take(n = CacheConstants.MaxStatisticsReports)

        savePersistent(current.copy(statisticsReports = updatedReports))
        return cachedReport
    }

    suspend fun saveAppIconUrl(appId: String, url: String) {
        savePersistent(
            snapshot =
                _snapshot.value.copy(appIconUrls = _snapshot.value.appIconUrls + (appId to url))
        )
    }

    suspend fun setRevenueTarget(
        accountName: String,
        amountMicros: Long,
        rangeDays: Int,
    ) {
        require(value = amountMicros > 0L) { "Revenue target must be greater than zero." }
        require(value = rangeDays > 0) { "Revenue target range must be greater than zero." }

        val current = _snapshot.value
        require(value = current.accounts.any { account -> account.name == accountName }) {
            "Unknown AdMob account."
        }
        val target = CachedRevenueTarget(
            accountName = accountName,
            amountMicros = amountMicros,
            rangeDays = rangeDays,
        )
        val updatedTargets = buildList {
            add(element = target)
            addAll(elements = current.revenueTargets.filterNot { cached -> cached.accountName == accountName })
        }

        savePersistent(snapshot = current.copy(revenueTargets = updatedTargets))
    }

    suspend fun clearRevenueTarget(accountName: String) {
        val current = _snapshot.value
        savePersistent(
            current.copy(
                revenueTargets = current.revenueTargets.filterNot { target ->
                    target.accountName == accountName
                },
            ),
        )
    }

    suspend fun signOut() {
        cache.clear()
        saveInMemory(CachedAdMobSnapshot())
    }

    private suspend fun savePersistent(snapshot: CachedAdMobSnapshot) {
        val normalizedSnapshot = snapshot.withNormalizedSelection()
        cache.write(normalizedSnapshot)
        saveInMemory(normalizedSnapshot)
    }

    private fun saveInMemory(snapshot: CachedAdMobSnapshot) {
        _snapshot.value = snapshot
    }

    private fun chooseSelectedAccount(
        accounts: List<AdMobAccount>,
        preferredAccountName: String?,
    ): AdMobAccount? =
        preferredAccountName
            ?.let { preferred -> accounts.firstOrNull { it.name == preferred } }
            ?: accounts.singleOrNull()

    private fun CachedAdMobSnapshot.withNormalizedSelection(): CachedAdMobSnapshot {
        val selected = selectedAccountName
            ?.let { selected -> accounts.firstOrNull { it.name == selected } }

        return copy(selectedAccountName = selected?.name)
    }
}
