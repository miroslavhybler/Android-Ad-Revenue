package mir.oslav.adrevenue.data.cache

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import mir.oslav.adrevenue.data.model.AdMobNetworkReport

@Keep
@Serializable
data class CachedStatisticsReport(
    val accountName: String,
    val startDateEpochMillis: Long,
    val endDateEpochMillis: Long,
    val report: AdMobNetworkReport,
    val refreshedAtEpochMillis: Long,
)
