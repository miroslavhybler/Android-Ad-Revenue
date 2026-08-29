package mir.oslav.adrevenue.core

object AdMobApiConstants {
    const val ApiBaseUrl = "https://admob.googleapis.com/v1"
    const val ReadonlyScope = "https://www.googleapis.com/auth/admob.readonly"
    const val ReportTimeZone = "America/Los_Angeles"
    const val ConnectTimeoutMillis = 15_000L
    const val ReadTimeoutMillis = 30_000L

    val NetworkReportDimensions = listOf("DATE")
    val NetworkReportMetrics = listOf(
        "ESTIMATED_EARNINGS",
        "IMPRESSIONS",
        "CLICKS",
        "MATCH_RATE",
        "IMPRESSION_RPM",
    )
}
