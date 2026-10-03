package mir.oslav.adrevenue.ui.search

import mir.oslav.adrevenue.data.search.DocumentationSearchResult

data class SearchUiState constructor(
    val query: String = "",
    val results: List<DocumentationSearchResult> = emptyList(),
    val history: List<DocumentationSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
)
