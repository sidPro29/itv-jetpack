package com.notifiy.itv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.NewsArticle
import com.notifiy.itv.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewsUiState(
    val isLoading: Boolean = false,
    val articles: List<NewsArticle> = emptyList(),
    val searchResults: List<NewsArticle> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val TAG = "siddharthaLogs"
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        Log.d(TAG, "NewsViewModel: Initialized")
        loadNews()
    }

    fun loadNews(page: Int = 1) {
        Log.d(TAG, "NewsViewModel: loadNews() — page=$page")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            newsRepository.getNewsArticles(page = page, perPage = 20)
                .onSuccess { articles ->
                    Log.d(TAG, "NewsViewModel: Got ${articles.size} articles on page $page")
                    _uiState.update { state ->
                        val list = if (page == 1) articles else state.articles + articles
                        state.copy(
                            isLoading = false,
                            articles = list,
                            currentPage = page,
                            hasMorePages = articles.size == 20
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "NewsViewModel: loadNews failed: ${e.message}", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load news") }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        Log.d(TAG, "NewsViewModel: onSearchQueryChanged query='$query'")
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _uiState.update { it.copy(isSearching = true) }
            Log.d(TAG, "NewsViewModel: Searching for '$query'")
            newsRepository.searchNewsArticles(query)
                .onSuccess { results ->
                    Log.d(TAG, "NewsViewModel: Search '$query' returned ${results.size} results")
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "NewsViewModel: Search failed: ${e.message}", e)
                    _uiState.update { it.copy(isSearching = false) }
                }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMorePages) return
        Log.d(TAG, "NewsViewModel: loadNextPage — page=${state.currentPage + 1}")
        loadNews(page = state.currentPage + 1)
    }

    fun refresh() {
        Log.d(TAG, "NewsViewModel: refresh()")
        loadNews(page = 1)
    }
}
