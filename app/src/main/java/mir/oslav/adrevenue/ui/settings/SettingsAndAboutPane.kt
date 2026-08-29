package mir.oslav.adrevenue.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.ui.AdRevenueTheme
import mir.oslav.adrevenue.ui.PreviewContainer

@Composable
fun SettingsAndAboutPane(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsAndAboutPaneImpl(
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun SettingsAndAboutPaneImpl(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val adMobConsoleUrl = stringResource(R.string.settings_admob_console_url)
    val githubUrl = stringResource(R.string.settings_github_url)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 24.dp + WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                end = 20.dp,
                bottom = 24.dp + WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.settings_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = { uriHandler.openUri(adMobConsoleUrl) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logo_admob),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_open_admob_console))
                }
            }
            item {
                OutlinedButton(
                    onClick = { uriHandler.openUri(githubUrl) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_logo_github),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_github))
                }
            }
            item {
                Button(onClick = onBackClick) {
                    Text(stringResource(R.string.settings_back))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsAndAboutPanePreview() {
    PreviewContainer {
        SettingsAndAboutPaneImpl(onBackClick = {})
    }
}
