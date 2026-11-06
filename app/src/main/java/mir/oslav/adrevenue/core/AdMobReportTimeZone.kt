package mir.oslav.adrevenue.core

import java.util.TimeZone

private val availableReportTimeZoneIds by lazy {
    TimeZone.getAvailableIDs().toHashSet()
}

/** Returns a valid account reporting time-zone ID, falling back for old or incomplete cache data. */
fun normalizeAdMobReportTimeZone(timeZoneId: String): String =
    timeZoneId.trim().takeIf { candidate -> candidate in availableReportTimeZoneIds }
        ?: AdMobApiConstants.FallbackReportTimeZone

fun adMobReportTimeZone(timeZoneId: String): TimeZone =
    TimeZone.getTimeZone(normalizeAdMobReportTimeZone(timeZoneId = timeZoneId))
