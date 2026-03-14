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
    val sciFiUniverse: List<Post> = emptyList(),
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
            try {
                // Fetch videos/movies to load watchlist items later if needed, but repository already handles caching
                val firebasePosts = repository.getFirebasePosts() ?: emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        liveTv = firebasePosts.filter { p -> p.second.contains("LiveTV") }.map { p -> p.first },
                        top10 = firebasePosts.filter { p -> p.second.contains("Our Top 10") }.map { p -> p.first },
                        bingeVideos = firebasePosts.filter { p -> p.second.contains("Binge Videos") }.map { p -> p.first },
                        bingeEpicSeries = firebasePosts.filter { p -> p.second.contains("Binge- Epic series") }.map { p -> p.first },
                        mustWatchSpaceEpic = firebasePosts.filter { p -> p.second.contains("Must-watch space epic") }.map { p -> p.first },
                        spaceToGround = firebasePosts.filter { p -> p.second.contains("space-to-ground Report") }.map { p -> p.first },
                        sciFiUniverse = firebasePosts.filter { p -> p.second.contains("The sci-fi universe") }.map { p -> p.first },
                        news = firebasePosts.filter { p -> p.second.contains("News") }.map { p -> p.first },
                        talkShows = firebasePosts.filter { p -> p.second.contains("Talk show") }.map { p -> p.first },
                        documentarySeries = firebasePosts.filter { p -> p.second.contains("Doccumentry series") }.map { p -> p.first },
                        documentaryFilms = firebasePosts.filter { p -> p.second.contains("Documentry Film") }.map { p -> p.first },
                        scienceFiction = firebasePosts.filter { p -> p.second.contains("Science Fiction") }.map { p -> p.first },
                        watchlist = firebasePosts.map { p -> p.first }.filter { p -> p.id.toString() in sessionManager.getWatchlist() }.distinctBy { p -> p.id }
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
