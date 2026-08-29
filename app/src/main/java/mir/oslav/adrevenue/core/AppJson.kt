package mir.oslav.adrevenue.core

import kotlinx.serialization.json.Json

object AppJson {
    val Json = Json {
        coerceInputValues = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }
}
