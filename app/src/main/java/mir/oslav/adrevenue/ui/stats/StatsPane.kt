package mir.oslav.adrevenue.ui.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.cache.CachedRevenueTarget
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.model.AdMobReportRow
import mir.oslav.adrevenue.ui.PreviewContainer
import mir.oslav.adrevenue.ui.PreviewUI
import mir.oslav.adrevenue.ui.UiMessageText

@Composable
fun StatsPane(
    onAuthorizationRequested: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    var isTargetDialogVisible by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(key1 = viewModel) {
        viewModel.setPaneActive(active = true)
        onDispose {
            viewModel.setPaneActive(active = false)
        }
    }

    LaunchedEffect(
        key1 = state.value.shouldRequestAuthorization,
        key2 = state.value.period,
    ) {
        if (state.value.shouldRequestAuthorization) {
            onAuthorizationRequested()
        }
    }

    StatsPaneImpl(
        state = state.value,
        onTimeRangeSelected = viewModel::selectTimeRange,
        onPreviousPeriod = viewModel::showPreviousPeriod,
        onNextPeriod = viewModel::showNextPeriod,
        onShowTrendChanged = viewModel::setShowTrend,
        onShowBreakdownChanged = viewModel::setShowBreakdown,
        onBreakdownDimensionSelected = viewModel::selectBreakdownDimension,
        onOpenTargetSettings = { isTargetDialogVisible = true },
        modifier = modifier,
    )

    if (isTargetDialogVisible) {
        RevenueTargetDialog(
            target = state.value.revenueTarget,
            currencyCode = state.value.currencyCode,
            onSave = { amountMicros, rangeDays ->
                viewModel.setRevenueTarget(
                    amountMicros = amountMicros,
                    rangeDays = rangeDays,
                )
                isTargetDialogVisible = false
            },
            onClear = {
                viewModel.clearRevenueTarget()
                isTargetDialogVisible = false
            },
            onDismiss = { isTargetDialogVisible = false },
        )
    }
}

