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

    fun loadData(type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val items = when (type) {
                    "Movies" -> repository.getMovies()
                    "TV Shows" -> repository.getTVShows()
                    // Fallback or other categories
                    else -> emptyList()
                }
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
