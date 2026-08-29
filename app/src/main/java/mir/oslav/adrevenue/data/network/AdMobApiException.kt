package mir.oslav.adrevenue.data.network

import java.io.IOException

class AdMobApiException(
    val responseCode: Int,
    val url: String,
    val responseBody: String,
    override val message: String,
) : IOException(message)
