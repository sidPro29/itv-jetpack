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

data class HomeUiState(
    val isLoading: Boolean = false,
    val videos: List<Post> = emptyList(),
    val movies: List<Post> = emptyList(),
    val tvShows: List<Post> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch in parallel for better performance
                val videosDeferred = async { repository.getVideos() }
                val moviesDeferred = async { repository.getMovies() }
                val tvShowsDeferred = async { repository.getTVShows() }

                val videos = videosDeferred.await()
                val movies = moviesDeferred.await()
                val tvShows = tvShowsDeferred.await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        videos = videos,
                        movies = movies,
                        tvShows = tvShows
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
