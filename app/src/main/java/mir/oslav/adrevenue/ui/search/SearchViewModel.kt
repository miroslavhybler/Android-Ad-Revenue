package mir.oslav.adrevenue.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mir.oslav.adrevenue.data.search.DeveloperKnowledgeClient
import mir.oslav.adrevenue.data.search.DocumentationSearchResult
import mir.oslav.adrevenue.utils.updateCopy

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val client: DeveloperKnowledgeClient,
) : ViewModel() {
    private val _state = MutableStateFlow(value = SearchUiState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null

    fun search(query: String) {
        val normalized = query.trim()
        _state.updateCopy {
            copy(query = query, isLoading = normalized.length >= 3, message = null)
        }
        if (normalized.length < 3) {
            _state.updateCopy { copy(results = emptyList()) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(timeMillis = SEARCH_DEBOUNCE_MILLIS)
            runCatching { client.search(query= normalized) }
                .onSuccess { results ->
                    _state.updateCopy { copy(results = results, isLoading = false) }
                }
                .onFailure { error ->
                    _state.updateCopy { copy(isLoading = false, message = error.message) }
                }
        }
    }

    fun addHistory(item: DocumentationSearchResult) {
        _state.updateCopy { copy(history = listOf(item) + history.filterNot { it.url == item.url }) }
    }

    fun removeHistory(result: DocumentationSearchResult) {
        _state.updateCopy { copy(history = history.filterNot { it.url == result.url }) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 600L
    }
}
