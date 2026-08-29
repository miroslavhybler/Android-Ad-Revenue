package mir.oslav.adrevenue.data.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class SignedInUser(
    val id: String,
    val displayName: String? = null,
    val profilePictureUri: String? = null,
)
