package mir.oslav.adrevenue.data.mock

import java.util.Calendar
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobApp
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.model.AdMobReportRow

/** Shared deterministic data for mock builds, previews, and presentation screenshots. */
object MockAdMobFixtures {
    private val adFormats = listOf(
        "Banner" to 4L,
        "Interstitial" to 3L,
        "Rewarded" to 2L,
        "App open" to 1L,
    )

    val account = AdMobAccount("accounts/pub-mock", "pub-mock", "America/Los_Angeles", "USD")
    val apps = listOf(
        AdMobApp("ca-app-pub-mock~one", "com.example.mock.one", "Revenue App"),
        AdMobApp("ca-app-pub-mock~two", "com.example.mock.two", "Fitness App"),
    )

    fun report(startDateEpochMillis: Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -29)
    }.timeInMillis, includeAppDimension: Boolean = true): AdMobNetworkReport {
        val rows = (0..29).flatMap { index ->
            val date = startDateEpochMillis + index * 86_400_000L
            apps.flatMapIndexed { appIndex, app ->
                val appEarnings = 1_000_000L + appIndex * 500_000L
                val appImpressions = 1_000L + appIndex * 200L
                adFormats.map { (format, weight) ->
                    AdMobReportRow(
                        dateLabel = "Mock day $index",
                        appName = app.displayName,
                        appId = app.appId,
                        estimatedEarningsMicros = appEarnings * weight / 10L,
                        impressions = appImpressions * weight / 10L,
                        clicks = weight,
                        matchRate = .95,
                        impressionRpmMicros = 1_000_000L,
                        dateEpochMillis = date,
                        adFormat = format,
                    )
                }
            }
        }
        val outputRows = if (includeAppDimension) rows else rows.groupBy { it.dateEpochMillis }
            .map { (_, values) -> values.first().copy(
                estimatedEarningsMicros = values.sumOf { it.estimatedEarningsMicros ?: 0L },
                appName = null, appId = null,
            ) }
        return AdMobNetworkReport("USD", "Mock period", outputRows.size, outputRows)
    }
}
