package mir.oslav.adrevenue.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.ui.auth.AuthViewModel
import mir.oslav.adrevenue.ui.home.HomeViewModel
import mir.oslav.adrevenue.ui.stats.StatsViewModel

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    statsViewModel: StatsViewModel,
    onSignInClick: () -> Unit,
    onUseAnotherGoogleAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onStatisticsAuthorizationRequested: () -> Unit,
) {
    val authState = authViewModel.uiState.collectAsStateWithLifecycle()
    val homeState = homeViewModel.uiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(Destination.Auth)

    LaunchedEffect(key1 = authState.value.shouldShowAuth, key2 = homeState.value.shouldShowHome) {
        when {
            authState.value.shouldShowAuth -> backStack.replaceWith(destination = Destination.Auth)
            homeState.value.shouldShowHome && backStack.lastOrNull() == Destination.Auth -> {
                backStack.replaceWith(destination = Destination.Home)
            }
        }
    }

    val currentDestination = backStack.lastOrNull() as? Destination
    val showPrimaryNavigation = currentDestination in mainNavigationDestinations
    val content = @Composable {
        AdMobClientNavDisplay(
            backStack = backStack,
            authViewModel = authViewModel,
            homeViewModel = homeViewModel,
            statsViewModel = statsViewModel,
            onNavigate = backStack::navigate,
            onBack = backStack::navigateBack,
            onSignInClick = onSignInClick,
            onUseAnotherGoogleAccountClick = onUseAnotherGoogleAccountClick,
            onRefreshClick = onRefreshClick,
            onStatisticsAuthorizationRequested = onStatisticsAuthorizationRequested,
        )
    }

    if (authState.value.isRestoring) {
        // Do not compose AuthPane while the persisted session is being restored.
        // This prevents a signed-in user from seeing a one-frame auth screen.
        Box(modifier = Modifier.fillMaxSize())
    } else if (showPrimaryNavigation) {
        MainNavigationBar(
            currentDestination = currentDestination,
            onNavigate = backStack::navigate,
            content = content,
        )
    } else {
        content()
    }
}

private fun NavBackStack<NavKey>.replaceWith(destination: Destination) {
    if (size == 1 && firstOrNull() == destination) return
    clear()
    add(element = destination)
}

private fun NavBackStack<NavKey>.navigate(destination: Destination) {
    when (destination) {
        Destination.Auth,
        Destination.Home,
        Destination.Statistics,
        Destination.Account, Destination.Search -> replaceWith(destination = destination)

        Destination.SettingsAndAbout,
        is Destination.SearchDocumentDetail -> {
            removeAll(predicate = { it == destination })
            add(element = destination)
        }
    }
}

private fun NavBackStack<NavKey>.navigateBack() {
    if (size > 1) {
        removeLastOrNull()
    }
}

private val mainNavigationDestinations: List<Destination> =
    mainNavigationItems.map(transform = { item -> item.destination })
