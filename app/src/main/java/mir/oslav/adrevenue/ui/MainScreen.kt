package mir.oslav.adrevenue.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
    onSignOutClick: () -> Unit,
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
            onSignOutClick = onSignOutClick,
        )
    }

    if (showPrimaryNavigation) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                mainNavigationItems.forEach { navigationItem ->
                    item(
                        selected = currentDestination == navigationItem.destination,
                        onClick = { backStack.navigate(destination = navigationItem.destination) },
                        icon = {
                            Icon(
                                imageVector = navigationItem.icon,
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(text = stringResource(id = navigationItem.labelResource))
                        },
                    )
                }
            },
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
        Destination.Account -> replaceWith(destination = destination)

        Destination.SettingsAndAbout -> {
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

private data class MainNavigationItem(
    val destination: Destination,
    @param:StringRes val labelResource: Int,
    val icon: ImageVector,
)

private val mainNavigationItems = listOf(
    MainNavigationItem(
        destination = Destination.Home,
        labelResource = R.string.navigation_home,
        icon = Icons.Outlined.Home,
    ),
    MainNavigationItem(
        destination = Destination.Statistics,
        labelResource = R.string.navigation_statistics,
        icon = Icons.Outlined.BarChart,
    ),
    MainNavigationItem(
        destination = Destination.Account,
        labelResource = R.string.navigation_account,
        icon = Icons.Outlined.AccountCircle,
    ),
)

private val mainNavigationDestinations = mainNavigationItems.map { item -> item.destination }
