package mir.oslav.adrevenue.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import mir.oslav.adrevenue.data.cache.CachedAdMobSnapshot
import mir.oslav.adrevenue.data.model.AdMobAccount
import mir.oslav.adrevenue.data.model.SignedInUser
import mir.oslav.adrevenue.ui.auth.AuthStep
import mir.oslav.adrevenue.ui.auth.AuthUiState
import mir.oslav.adrevenue.ui.home.HomeUiState

class NavigationStateTest {
    private val user = SignedInUser(id = "developer@example.com")
    private val account = AdMobAccount(
        name = "accounts/pub-1234567890123456",
        publisherId = "pub-1234567890123456",
        reportingTimeZone = "Europe/Prague",
        currencyCode = "USD",
    )

    @Test
    fun singleDiscoveredAccount_staysInAuthUntilConfirmed() {
        val authState = AuthUiState(
            isRestoring = false,
            step = AuthStep.AccountSelection,
            signedInUser = user,
            accounts = listOf(account),
            selectedAccountName = account.name,
            hasConfirmedAccount = false,
        )
        val homeState = HomeUiState(
            snapshot = CachedAdMobSnapshot(
                signedInUser = user,
                accounts = listOf(account),
            ),
        )

        assertTrue(authState.shouldShowAuth)
        assertFalse(homeState.shouldShowHome)
    }

    @Test
    fun confirmedCachedAccount_opensHome() {
        val authState = AuthUiState(
            isRestoring = false,
            step = AuthStep.AccountSelection,
            signedInUser = user,
            accounts = listOf(account),
            selectedAccountName = account.name,
            hasConfirmedAccount = true,
        )
        val homeState = HomeUiState(
            snapshot = CachedAdMobSnapshot(
                signedInUser = user,
                accounts = listOf(account),
                selectedAccountName = account.name,
            ),
        )

        assertFalse(authState.shouldShowAuth)
        assertTrue(homeState.shouldShowHome)
    }
}
