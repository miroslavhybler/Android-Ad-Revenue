package mir.oslav.adrevenue.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import mir.oslav.adrevenue.R

@Composable
fun MainNavigationBar(currentDestination: Destination?, onNavigate: (Destination) -> Unit, content: @Composable () -> Unit) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            mainNavigationItems.forEach { item ->
                item(selected = currentDestination == item.destination, onClick = { onNavigate(item.destination) }, icon = { Icon(item.icon, null) }, label = { Text(stringResource(item.labelResource)) })
            }
        },
        content = content,
    )
}

data class MainNavigationItem(val destination: Destination, @param:StringRes val labelResource: Int, val icon: ImageVector)

val mainNavigationItems = listOf(
    MainNavigationItem(Destination.Home, R.string.navigation_home, Icons.Outlined.Home),
    MainNavigationItem(Destination.Statistics, R.string.navigation_statistics, Icons.Outlined.BarChart),
    MainNavigationItem(Destination.Search, R.string.navigation_search, Icons.Outlined.Search),
    MainNavigationItem(Destination.Account, R.string.navigation_account, Icons.Outlined.AccountCircle),
)
