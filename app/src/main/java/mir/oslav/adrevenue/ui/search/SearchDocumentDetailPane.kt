@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package mir.oslav.adrevenue.ui.search

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.search.DocumentationSearchResult
import mir.oslav.adrevenue.ui.Destination
import mir.oslav.adrevenue.ui.PreviewContainer
import mir.oslav.adrevenue.ui.PreviewUI

@Composable
fun SearchDocumentDetailPane(
    result: DocumentationSearchResult,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SearchDocumentDetailPaneImpl(
        result = result,
        onNavigate = onNavigate,
        onOpenSource = { context.openCustomTab(url = result.url) },
        modifier = modifier,
    )
}

@Composable
private fun SearchDocumentDetailPaneImpl(
    result: DocumentationSearchResult,
    onNavigate: (Destination) -> Unit,
    onOpenSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val markdown = markdownToAnnotatedString(markdown = result.snippet)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = result.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(Destination.Search) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(id = R.string.search_detail_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSource) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(
                                id = R.string.search_detail_open_source,
                            ),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = paddingValues.calculateTopPadding() + 20.dp,
                end = 20.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp),
        ) {
            item {
                Text(
                    text = result.url,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                Text(
                    text = markdown,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@PreviewUI
@Composable
private fun SearchDocumentDetailPanePreview() {
    PreviewContainer {
        SearchDocumentDetailPaneImpl(
            result = DocumentationSearchResult(
                title = "AdMob network report",
                snippet = """# Network report

Use **estimated earnings** to understand your revenue.

- Choose a date range
- Review `ESTIMATED_EARNINGS`
- [Open the documentation](https://developers.google.com/admob/api/v1/reporting)
""".trimIndent(),
                url = "https://developers.google.com/admob/api/v1/reporting",
            ),
            onNavigate = {},
            onOpenSource = {},
        )
    }
}

private fun Context.openCustomTab(url: String) {
    val uri = url.toUri()
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, uri)
    } catch (_: Exception) {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
