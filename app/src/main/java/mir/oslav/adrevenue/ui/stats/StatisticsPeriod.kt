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

data class StatisticsPeriod(
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
)

data class StatisticsChartBar(
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val estimatedEarningsMicros: Long,
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

        StatisticsChartBar(
            startDateEpochMillis = start.timeInMillis,
            endDateEpochMillis = end.timeInMillis,
            estimatedEarningsMicros = rows.asSequence()
                .filter { row ->
                    val rowDate = row.dateEpochMillis
                    rowDate != null && rowDate in start.timeInMillis..end.timeInMillis
                }
                .sumOf { row -> row.estimatedEarningsMicros ?: 0L },
        )
    }.toList()
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
