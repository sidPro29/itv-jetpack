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
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    // ID Lists
    private val liveTvIds = setOf(29897, 29900, 29903, 29919, 29925, 29929, 29932, 29935, 29938)
    private val top10Ids = setOf(29866, 24409, 24245, 24063, 23931, 23927, 23919, 23903, 23899)
    private val bingeVideoIds = setOf(24062, 24030, 23976, 23954, 23947, 23946, 23935, 23933, 23927, 23913, 23874, 23861, 23895)
    private val sciFiUniverseIds = setOf(30305, 24208, 24181, 24163, 24159, 24158, 24157, 24062, 24063, 24060, 23874)
    private val documentaryFilmIds = setOf(29875, 24174, 23872, 24137, 24001, 23946, 23933, 23915, 23913, 23911, 23906, 23899)

    // Keyword Lists (Lowercase for case-insensitive matching)
    private val spaceToGroundKeywords = listOf("space to ground")
    private val newsKeywords = listOf("space to ground", "roscosmos weekly", "TWAN", "NASA 2025", "ESA", "Briefing", "space symposium", "jared isaacman")
    private val talkShowKeywords = listOf("quantum earth", "molli and max", "jared isaacman", "tech for humanity", "interplanetary quest", "voyager space")
    private val documentarySeriesKeywords = listOf("Apollo", "Quest", "Tech for humanity")
    private val scienceFictionKeywords = listOf("Star Trek", "Rubikon", "Superman", "space cadet", "the first ten thousand days", "insurrection", "the martian", "red planet", "mission to mars") 

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

                // Processing Lists
                val liveTv = videos.filter { it.id in liveTvIds }
                val top10 = videos.filter { it.id in top10Ids }
                val bingeVideos = videos.filter { it.id in bingeVideoIds }
                val sciFiUniverse = videos.filter { it.id in sciFiUniverseIds }
                val documentaryFilms = videos.filter { it.id in documentaryFilmIds }
                
                // Keyword filtering helper
                fun filterByKeywords(items: List<Post>, keywords: List<String>): List<Post> {
                    return items.filter { post ->
                        val title = post.title.rendered.lowercase()
                        keywords.any { keyword -> title.contains(keyword.lowercase()) }
                    }
                }

                val spaceToGround = filterByKeywords(videos, spaceToGroundKeywords)
                val news = filterByKeywords(videos, newsKeywords)
                val talkShows = filterByKeywords(videos, talkShowKeywords)
                val documentarySeries = filterByKeywords(videos, documentarySeriesKeywords)
                val scienceFiction = filterByKeywords(videos, scienceFictionKeywords)


                _uiState.update {
                    it.copy(
                        isLoading = false,
                        liveTv = liveTv,
                        top10 = top10,
                        bingeVideos = bingeVideos,
                        bingeEpicSeries = tvShows, // All TV Shows
                        mustWatchSpaceEpic = movies, // All Movies
                        spaceToGround = spaceToGround,
                        sciFiUniverse = sciFiUniverse,
                        news = news,
                        talkShows = talkShows,
                        documentarySeries = documentarySeries,
                        documentaryFilms = documentaryFilms,
                        scienceFiction = scienceFiction
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