@Composable
private fun StatsPaneImpl(
    state: StatsUiState,
    onTimeRangeSelected: (StatisticsTimeRange) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onShowTrendChanged: (Boolean) -> Unit,
    onShowBreakdownChanged: (Boolean) -> Unit,
    onBreakdownDimensionSelected: (StatisticsBreakdownDimension) -> Unit,
    onOpenTargetSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.statistics_title),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenTargetSettings,
                            enabled = state.accountName != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(
                                    id = R.string.statistics_target_settings,
                                ),
                            )
                        }
                    },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 4.dp),
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isLoading,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = paddingValues.calculateTopPadding() + 12.dp,
                end = 20.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(state = rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    StatisticsTimeRange.entries.forEach { timeRange ->
                        FilterChip(
                            selected = state.timeRange == timeRange,
                            onClick = { onTimeRangeSelected(timeRange) },
                            enabled = state.accountName != null,
                            label = {
                                Text(
                                    text = stringResource(
                                        id = when (timeRange) {
                                            StatisticsTimeRange.Week -> R.string.statistics_range_week
                                            StatisticsTimeRange.Month -> R.string.statistics_range_month
                                            StatisticsTimeRange.Quarter -> R.string.statistics_range_quarter
                                            StatisticsTimeRange.Year -> R.string.statistics_range_year
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onPreviousPeriod,
                        enabled = state.accountName != null,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(
                                id = R.string.statistics_previous_period,
                            ),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(weight = 1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.statistics_date_range,
                                formatStatisticsDate(
                                    epochMillis = state.period.startDateEpochMillis,
                                ),
                                formatStatisticsDate(
                                    epochMillis = state.period.endDateEpochMillis,
                                ),
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        state.refreshedAtEpochMillis?.let { refreshedAt ->
                            Text(
                                text = stringResource(
                                    id = R.string.home_updated,
                                    formatStatisticsUpdatedAt(epochMillis = refreshedAt),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        onClick = onNextPeriod,
                        enabled = state.accountName != null && state.canShowNextPeriod,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = stringResource(
                                id = R.string.statistics_next_period,
                            ),
                        )
                    }
                }
            }

            item    {
                StatisticsBreakdownSelector(
                    selectedDimension = state.breakdownDimension,
                    onDimensionSelected = onBreakdownDimensionSelected,
                )
            }

            if (state.shouldShowCachedWarning) {
                item {
                    AssistChip(
                        onClick = {},
                        label = { Text(text = stringResource(id = R.string.home_cached_warning)) },
                    )
                }
            }

            state.message?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        UiMessageText(
                            message = message,
                            modifier = Modifier.padding(all = 16.dp),
                        )
                    }
                }
            }

            if (state.shouldShowOfflineEmpty) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(all = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_offline_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(id = R.string.statistics_offline_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else if (state.isLoading && !state.hasVisibleChart) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.hasVisibleChart) {
                item {
                    StatisticsRevenueChartCard(
                        bars = state.chartBars,
                        timeRange = checkNotNull(state.chartTimeRange),
                        period = checkNotNull(state.chartPeriod),
                        currencyCode = state.chartCurrencyCode,
                        showTrend = state.showTrend,
                        showBreakdown = state.showBreakdown,
                        breakdownDimension = state.breakdownDimension,
                        breakdown = state.chartBreakdown,
                        revenueTarget = state.revenueTarget,
                        onShowTrendChanged = onShowTrendChanged,
                        onShowBreakdownChanged = onShowBreakdownChanged,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.contributions.isNotEmpty()) {
                    item {
                        StatisticsContributionCard(
                            contributions = state.contributions,
                            dimension = state.breakdownDimension,
                            currencyCode = state.chartCurrencyCode,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else if (!state.shouldShowOfflineEmpty && state.accountName != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.statistics_no_data),
                            modifier = Modifier.padding(all = 18.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@PreviewUI
@Composable
private fun StatsPanePreview() {
    val previewBars = listOf(
        1_787_126_400_000L to 2_020_000L,
        1_787_212_800_000L to 2_680_000L,
        1_787_299_200_000L to 1_580_000L,
        1_787_385_600_000L to 2_140_000L,
        1_787_472_000_000L to 1_760_000L,
        1_787_558_400_000L to 2_430_000L,
        1_787_644_800_000L to 1_920_000L,
    ).map { (date, revenue) ->
        StatisticsChartBar(
            startDateEpochMillis = date,
            endDateEpochMillis = date,
            estimatedEarningsMicros = revenue,
        )
    }
    val firstAppEarnings = previewBars.sumOf { bar ->
        bar.estimatedEarningsMicros * 3L / 5L
    }
    val secondAppEarnings = previewBars.sumOf { bar ->
        bar.estimatedEarningsMicros - bar.estimatedEarningsMicros * 3L / 5L
    }
    val previewBreakdown = StatisticsChartBreakdown(
        dimension = StatisticsBreakdownDimension.App,
        series = listOf(
            StatisticsChartSeries(
                key = "app-1",
                label = "Revenue App",
                estimatedEarningsMicros = firstAppEarnings,
            ),
            StatisticsChartSeries(
                key = "app-2",
                label = "Fitness App",
                estimatedEarningsMicros = secondAppEarnings,
            ),
        ),
        bars = previewBars.map { bar ->
            val firstApp = bar.estimatedEarningsMicros * 3L / 5L
            StatisticsStackedChartBar(
                startDateEpochMillis = bar.startDateEpochMillis,
                endDateEpochMillis = bar.endDateEpochMillis,
                earningsMicrosBySeries = mapOf(
                    "app-1" to firstApp,
                    "app-2" to bar.estimatedEarningsMicros - firstApp,
                ),
            )
        },
    )

    PreviewContainer {
        StatsPaneImpl(
            state = StatsUiState(
                period = StatisticsPeriod(
                    startDateEpochMillis = 1_787_126_400_000,
                    endDateEpochMillis = 1_787_644_800_000,
                ),
                accountName = "accounts/pub-1234567890123456",
                currencyCode = "USD",
                report = AdMobNetworkReport(
                    currencyCode = "USD",
                    dateRangeLabel = "Aug 19, 2026 - Aug 25, 2026",
                    matchingRowCount = 7,
                    rows = previewBars.map { bar ->
                        AdMobReportRow(
                            dateLabel = formatStatisticsBarLabel(
                                bar = bar,
                                timeRange = StatisticsTimeRange.Week,
                            ),
                            estimatedEarningsMicros = bar.estimatedEarningsMicros,
                            impressions = null,
                            clicks = null,
                            matchRate = null,
                            impressionRpmMicros = null,
                            dateEpochMillis = bar.startDateEpochMillis,
                        )
                    },
                ),
                chartBars = previewBars,
                chartTimeRange = StatisticsTimeRange.Week,
                chartPeriod = StatisticsPeriod(
                    startDateEpochMillis = 1_787_126_400_000,
                    endDateEpochMillis = 1_787_644_800_000,
                ),
                chartCurrencyCode = "USD",
                chartBreakdown = previewBreakdown,
                revenueTarget = CachedRevenueTarget(
                    accountName = "accounts/pub-1234567890123456",
                    amountMicros = 30_000_000,
                    rangeDays = 30,
                ),
                showTrend = true,
                showBreakdown = true,
                contributions = listOf(
                    StatisticsContribution(
                        label = "Revenue App",
                        estimatedEarningsMicros = firstAppEarnings,
                        colorIndex = 0,
                    ),
                    StatisticsContribution(
                        label = "Fitness App",
                        estimatedEarningsMicros = secondAppEarnings,
                        colorIndex = 1,
                    ),
                ),
            ),
            onTimeRangeSelected = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onShowTrendChanged = {},
            onShowBreakdownChanged = {},
            onBreakdownDimensionSelected = {},
            onOpenTargetSettings = {},
        )
    }
}
