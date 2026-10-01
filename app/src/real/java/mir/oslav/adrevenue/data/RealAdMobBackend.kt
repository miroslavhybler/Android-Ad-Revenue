package mir.oslav.adrevenue.data

import javax.inject.Inject
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobApp
import mir.oslav.adrevenue.data.model.AdMobNetworkReport
import mir.oslav.adrevenue.data.network.AdMobApiClient


/** Production backend that delegates operations to the AdMob REST client. */
class RealAdMobBackend @Inject constructor(
    private val api: AdMobApiClient,
) : AdMobBackend {

    override suspend fun listAccounts(
        accessToken: String,
    ): List<AdMobAccount> = api.listAccounts(accessToken = accessToken)

    override suspend fun listApps(
        accessToken: String,
        accountName: String,
    ): List<AdMobApp> = api.listApps(accessToken = accessToken, accountName = accountName)

    override suspend fun generateNetworkReport(
        accessToken: String,
        accountName: String,
        startDateEpochMillis: Long?,
        endDateEpochMillis: Long?,
        includeAppDimension: Boolean,
    ): AdMobNetworkReport = if (startDateEpochMillis == null || endDateEpochMillis == null) {
        api.generateNetworkReport(accessToken, accountName)
    } else {
        api.generateNetworkReport(
            accessToken = accessToken,
            accountName = accountName,
            startDateEpochMillis = startDateEpochMillis,
            endDateEpochMillis = endDateEpochMillis,
            includeAppDimension = includeAppDimension,
        )
    }
}
