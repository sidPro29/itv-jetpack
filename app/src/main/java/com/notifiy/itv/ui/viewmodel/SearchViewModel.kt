package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.repository.ItvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchItem(
    val post: Post,
    val type: String // "Tvshow", "Video", "Movie"
)

data class SearchUiState(
    val isLoading: Boolean = false,
    val allItems: List<SearchItem> = emptyList(),
    val filteredItems: List<SearchItem> = emptyList(),
    val selectedFilter: String = "All",
    val searchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ItvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val videosDeferred = async { repository.getVideos() }
                val moviesDeferred = async { repository.getMovies() }
                val tvShowsDeferred = async { repository.getTVShows() }

                val videos = videosDeferred.await().map { SearchItem(it, "Video") }
                val movies = moviesDeferred.await().map { SearchItem(it, "Movie") }
                val tvShows = tvShowsDeferred.await().map { SearchItem(it, "Tvshow") }

                val allItems = (videos + movies + tvShows).shuffled()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allItems = allItems,
                        filteredItems = allItems
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterItems()
    }

    fun onSearch() {
        filterItems()
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        filterItems()
    }

    private fun filterItems() {
        val currentState = _uiState.value
        val query = currentState.searchQuery.trim().lowercase()
        val filter = currentState.selectedFilter

        val filtered = currentState.allItems.filter { item ->
            val matchesQuery = if (query.isEmpty()) true else item.post.title.rendered.lowercase().contains(query)
            val matchesFilter = when (filter) {
                "All" -> true
                "Video" -> item.type == "Video" || item.type == "Movie"
                else -> item.type == filter
            }
            
            matchesQuery && matchesFilter
        }
        
        _uiState.update { it.copy(filteredItems = filtered) }
    }
}
