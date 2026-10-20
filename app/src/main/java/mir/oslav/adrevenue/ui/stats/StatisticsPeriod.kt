package mir.oslav.adrevenue.ui.stats

import java.util.Calendar
import java.util.TimeZone
import mir.oslav.adrevenue.core.AdMobApiConstants
import mir.oslav.adrevenue.data.model.AdMobReportRow

enum class StatisticsTimeRange {
    Week,
    Month,
    Quarter,
    Year,
}

/** Dimension used to split each earnings bar into stacked segments. */
enum class StatisticsBreakdownDimension {
    App,
    AdFormat,
}

data class StatisticsPeriod(
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
)

data class StatisticsChartBar(
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val estimatedEarningsMicros: Long,
)

/** One consistently colored series shown in a stacked statistics chart. */
data class StatisticsChartSeries(
    val key: String,
    val label: String,
    val estimatedEarningsMicros: Long,
)

/** Earnings belonging to each series for one chart time bucket. */
data class StatisticsStackedChartBar(
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val earningsMicrosBySeries: Map<String, Long>,
)

/** Complete data needed to render an app or ad-format stacked chart. */
data class StatisticsChartBreakdown(
    val dimension: StatisticsBreakdownDimension,
    val series: List<StatisticsChartSeries>,
    val bars: List<StatisticsStackedChartBar>,
)

internal fun calculateStatisticsPeriod(
    timeRange: StatisticsTimeRange,
    periodOffset: Int,
    anchorEpochMillis: Long,
): StatisticsPeriod {
    require(periodOffset <= 0) { "Statistics cannot request a future period." }

    val timeZone = TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)
    val today = reportCalendar(epochMillis = anchorEpochMillis, timeZone = timeZone)
    val start = today.copyCalendar()

    when (timeRange) {
        StatisticsTimeRange.Week -> {
            val daysSinceMonday = (start.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            start.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
            start.add(Calendar.WEEK_OF_YEAR, periodOffset)
        }

        StatisticsTimeRange.Month -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.add(Calendar.MONTH, periodOffset)
        }

        StatisticsTimeRange.Quarter -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.MONTH, start.get(Calendar.MONTH) / MONTHS_PER_QUARTER * MONTHS_PER_QUARTER)
            start.add(Calendar.MONTH, periodOffset * MONTHS_PER_QUARTER)
        }

        StatisticsTimeRange.Year -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.MONTH, Calendar.JANUARY)
            start.add(Calendar.YEAR, periodOffset)
        }
    }

    val end = start.copyCalendar().apply {
        when (timeRange) {
            StatisticsTimeRange.Week -> add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK)
            StatisticsTimeRange.Month -> add(Calendar.MONTH, 1)
            StatisticsTimeRange.Quarter -> add(Calendar.MONTH, MONTHS_PER_QUARTER)
            StatisticsTimeRange.Year -> add(Calendar.YEAR, 1)
        }
        add(Calendar.DAY_OF_MONTH, -1)
    }
    if (periodOffset == 0 && end.after(today)) {
        end.timeInMillis = today.timeInMillis
    }

    return StatisticsPeriod(
        startDateEpochMillis = start.timeInMillis,
        endDateEpochMillis = end.timeInMillis,
    )
}

internal fun buildStatisticsChartBars(
    rows: List<AdMobReportRow>,
    timeRange: StatisticsTimeRange,
    period: StatisticsPeriod,
): List<StatisticsChartBar> {
    return buildStatisticsBuckets(timeRange = timeRange, period = period).map { bucket ->
        StatisticsChartBar(
            startDateEpochMillis = bucket.startDateEpochMillis,
            endDateEpochMillis = bucket.endDateEpochMillis,
            estimatedEarningsMicros = rows.asSequence()
                .filter { row -> row.isIn(bucket = bucket) }
                .sumOf { row -> row.estimatedEarningsMicros ?: 0L },
        )
    }
}

