package mir.oslav.adrevenue.ui.stats

import androidx.compose.ui.graphics.Color

/** Shared palette that keeps an app's color consistent across statistics charts. */
internal val StatisticsChartSeriesColors = listOf(
    Color(0xFF5D54C5),
    Color(0xFF006875),
    Color(0xFF7B5800),
    Color(0xFFB04A67),
    Color(0xFF4D7C5A),
    Color(0xFF7A5C99),
)

internal fun statisticsChartSeriesColor(index: Int): Color =
    StatisticsChartSeriesColors[index % StatisticsChartSeriesColors.size]
