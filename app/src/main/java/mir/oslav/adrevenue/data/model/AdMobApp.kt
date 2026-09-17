package mir.oslav.adrevenue.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class AdMobApp constructor(
    val appId: String,
    val appStoreId: String? = null,
    val displayName: String,
)
