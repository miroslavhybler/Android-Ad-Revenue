package mir.oslav.adrevenue.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.cache.CachedRevenueTarget

@Composable
internal fun StatisticsRevenueChartCard(
    bars: List<StatisticsChartBar>,
    timeRange: StatisticsTimeRange,
    period: StatisticsPeriod,
    currencyCode: String,
    showTrend: Boolean,
    revenueTarget: CachedRevenueTarget?,
    onShowTrendChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val resources = LocalResources.current
    val trendColor = MaterialTheme.colorScheme.tertiary
    val targetColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val chartData = remember(bars, timeRange, barColor) {
        bars.map { bar ->
            val label = formatStatisticsBarLabel(bar = bar, timeRange = timeRange)
            Bars(
                label = label,
                values = listOf(
                    Bars.Data(
                        label = label,
                        value = bar.estimatedEarningsMicros / MICROS_PER_CURRENCY_UNIT,
                        color = SolidColor(value = barColor),
                    ),
                ),
            )
        }
    }
    val revenueValues = remember(chartData) {
        chartData.flatMap { chartBars -> chartBars.values }.map { data -> data.value }
    }
    val targetProjection = remember(revenueTarget, timeRange, period, bars.size) {
        revenueTarget?.let { configuredTarget ->
            projectRevenueTarget(
                target = configuredTarget,
                timeRange = timeRange,
                period = period,
                barCount = bars.size,
            )
        }
    }
    val targetLineAmount = targetProjection?.chartLineAmountMicros
        ?.div(MICROS_PER_CURRENCY_UNIT)
    val chartBoundValues = revenueValues + listOfNotNull(targetLineAmount)
    val minValue = minOf(0.0, chartBoundValues.minOrNull() ?: 0.0)
    val maxValue = maxOf(0.0, chartBoundValues.maxOrNull() ?: 0.0)
        .takeUnless { value -> value == 0.0 && minValue == 0.0 }
        ?: EMPTY_CHART_MAX_VALUE
    val trendAmount = revenueValues.takeIf { values -> values.isNotEmpty() }?.average() ?: 0.0
    val totalMicros = bars.sumOf { bar -> bar.estimatedEarningsMicros }
    val dataDescription = bars.joinToString(separator = ", ") { bar ->
        val label = formatStatisticsBarLabel(bar = bar, timeRange = timeRange)
        resources.getString(
            R.string.statistics_chart_data_point,
            label,
            formatStatisticsMoney(bar.estimatedEarningsMicros, currencyCode),
        )
    }
    val revenueContentDescription = stringResource(
        id = R.string.statistics_chart_content_description,
        dataDescription,
    )
    val targetContentDescription = if (targetProjection == null) {
        null
    } else {
        stringResource(
            id = R.string.statistics_chart_target_content_description,
            formatStatisticsMoney(
                micros = targetProjection.chartLineAmountMicros,
                currencyCode = currencyCode,
            ),
        )
    }
    val chartContentDescription = listOfNotNull(
        revenueContentDescription,
        targetContentDescription,
    ).joinToString(separator = " ")

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.home_metric_estimated_earnings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatStatisticsMoney(totalMicros, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = STATISTICS_CHART_HEIGHT)
                    .semantics { contentDescription = chartContentDescription },
            ) {
                ColumnChart(
                    data = chartData,
                    modifier = Modifier.matchParentSize(),
                    barProperties = BarProperties(
                        thickness = statisticsBarThickness(barCount = bars.size),
                        spacing = STATISTICS_CHART_BAR_SPACING,
                        cornerRadius = Bars.Data.Radius.Rectangle(
                            topLeft = STATISTICS_CHART_BAR_RADIUS,
                            topRight = STATISTICS_CHART_BAR_RADIUS,
                        ),
                    ),
                    labelProperties = LabelProperties(
                        enabled = true,
                        textStyle = MaterialTheme.typography.labelSmall.copy(color = contentColor),
                        padding = STATISTICS_CHART_LABEL_PADDING,
                    ),
                    labelHelperProperties = LabelHelperProperties(enabled = false),
                    indicatorProperties = HorizontalIndicatorProperties(
                        textStyle = MaterialTheme.typography.labelSmall.copy(color = contentColor),
                        count = IndicatorCount.CountBased(
                            count = STATISTICS_CHART_INDICATOR_COUNT,
                        ),
                        contentBuilder = ::formatStatisticsChartAmount,
                    ),
                    gridProperties = GridProperties(
                        xAxisProperties = GridProperties.AxisProperties(
                            color = SolidColor(value = gridColor),
                            lineCount = STATISTICS_CHART_INDICATOR_COUNT,
                        ),
                        yAxisProperties = GridProperties.AxisProperties(enabled = false),
                    ),
                    zeroLineProperties = ZeroLineProperties(
                        enabled = minValue < 0.0,
                        color = SolidColor(value = gridColor),
                    ),
                    popupProperties = PopupProperties(
                        contentBuilder = { popup ->
                            formatStatisticsMoney(
                                micros = popup.value * MICROS_PER_CURRENCY_UNIT,
                                currencyCode = currencyCode,
                            )
                        },
                    ),
                    minValue = minValue,
                    maxValue = maxValue,
                )

                if (showTrend || targetProjection != null) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = STATISTICS_CHART_PLOT_HEIGHT)
                            .padding(start = STATISTICS_CHART_INDICATOR_PADDING),
                    ) {
                        targetLineAmount?.let { amount ->
                            drawStatisticsHorizontalLine(
                                value = amount,
                                minValue = minValue,
                                maxValue = maxValue,
                                color = targetColor,
                                strokeWidth = STATISTICS_TARGET_LINE_WIDTH,
                            )
                        }

                        if (showTrend) {
                            drawStatisticsHorizontalLine(
                                value = trendAmount,
                                minValue = minValue,
                                maxValue = maxValue,
                                color = trendColor,
                                strokeWidth = STATISTICS_TREND_LINE_WIDTH,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(
                                        STATISTICS_TREND_DASH_LENGTH.toPx(),
                                        STATISTICS_TREND_DASH_GAP.toPx(),
                                    ),
                                ),
                            )
                        }
                    }
                }
            }

            targetProjection?.let { projection ->
                val timeRangeLabel = stringResource(
                    id = when (timeRange) {
                        StatisticsTimeRange.Week -> R.string.statistics_range_week
                        StatisticsTimeRange.Month -> R.string.statistics_range_month
                        StatisticsTimeRange.Quarter -> R.string.statistics_range_quarter
                        StatisticsTimeRange.Year -> R.string.statistics_range_year
                    },
                )
                Column(verticalArrangement = Arrangement.spacedBy(space = 2.dp)) {
                    Text(
                        text = pluralStringResource(
                            id = R.plurals.statistics_target_configured,
                            count = projection.configuredRangeDays,
                            formatStatisticsMoney(
                                micros = projection.configuredAmountMicros,
                                currencyCode = currencyCode,
                            ),
                            projection.configuredRangeDays,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                    )
                    Text(
                        text = stringResource(
                            id = R.string.statistics_target_period_summary,
                            timeRangeLabel,
                            formatStatisticsMoney(
                                micros = projection.selectedRangeAmountMicros,
                                currencyCode = currencyCode,
                            ),
                            formatStatisticsMoney(
                                micros = projection.chartLineAmountMicros,
                                currencyCode = currencyCode,
                            ),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = targetColor,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = showTrend,
                        role = Role.Checkbox,
                        onValueChange = onShowTrendChanged,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = showTrend,
                    onCheckedChange = null,
                )
                Text(text = stringResource(id = R.string.statistics_show_trend))
                Spacer(modifier = Modifier.weight(weight = 1f))
                if (showTrend) {
                    Text(
                        text = stringResource(
                            id = R.string.statistics_average,
                            formatStatisticsMoney(
                                micros = trendAmount * MICROS_PER_CURRENCY_UNIT,
                                currencyCode = currencyCode,
                            ),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = trendColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStatisticsHorizontalLine(
    value: Double,
    minValue: Double,
    maxValue: Double,
    color: Color,
    strokeWidth: Dp,
    pathEffect: PathEffect? = null,
) {
    val range = maxValue - minValue
    val fraction = if (range == 0.0) 0.0 else (value - minValue) / range
    val y = (size.height * (1.0 - fraction)).toFloat()
        .coerceIn(minimumValue = 1f, maximumValue = size.height - 1f)
    drawLine(
        color = color,
        start = Offset(x = 0f, y = y),
        end = Offset(x = size.width, y = y),
        strokeWidth = strokeWidth.toPx(),
        pathEffect = pathEffect,
    )
}

private fun statisticsBarThickness(barCount: Int): Dp = when {
    barCount <= 7 -> 22.dp
    barCount <= 12 -> 16.dp
    else -> 12.dp
}

private const val STATISTICS_CHART_INDICATOR_COUNT = 4
private const val EMPTY_CHART_MAX_VALUE = 1.0
private val STATISTICS_CHART_HEIGHT = 260.dp
private val STATISTICS_CHART_PLOT_HEIGHT = 210.dp
private val STATISTICS_CHART_INDICATOR_PADDING = 48.dp
private val STATISTICS_CHART_BAR_SPACING = 6.dp
private val STATISTICS_CHART_BAR_RADIUS = 5.dp
private val STATISTICS_CHART_LABEL_PADDING = 8.dp
private val STATISTICS_TREND_LINE_WIDTH = 2.dp
private val STATISTICS_TARGET_LINE_WIDTH = 2.dp
private val STATISTICS_TREND_DASH_LENGTH = 8.dp
private val STATISTICS_TREND_DASH_GAP = 6.dp
