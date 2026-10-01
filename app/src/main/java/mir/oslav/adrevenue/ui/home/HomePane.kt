package mir.oslav.adrevenue.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jet.utils.copy
import com.jet.utils.plus
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.model.AdMobReportRow
import mir.oslav.adrevenue.data.model.SignedInUser
import mir.oslav.adrevenue.ui.Destination
import mir.oslav.adrevenue.ui.PreviewUI
import mir.oslav.adrevenue.ui.UiMessageText
import mir.oslav.adrevenue.ui.AdRevenueTheme
import mir.oslav.adrevenue.ui.PreviewContainer

@Composable
fun HomePane(
    onNavigate: (Destination) -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    HomePaneImpl(
        state = state.value,
        onNavigate = onNavigate,
        onRefreshRequested = onRefreshClick,
        modifier = modifier,
    )
}

@Composable
private fun HomePaneImpl(
    state: HomeUiState,
    onNavigate: (Destination) -> Unit,
    onRefreshRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(key1 = state.signedInUser?.id, key2 = state.isOnline) {
        if (state.shouldShowHome && state.isOnline && !state.isRefreshing) {
            onRefreshRequested()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(weight = 1f),
                        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(size = 32.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = state.signedInUser?.displayName
                                    ?.let { stringResource(id = R.string.home_connected_as, it) }
                                    ?: stringResource(id = R.string.home_connected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    TextButton(onClick = { onNavigate(Destination.SettingsAndAbout) }) {
                        Text(text = stringResource(id = R.string.home_about))
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)

                ) {
                    Text(
                        modifier = Modifier,
                        text = stringResource(id = R.string.not_affiliated_with_google),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            isRefreshing = state.isRefreshing,
            onRefresh = onRefreshRequested,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp,
                    bottom = 24.dp,
                ) + paddingValues.copy(top = 0.dp),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            ) {
                if (state.shouldShowNoConnectionEmpty) {
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
                                    text = stringResource(R.string.home_offline_empty_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }

                if (state.shouldShowCachedWarning) {
                    item {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.home_cached_warning)) },
                        )
                    }
                }

                state.message?.let { message ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            UiMessageText(
                                message = message,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }

                state.report?.let { report ->
                    val orderedRows = report.rows.sortedWith(
                        compareByDescending<AdMobReportRow> { it.dateEpochMillis ?: Long.MIN_VALUE }
                            .thenByDescending { it.dateLabel },
                    )

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
                                Text(
                                    text = stringResource(id = R.string.home_last_30_days),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = report.dateRangeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                state.refreshedAtEpochMillis?.let { refreshedAt ->
                                    Text(
                                        text = stringResource(
                                            id = R.string.home_updated,
                                            formatUpdatedAt(epochMillis = refreshedAt)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.horizontalScroll(state = rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
                            ) {
                                ElevatedCard(modifier = Modifier.width(width = 172.dp)) {
                                    Column(
                                        modifier = Modifier.padding(all = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.home_metric_estimated_earnings),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = formatMoney(
                                                report.totalEstimatedEarningsMicros,
                                                report.currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                ElevatedCard(modifier = Modifier.width(172.dp)) {
                                    Column(
                                        modifier = Modifier.padding(all = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.home_metric_impressions),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = formatWholeNumber(value = report.totalImpressions),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                ElevatedCard(modifier = Modifier.width(width = 172.dp)) {
                                    Column(
                                        modifier = Modifier.padding(all = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.home_metric_clicks),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = formatWholeNumber(value = report.totalClicks),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                ElevatedCard(modifier = Modifier.width(172.dp)) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.home_metric_match_rate),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = formatPercent(report.averageMatchRate),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                                ElevatedCard(modifier = Modifier.width(172.dp)) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.home_metric_ecpm),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = formatMoney(
                                                report.averageImpressionRpmMicros,
                                                report.currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(orderedRows) { row ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(all = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = row.dateLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = formatMoney(
                                            row.estimatedEarningsMicros,
                                            report.currencyCode
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.weight(weight = 1f),
                                        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.home_metric_impressions),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(text = formatWholeNumber(value = row.impressions))
                                    }
                                    Column(
                                        modifier = Modifier.weight(weight = 1f),
                                        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.home_metric_clicks),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(text = formatWholeNumber(value = row.clicks))
                                    }
                                    Column(
                                        modifier = Modifier.weight(weight = 1f),
                                        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.home_stat_match),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(text = formatPercent(value = row.matchRate))
                                    }
                                }
                            }
                        }
                    }
                } ?: item {
                    if (!state.shouldShowNoConnectionEmpty) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(all = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(id = R.string.home_no_report_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(id = R.string.home_no_report_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@PreviewUI
@Composable
private fun HomePanePreview() {
    PreviewContainer {
        HomePaneImpl(
            state = HomeUiState(
                snapshot = CachedAdMobSnapshot(
                    signedInUser = SignedInUser(id = "user@example.com", displayName = "John Doe"),
                    accounts = listOf(
                        AdMobAccount(
                            name = "accounts/pub-1234567890123456",
                            publisherId = "pub-1234567890123456",
                            reportingTimeZone = "America/Los_Angeles",
                            currencyCode = "USD",
                        ),
                    ),
                    selectedAccountName = "accounts/pub-1234567890123456",
                    refreshedAtEpochMillis = 1_787_683_200_000,
                    report = AdMobNetworkReport(
                        currencyCode = "USD",
                        dateRangeLabel = "Jul 27, 2026 - Aug 25, 2026",
                        matchingRowCount = 7,
                        rows = listOf(
                            AdMobReportRow(
                                dateLabel = "Aug 25",
                                estimatedEarningsMicros = 1_920_000,
                                impressions = 11_830,
                                clicks = 264,
                                matchRate = 0.91,
                                impressionRpmMicros = 162_000,
                                dateEpochMillis = 1_787_644_800_000,
                            ),
                            AdMobReportRow(
                                dateLabel = "Aug 24",
                                estimatedEarningsMicros = 2_430_000,
                                impressions = 14_100,
                                clicks = 312,
                                matchRate = 0.94,
                                impressionRpmMicros = 172_000,
                                dateEpochMillis = 1_787_558_400_000,
                            ),
                            AdMobReportRow(
                                dateLabel = "Aug 23",
                                estimatedEarningsMicros = 1_760_000,
                                impressions = 10_920,
                                clicks = 231,
                                matchRate = 0.90,
                                impressionRpmMicros = 161_000,
                                dateEpochMillis = 1_787_472_000_000,
                            ),
                            AdMobReportRow(
                                dateLabel = "Aug 22",
                                estimatedEarningsMicros = 2_140_000,
                                impressions = 12_640,
                                clicks = 278,
                                matchRate = 0.93,
                                impressionRpmMicros = 169_000,
                                dateEpochMillis = 1_787_385_600_000,
                            ),
                            AdMobReportRow(
                                dateLabel = "Aug 21",
                                estimatedEarningsMicros = 1_580_000,
                                impressions = 9_870,
                                clicks = 204,
                                matchRate = 0.89,
                                impressionRpmMicros = 160_000,
                                dateEpochMillis = 1_787_299_200_000,
                            ),
                            AdMobReportRow(
                                dateLabel = "Aug 20",
                                estimatedEarningsMicros = 2_680_000,
                                impressions = 15_240,
                                clicks = 337,
                                matchRate = 0.95,
                                impressionRpmMicros = 176_000,
                                dateEpochMillis = 1_787_212_800_000,
                            ),
                            AdMobReportRow(
                                dateLabel = "Aug 19",
                                estimatedEarningsMicros = 2_020_000,
                                impressions = 12_110,
                                clicks = 259,
                                matchRate = 0.92,
                                impressionRpmMicros = 167_000,
                                dateEpochMillis = 1_787_126_400_000,
                            ),
                        ),
                    ),
                ),
            ),
            onNavigate = {},
            onRefreshRequested = {},
        )
    }
}

private fun formatMoney(micros: Long?, currencyCode: String): String =
    micros?.toDouble()?.let { formatMoney(it, currencyCode) } ?: "-"

private fun formatMoney(micros: Double?, currencyCode: String): String {
    if (micros == null) return "-"
    val amount = micros / 1_000_000.0
    val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)
    return listOf(currencyCode.takeIf { it.isNotBlank() }, formatted)
        .filterNotNull()
        .joinToString(" ")
}

private fun formatWholeNumber(value: Long?): String =
    value?.let { NumberFormat.getIntegerInstance(Locale.getDefault()).format(it) } ?: "-"

private fun formatPercent(value: Double?): String {
    if (value == null) return "-"
    val percentValue = if (value <= 1.0) value * 100.0 else value
    return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(percentValue) + "%"
}

private fun formatUpdatedAt(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(epochMillis))