/** Aggregates report rows into the same time buckets as the total earnings chart. */
internal fun buildStatisticsChartBreakdown(
    rows: List<AdMobReportRow>,
    timeRange: StatisticsTimeRange,
    period: StatisticsPeriod,
    dimension: StatisticsBreakdownDimension,
): StatisticsChartBreakdown {
    val rowsBySeries = rows.groupBy { row -> row.breakdownKey(dimension = dimension) }
    val series = rowsBySeries.map { (key, seriesRows) ->
        StatisticsChartSeries(
            key = key,
            label = seriesRows.first().breakdownLabel(dimension = dimension),
            estimatedEarningsMicros = seriesRows.sumOf { row ->
                row.estimatedEarningsMicros ?: 0L
            },
        )
    }.sortedWith(
        compareByDescending<StatisticsChartSeries> { item -> item.estimatedEarningsMicros }
            .thenBy { item -> item.label },
    )

    val bars = buildStatisticsBuckets(timeRange = timeRange, period = period).map { bucket ->
        StatisticsStackedChartBar(
            startDateEpochMillis = bucket.startDateEpochMillis,
            endDateEpochMillis = bucket.endDateEpochMillis,
            earningsMicrosBySeries = rows.asSequence()
                .filter { row -> row.isIn(bucket = bucket) }
                .groupBy { row -> row.breakdownKey(dimension = dimension) }
                .mapValues { (_, bucketRows) ->
                    bucketRows.sumOf { row -> row.estimatedEarningsMicros ?: 0L }
                },
        )
    }

    return StatisticsChartBreakdown(
        dimension = dimension,
        series = series,
        bars = bars,
    )
}

private fun buildStatisticsBuckets(
    timeRange: StatisticsTimeRange,
    period: StatisticsPeriod,
): List<StatisticsPeriod> {
    val timeZone = TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)
    val periodEnd = reportCalendar(period.endDateEpochMillis, timeZone)
    val bucketStarts = generateSequence(
        seed = reportCalendar(period.startDateEpochMillis, timeZone),
    ) { previous ->
        previous.copyCalendar().apply {
            when (timeRange) {
                StatisticsTimeRange.Week -> add(Calendar.DAY_OF_MONTH, 1)
                StatisticsTimeRange.Month,
                StatisticsTimeRange.Quarter -> add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK)
                StatisticsTimeRange.Year -> add(Calendar.MONTH, 1)
            }
        }
    }.takeWhile { start -> !start.after(periodEnd) }

    return bucketStarts.map { start ->
        val end = start.copyCalendar().apply {
            when (timeRange) {
                StatisticsTimeRange.Week -> Unit
                StatisticsTimeRange.Month,
                StatisticsTimeRange.Quarter -> add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK - 1)
                StatisticsTimeRange.Year -> {
                    add(Calendar.MONTH, 1)
                    add(Calendar.DAY_OF_MONTH, -1)
                }
            }
        }
        if (end.after(periodEnd)) {
            end.timeInMillis = periodEnd.timeInMillis
        }

        StatisticsPeriod(
            startDateEpochMillis = start.timeInMillis,
            endDateEpochMillis = end.timeInMillis,
        )
    }.toList()
}

private fun AdMobReportRow.isIn(bucket: StatisticsPeriod): Boolean {
    val rowDate = dateEpochMillis ?: return false
    return rowDate in bucket.startDateEpochMillis..bucket.endDateEpochMillis
}

private fun AdMobReportRow.breakdownKey(dimension: StatisticsBreakdownDimension): String =
    when (dimension) {
        StatisticsBreakdownDimension.App -> statisticsAppSeriesKey()

        StatisticsBreakdownDimension.AdFormat -> adFormat
            ?.takeIf { value -> value.isNotBlank() }
            ?: UNKNOWN_AD_FORMAT_KEY
    }

internal fun AdMobReportRow.statisticsAppSeriesKey(): String = appId
    ?.takeIf { value -> value.isNotBlank() }
    ?: appName?.takeIf { value -> value.isNotBlank() }
    ?: UNKNOWN_APP_KEY

private fun AdMobReportRow.breakdownLabel(dimension: StatisticsBreakdownDimension): String =
    when (dimension) {
        StatisticsBreakdownDimension.App -> appName
            ?.takeIf { value -> value.isNotBlank() }
            ?: appId?.takeIf { value -> value.isNotBlank() }
            ?: UNKNOWN_APP_LABEL

        StatisticsBreakdownDimension.AdFormat -> adFormat
            ?.takeIf { value -> value.isNotBlank() }
            ?.toAdFormatLabel()
            ?: UNKNOWN_AD_FORMAT_LABEL
    }

private fun String.toAdFormatLabel(): String {
    if (none { character -> character == '_' } && any { character -> character.isLowerCase() }) {
        return this
    }
    return lowercase()
        .replace(oldChar = '_', newChar = ' ')
        .replaceFirstChar { character -> character.titlecase() }
}

private fun reportCalendar(epochMillis: Long, timeZone: TimeZone): Calendar =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

private fun Calendar.copyCalendar(): Calendar = clone() as Calendar

private const val DAYS_PER_WEEK = 7
private const val MONTHS_PER_QUARTER = 3
private const val UNKNOWN_APP_KEY = "unknown-app"
private const val UNKNOWN_AD_FORMAT_KEY = "unknown-ad-format"
private const val UNKNOWN_APP_LABEL = "Unknown app"
private const val UNKNOWN_AD_FORMAT_LABEL = "Unknown ad format"
