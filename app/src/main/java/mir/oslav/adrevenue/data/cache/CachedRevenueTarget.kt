package mir.oslav.adrevenue.data.cache

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class CachedRevenueTarget(
    val accountName: String,
    val amountMicros: Long,
    val rangeDays: Int,
)
