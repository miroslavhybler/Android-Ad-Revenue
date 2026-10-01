package mir.oslav.adrevenue.data

import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.AdMobApp
import mir.oslav.adrevenue.data.model.AdMobNetworkReport

/** Flavor-selected data source used by the AdMob repository. */
interface AdMobBackend {

    /** Loads accounts available to the authenticated user. */

    suspend fun listAccounts(
        accessToken: String,
    ): List<AdMobAccount>

    /** Loads apps belonging to an AdMob account. */
    suspend fun listApps(
        accessToken: String,
        accountName: String
    ): List<AdMobApp>

    /** Generates a report, optionally split by app for statistics. */
    suspend fun generateNetworkReport(
        accessToken: String,
        accountName: String,
        startDateEpochMillis: Long? = null,
        endDateEpochMillis: Long? = null,
        includeAppDimension: Boolean = false,
    ): AdMobNetworkReport
}
