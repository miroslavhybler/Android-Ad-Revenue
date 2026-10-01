package mir.oslav.adrevenue.data.mock

import java.util.Calendar
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobApp
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.model.AdMobReportRow

/** Shared deterministic data for mock builds, previews, and presentation screenshots. */
object MockAdMobFixtures {
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
            apps.mapIndexed { appIndex, app ->
                AdMobReportRow("Mock day $index", app.displayName, app.appId,
                    1_000_000L + appIndex * 500_000L, 1000L + appIndex * 200,
                    10L, .95, 1_000_000L, date)
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
