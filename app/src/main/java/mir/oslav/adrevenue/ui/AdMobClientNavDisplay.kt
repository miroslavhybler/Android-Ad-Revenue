package mir.oslav.adrevenue.ui

import androidx.annotation.Keep
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import mir.oslav.adrevenue.ui.account.AccountPane
import mir.oslav.adrevenue.ui.auth.AuthPane
import mir.oslav.adrevenue.ui.auth.AuthViewModel
import mir.oslav.adrevenue.ui.home.HomePane
import mir.oslav.adrevenue.ui.home.HomeViewModel
import mir.oslav.adrevenue.ui.settings.SettingsAndAboutPane
import mir.oslav.adrevenue.ui.stats.StatsPane
import mir.oslav.adrevenue.ui.stats.StatsViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdMobClientNavDisplay(
    backStack: NavBackStack<NavKey>,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    statsViewModel: StatsViewModel,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
    onUseAnotherGoogleAccountClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onStatisticsAuthorizationRequested: () -> Unit,
) {
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    NavDisplay(
        modifier = Modifier.fillMaxSize(),
        backStack = backStack,
        onBack = onBack,
        sceneStrategies = listOf(listDetailStrategy),
        predictivePopTransitionSpec = {
            fadeIn() togetherWith slideOutHorizontally { it }
        },
        entryProvider = entryProvider {
            entry<Destination.Auth>(
                metadata = ListDetailSceneStrategy.listPane(),
            ) {
                AuthPane(
                    onNavigate = onNavigate,
                    onSignInClick = onSignInClick,
                    onUseAnotherGoogleAccountClick = onUseAnotherGoogleAccountClick,
                    viewModel = authViewModel,
                )
            }

            entry<Destination.Home>(
                metadata = ListDetailSceneStrategy.listPane(),
            ) {
                HomePane(
                    onNavigate = onNavigate,
                    onRefreshClick = onRefreshClick,
                    viewModel = homeViewModel,
                )
            }

            entry<Destination.Statistics>(
                metadata = ListDetailSceneStrategy.listPane(),
            ) {
                StatsPane(
                    onAuthorizationRequested = onStatisticsAuthorizationRequested,
                    viewModel = statsViewModel,
                )
            }

            entry<Destination.Account>(
                metadata = ListDetailSceneStrategy.listPane(),
            ) {
                AccountPane()
            }

            entry<Destination.SettingsAndAbout>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) {
                SettingsAndAboutPane(
                    onBackClick = onBack,
                )
            }
        },
    )
}

@Keep
@Serializable
sealed interface Destination : NavKey {
    @Keep
    @Serializable
    data object Auth : Destination

    @Keep
    @Serializable
    data object Home : Destination

    @Keep
    @Serializable
    data object Statistics : Destination

    @Keep
    @Serializable
    data object Account : Destination

    @Keep
    @Serializable
    data object SettingsAndAbout : Destination
}
