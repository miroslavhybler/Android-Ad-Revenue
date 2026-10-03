@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package mir.oslav.adrevenue.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mir.oslav.adrevenue.R
import mir.oslav.adrevenue.data.search.DocumentationSearchResult
import mir.oslav.adrevenue.ui.PreviewContainer
import mir.oslav.adrevenue.ui.PreviewUI
import mir.oslav.adrevenue.ui.Destination

@Composable
fun SearchPane(
    onNavigate: (Destination) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    SearchPaneImpl(
        state = state.value,
        onQueryChanged = viewModel::search,
        onResultClicked = { result ->
            viewModel.addHistory(item = result)
            onNavigate(Destination.SearchDocumentDetail(result = result))
        },
        onHistoryItemRemoved = viewModel::removeHistory,
    )
}

@Composable
private fun SearchPaneImpl(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onResultClicked: (DocumentationSearchResult) -> Unit,
    onHistoryItemRemoved: (DocumentationSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayedItems = if (state.query.trim().length >= 3) state.results else state.history

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = state.query,
                    onValueChange = onQueryChanged,
                    label = { Text(text = stringResource(id = R.string.search_hint)) },
                    singleLine = true,
                )

                Box(modifier = Modifier.height(height = 4.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.isLoading
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            contentPadding = paddingValues,
        ) {
            items(
                items = displayedItems,
                key = { result -> result.url },
            ) { result ->
                SearchResultItem(
                    result = result,
                    onClick = { onResultClicked(result) },
                    onRemove = { onHistoryItemRemoved(result) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: DocumentationSearchResult,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row() {
            Column(
                modifier = Modifier.padding(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 6.dp),
            ) {
                Text(text = result.title, fontWeight = FontWeight.SemiBold)
                Text(text = result.snippet, maxLines = 2)
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(id = R.string.search_remove_history),
                )
            }
        }
    }
}

@PreviewUI
@Composable
private fun SearchPanePreview() {
    val history = listOf(
        DocumentationSearchResult(
            title = "AdMob network report",
            snippet = "Generate a report containing AdMob network performance metrics.",
            url = "https://developers.google.com/admob/api/v1/reporting",
        ),
        DocumentationSearchResult(
            title = "AdMob API overview",
            snippet = "Use the AdMob API to access account and reporting resources.",
            url = "https://developers.google.com/admob/api",
        ),
    )
    PreviewContainer {
        SearchPaneImpl(
            state = SearchUiState(history = history),
            onQueryChanged = {},
            onResultClicked = {},
            onHistoryItemRemoved = {},
        )
    }
}
