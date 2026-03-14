package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.repository.ItvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val isLoading: Boolean = false,
    val items: List<Post> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: ItvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState = _uiState.asStateFlow()

    // Removed static ID lists in favor of Firebase tags

    fun loadData(categoryName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch the mapped posts from Firebase locally (cached)
                val firebasePosts = repository.getFirebasePosts() ?: emptyList()
                
                val items = firebasePosts.filter { p ->
                    // Map the top-level 'type' or 'categoryName'
                    when (categoryName) {
                        "Movies" -> p.second.contains("movies")
                        "TV Shows" -> p.second.contains("tvshows")
                        "Videos" -> p.second.contains("videos")
                        // Everything else should just pull anything with that exactly matching tag 
                        // Note: Our Firebase tags are "News", "Doccumentry series", "Science Fiction", etc.
                        else -> p.second.contains(categoryName)
                    }
                }.map { it.first }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items
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
}
