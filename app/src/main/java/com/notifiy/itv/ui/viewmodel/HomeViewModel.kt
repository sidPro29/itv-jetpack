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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val liveTv: List<Post> = emptyList(),
    val top10: List<Post> = emptyList(),
    val bingeVideos: List<Post> = emptyList(),
    val bingeEpicSeries: List<Post> = emptyList(), // TV Shows
    val mustWatchSpaceEpic: List<Post> = emptyList(), // Movies
    val spaceToGround: List<Post> = emptyList(),
    val news: List<Post> = emptyList(),

    val talkShows: List<Post> = emptyList(),
    val documentarySeries: List<Post> = emptyList(),
    val documentaryFilms: List<Post> = emptyList(), // Video IDs
    val scienceFiction: List<Post> = emptyList(), // Video Keywords
    val watchlist: List<Post> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItvRepository,
    private val sessionManager: com.notifiy.itv.data.repository.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Phase 1: Load initial data from assets/internal storage (no loading spinner if possible)
            updateStacks(
                repository.getInitialVideos(),
                repository.getInitialMovies(),
                repository.getInitialTVShows()
            )
            
            _uiState.update { it.copy(isLoading = false) }

            // Phase 2: Update from API and refresh UI
            try {
                val videosTask = async { repository.updateVideos() }
                val moviesTask = async { repository.updateMovies() }
                val tvShowsTask = async { repository.updateTVShows() }
                
                updateStacks(
                    videosTask.await(),
                    moviesTask.await(),
                    tvShowsTask.await()
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun updateStacks(videos: List<Post>, movies: List<Post>, tvShows: List<Post>) {
        val allPosts = videos + movies + tvShows
        
        fun String.containsCaseInsensitive(query: String): Boolean {
            return this.lowercase().contains(query.lowercase())
        }

        fun Post.matches(query: String): Boolean {
            val tagsMatched = tag?.containsCaseInsensitive(query) == true
            val genreMatched = genre?.containsCaseInsensitive(query) == true
            val descMatched = description?.containsCaseInsensitive(query) == true
            return tagsMatched || genreMatched || descMatched
        }

        _uiState.update {
            it.copy(
                liveTv = allPosts.filter { p -> p.tag?.containsCaseInsensitive("live 24/7") == true }
                    .sortedByDescending { p -> p.title.rendered.containsCaseInsensitive("Live TV") },
                top10 = allPosts.filter { p -> p.tag?.containsCaseInsensitive("Our Top 10") == true },

                bingeVideos = allPosts.filter { p -> p.tag?.containsCaseInsensitive("Binge Videos") == true },
                bingeEpicSeries = tvShows, // all the tvshows
                mustWatchSpaceEpic = movies, // all the movies
                spaceToGround = allPosts.filter { p -> p.title.rendered.containsCaseInsensitive("Space to Ground") },
                news = allPosts.filter { p -> p.matches("news") },
                talkShows = allPosts.filter { p -> p.matches("talk show") },
                documentarySeries = allPosts.filter { p -> p.matches("Documentary Series") },
                documentaryFilms = allPosts.filter { p -> p.matches("Documentary film") },
                scienceFiction = allPosts.filter { p -> p.matches("science fiction") || p.matches("sci-fi") },
                watchlist = allPosts.filter { p -> p.id.toString() in sessionManager.getWatchlist() }.distinctBy { p -> p.id }

            )
        }
    }
}

