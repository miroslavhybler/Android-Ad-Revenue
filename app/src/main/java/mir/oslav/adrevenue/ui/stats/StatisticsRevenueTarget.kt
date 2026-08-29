package mir.oslav.adrevenue.ui.stats

import java.util.Calendar
import java.util.TimeZone
import mir.oslav.adrevenue.core.AdMobApiConstants
import mir.oslav.adrevenue.data.cache.CachedRevenueTarget

data class StatisticsTargetProjection(
    val configuredAmountMicros: Long,
    val configuredRangeDays: Int,
    val selectedRangeAmountMicros: Double,
    val chartLineAmountMicros: Double,
)

internal fun projectRevenueTarget(
    target: CachedRevenueTarget,
    timeRange: StatisticsTimeRange,
    period: StatisticsPeriod,
    barCount: Int,
): StatisticsTargetProjection? {
    if (target.amountMicros <= 0L || target.rangeDays <= 0 || barCount <= 0) return null

    val dailyTargetMicros = target.amountMicros.toDouble() / target.rangeDays
    val fullRangeDays = fullStatisticsRangeDays(
        timeRange = timeRange,
        startDateEpochMillis = period.startDateEpochMillis,
    )
    val displayedDays = statisticsPeriodDays(period = period)

    return StatisticsTargetProjection(
        configuredAmountMicros = target.amountMicros,
        configuredRangeDays = target.rangeDays,
        selectedRangeAmountMicros = dailyTargetMicros * fullRangeDays,
        chartLineAmountMicros = dailyTargetMicros * displayedDays / barCount,
    )
}

private fun fullStatisticsRangeDays(
    timeRange: StatisticsTimeRange,
    startDateEpochMillis: Long,
): Int {
    val start = targetCalendar(epochMillis = startDateEpochMillis)
    val endExclusive = start.copyTargetCalendar().apply {
        when (timeRange) {
            StatisticsTimeRange.Week -> add(Calendar.DAY_OF_MONTH, DAYS_PER_TARGET_WEEK)
            StatisticsTimeRange.Month -> add(Calendar.MONTH, 1)
            StatisticsTimeRange.Quarter -> add(Calendar.MONTH, MONTHS_PER_TARGET_QUARTER)
            StatisticsTimeRange.Year -> add(Calendar.YEAR, 1)
        }
    }
    return countCalendarDays(start = start, endExclusive = endExclusive)
}

private fun statisticsPeriodDays(period: StatisticsPeriod): Int {
    val start = targetCalendar(epochMillis = period.startDateEpochMillis)
    val endExclusive = targetCalendar(epochMillis = period.endDateEpochMillis).apply {
        add(Calendar.DAY_OF_MONTH, 1)
    }
    return countCalendarDays(start = start, endExclusive = endExclusive)
}

private fun countCalendarDays(start: Calendar, endExclusive: Calendar): Int {
    val cursor = start.copyTargetCalendar()
    var dayCount = 0
    while (cursor.before(endExclusive)) {
        dayCount += 1
        cursor.add(Calendar.DAY_OF_MONTH, 1)
    }
    return dayCount
}

private fun targetCalendar(epochMillis: Long): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone(AdMobApiConstants.ReportTimeZone)).apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

private fun Calendar.copyTargetCalendar(): Calendar = clone() as Calendar

internal const val DEFAULT_REVENUE_TARGET_RANGE_DAYS = 30
internal const val MIN_REVENUE_TARGET_RANGE_DAYS = 1
internal const val MAX_REVENUE_TARGET_RANGE_DAYS = 3_650
private const val DAYS_PER_TARGET_WEEK = 7
private const val MONTHS_PER_TARGET_QUARTER = 3
