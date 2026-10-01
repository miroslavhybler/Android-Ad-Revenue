package mir.oslav.adrevenue.data

import javax.inject.Inject
import mir.oslav.adrevenue.data.mock.MockAdMobFixtures
import mir.oslav.adrevenue.data.model.AdMobNetworkReport

/** Deterministic, network-free backend for previews, screenshots, and tests. */
class MockAdMobBackend @Inject constructor() : AdMobBackend {
    override suspend fun listAccounts(accessToken: String) = listOf(MockAdMobFixtures.account)
    override suspend fun listApps(accessToken: String, accountName: String) = MockAdMobFixtures.apps

    override suspend fun generateNetworkReport(
        accessToken: String, accountName: String, startDateEpochMillis: Long?,
        endDateEpochMillis: Long?, includeAppDimension: Boolean,
    ): AdMobNetworkReport = MockAdMobFixtures.report(
        startDateEpochMillis = startDateEpochMillis ?: System.currentTimeMillis() - 29 * 86_400_000L,
        includeAppDimension = includeAppDimension,
    )
}
