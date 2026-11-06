package mir.oslav.adrevenue.ui.stats

import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import mir.oslav.adrevenue.core.adMobReportTimeZone

internal fun formatStatisticsDate(epochMillis: Long, reportingTimeZone: String): String {
    val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM).apply {
        timeZone = adMobReportTimeZone(timeZoneId = reportingTimeZone)
    }
    return formatter.format(Date(epochMillis))
}

internal fun formatStatisticsBarLabel(
    bar: StatisticsChartBar,
    timeRange: StatisticsTimeRange,
    reportingTimeZone: String,
): String {
    val pattern = if (timeRange == StatisticsTimeRange.Year) "MMM" else "MMM d"
    return SimpleDateFormat(pattern, Locale.getDefault()).apply {
        timeZone = adMobReportTimeZone(timeZoneId = reportingTimeZone)
    }.format(Date(bar.startDateEpochMillis))
}

internal fun formatStatisticsMoney(micros: Long, currencyCode: String): String =
    formatStatisticsMoney(micros = micros.toDouble(), currencyCode = currencyCode)

internal fun formatStatisticsMoney(micros: Double, currencyCode: String): String {
    val amount = micros / MICROS_PER_CURRENCY_UNIT
    val currency = currencyCode.takeIf { code -> code.isNotBlank() }
        ?.let { code -> runCatching { Currency.getInstance(code) }.getOrNull() }
    val formatter = if (currency == null) {
        NumberFormat.getNumberInstance(Locale.getDefault())
    } else {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            this.currency = currency
        }
    }
    return formatter.apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)
}

internal fun formatStatisticsChartAmount(value: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(value)

internal fun formatStatisticsUpdatedAt(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(epochMillis))

internal const val MICROS_PER_CURRENCY_UNIT = 1_000_000.0
