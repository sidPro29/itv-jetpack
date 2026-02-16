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

    fun loadData(type: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch videos first as most categories depend on it
                // Since repository is cached, this is cheap
                val allVideos = repository.getVideos()
                
                // Keyword filtering helper
                fun filterByKeywords(items: List<Post>, keywords: List<String>): List<Post> {
                    return items.filter { post ->
                        val title = post.title.rendered.lowercase()
                        keywords.any { keyword -> title.contains(keyword.lowercase()) }
                    }
                }

                val items = when (type) {
                    "Movies" -> repository.getMovies()
                    "TV Shows" -> repository.getTVShows()
                    "Videos" -> allVideos
                    "News" -> filterByKeywords(allVideos, newsKeywords)
                    "Documentary Films" -> allVideos.filter { it.id in documentaryFilmIds }
                    "Documentary Series" -> filterByKeywords(allVideos, documentarySeriesKeywords)
                    "Science-Fiction" -> filterByKeywords(allVideos, scienceFictionKeywords)
                    // Fallback
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
