package com.aniplex.app.presentation.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aniplex.app.domain.model.Anime
import com.aniplex.app.domain.model.Result
import com.aniplex.app.domain.repository.AnimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<Anime>, val hasNextPage: Boolean) : SearchUiState
    data class Error(val message: String) : SearchUiState
    data object Empty : SearchUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: AnimeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<Anime>>(emptyList())
    val suggestions: StateFlow<List<Anime>> = _suggestions.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Filter states
    val selectedType = MutableStateFlow<String?>(null)
    val selectedStatus = MutableStateFlow<String?>(null)
    val selectedSort = MutableStateFlow<String?>(null)
    val selectedLanguage = MutableStateFlow<String?>(null)
    val selectedGenres = MutableStateFlow<Set<String>>(emptySet())

    private var currentPage = 1
    private var isCurrentlyLoadingNextPage = false
    private val allResults = mutableListOf<Anime>()

    init {
        // Debounce search suggestions by 400ms
        viewModelScope.launch {
            searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank() && query.length >= 2) {
                        repository.getSuggestions(query).collect { result ->
                            if (result is Result.Success) {
                                _suggestions.value = result.data
                            }
                        }
                    } else {
                        _suggestions.value = emptyList()
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun performSearch(isNewSearch: Boolean = true) {
        if (isNewSearch) {
            currentPage = 1
            allResults.clear()
            _uiState.value = SearchUiState.Loading
        } else {
            isCurrentlyLoadingNextPage = true
        }

        val queryVal = _searchQuery.value.trim()
        val typeVal = selectedType.value
        val statusVal = selectedStatus.value
        val sortVal = selectedSort.value
        val langVal = selectedLanguage.value
        val genresVal = selectedGenres.value.joinToString(",")

        val hasFilters = typeVal != null || statusVal != null || sortVal != null || langVal != null || genresVal.isNotEmpty()

        viewModelScope.launch {
            val flowResult = if (hasFilters) {
                // If filters are present, use the advanced filter endpoint
                repository.filterAnime(
                    type = typeVal,
                    status = statusVal,
                    genres = if (genresVal.isEmpty()) null else genresVal,
                    sort = sortVal,
                    language = langVal,
                    page = currentPage
                )
            } else {
                // Otherwise use the standard text search endpoint
                if (queryVal.isEmpty()) {
                    _uiState.value = SearchUiState.Idle
                    return@launch
                }
                repository.search(queryVal, currentPage)
            }

            flowResult.collect { result ->
                when (result) {
                    is Result.Loading -> {
                        if (isNewSearch) _uiState.value = SearchUiState.Loading
                    }
                    is Result.Success -> {
                        isCurrentlyLoadingNextPage = false
                        val newItems = result.data
                        allResults.addAll(newItems)
                        
                        if (allResults.isEmpty()) {
                            _uiState.value = SearchUiState.Empty
                        } else {
                            // Assume next page is available if we got a full batch of items (usually 20+)
                            val hasNext = newItems.size >= 15
                            _uiState.value = SearchUiState.Success(allResults.toList(), hasNext)
                        }
                    }
                    is Result.Error -> {
                        isCurrentlyLoadingNextPage = false
                        if (isNewSearch) {
                            _uiState.value = SearchUiState.Error(result.message)
                        }
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state is SearchUiState.Success && state.hasNextPage && !isCurrentlyLoadingNextPage) {
            currentPage++
            performSearch(isNewSearch = false)
        }
    }

    fun toggleGenre(genre: String) {
        val current = selectedGenres.value
        selectedGenres.value = if (current.contains(genre)) {
            current - genre
        } else {
            current + genre
        }
    }

    fun clearFilters() {
        selectedType.value = null
        selectedStatus.value = null
        selectedSort.value = null
        selectedLanguage.value = null
        selectedGenres.value = emptySet()
    }
}
