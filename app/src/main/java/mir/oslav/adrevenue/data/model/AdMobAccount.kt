package mir.oslav.adrevenue.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class AdMobAccount(
    val name: String,
    val publisherId: String,
    val reportingTimeZone: String,
    val currencyCode: String,
)
