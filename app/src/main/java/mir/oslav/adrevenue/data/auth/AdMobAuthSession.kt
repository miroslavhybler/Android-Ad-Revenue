package mir.oslav.adrevenue.data.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AdMobAuthSession @Inject constructor() {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun setAccessToken(accessToken: String) {
        _accessToken.value = accessToken
    }

    fun clear() {
        _accessToken.value = null
    }
}
