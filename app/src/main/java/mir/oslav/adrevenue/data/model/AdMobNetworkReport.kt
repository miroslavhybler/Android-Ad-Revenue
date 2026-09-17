package mir.oslav.adrevenue.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class AdMobNetworkReport(
    val currencyCode: String,
    val dateRangeLabel: String,
    val matchingRowCount: Int,
    val rows: List<AdMobReportRow>,
) {
    val totalEstimatedEarningsMicros: Long = rows.sumOf { it.estimatedEarningsMicros ?: 0L }
    val totalImpressions: Long = rows.sumOf { it.impressions ?: 0L }
    val totalClicks: Long = rows.sumOf { it.clicks ?: 0L }
    val averageMatchRate: Double? = rows.mapNotNull { it.matchRate }.takeIf { it.isNotEmpty() }?.average()
    val averageImpressionRpmMicros: Double? =
        rows.mapNotNull { it.impressionRpmMicros }.takeIf { it.isNotEmpty() }?.average()
}

@Keep
@Serializable
data class AdMobReportRow(
    val dateLabel: String,
    val appName: String? = null,
    val appId: String? = null,
    val estimatedEarningsMicros: Long?,
    val impressions: Long?,
    val clicks: Long?,
    val matchRate: Double?,
    val impressionRpmMicros: Long?,
    val dateEpochMillis: Long? = null,
)
