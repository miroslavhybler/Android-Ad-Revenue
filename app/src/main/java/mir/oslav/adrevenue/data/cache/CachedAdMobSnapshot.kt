package mir.oslav.adrevenue.data.cache

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobApp
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.model.SignedInUser

@Keep
@Serializable
data class CachedAdMobSnapshot constructor(
    val signedInUser: SignedInUser? = null,
    val accounts: List<AdMobAccount> = emptyList(),
    val apps: List<AdMobApp> = emptyList(),
    val appIconUrls: Map<String, String> = emptyMap(),
    val selectedAccountName: String? = null,
    val report: AdMobNetworkReport? = null,
    val refreshedAtEpochMillis: Long? = null,
    val statisticsReports: List<CachedStatisticsReport> = emptyList(),
    val revenueTargets: List<CachedRevenueTarget> = emptyList(),
) {
    val selectedAccount: AdMobAccount?
        get() = accounts.firstOrNull { it.name == selectedAccountName }

    val hasDisplayableData: Boolean
        get() = accounts.isNotEmpty() || report != null
}
