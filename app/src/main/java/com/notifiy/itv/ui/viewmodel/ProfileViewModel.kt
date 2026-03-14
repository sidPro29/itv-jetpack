package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.ItvPurchase
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.repository.ItvRepository
import com.notifiy.itv.data.repository.SessionManager
import com.notifiy.itv.data.repository.StripeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val watchlist: Map<String, List<Post>> = emptyMap(),
    val playlist: Map<String, List<Post>> = emptyMap(),
    val liked: Map<String, List<Post>> = emptyMap(),
    val purchases: List<ItvPurchase> = emptyList(),
    val userName: String = "",
    val userEmail: String = "",
    val activePlan: String? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val itvRepository: ItvRepository,
    private val stripeRepository: StripeRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userName = sessionManager.getUserName()
                val userEmail = sessionManager.getUserEmail()
                val activePlan = sessionManager.fetchActivePlan()

                val watchlistIds = sessionManager.getWatchlist()
                val playlistIds = sessionManager.getPlaylist()
                val likedIds = sessionManager.getLiked()

                val firebasePosts = itvRepository.getFirebasePosts() ?: emptyList()
                val purchases = stripeRepository.getUserPurchases()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = userName,
                        userEmail = userEmail,
                        activePlan = activePlan,
                        purchases = purchases,
                        watchlist = categorizeItems(watchlistIds, firebasePosts),
                        playlist = categorizeItems(playlistIds, firebasePosts),
                        liked = categorizeItems(likedIds, firebasePosts)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                // ID Lists
                val liveTvIds = setOf(25524, 29897, 29900, 29903, 29919, 29925, 29929, 29932, 29935, 29938)
                val top10Ids = setOf(29866, 24409, 24245, 24063, 23931, 23927, 23919, 23903, 23899)
                val bingeVideoIds = setOf(24062, 24030, 23976, 23954, 23947, 23946, 23935, 23933, 23927, 23913, 23874, 23861, 23895)
                val sciFiUniverseIds = setOf(30305, 24208, 24181, 24163, 24159, 24158, 24157, 24062, 24063, 24060, 23874)
                val documentaryFilmIds = setOf(29875, 24174, 23872, 24137, 24001, 23946, 23933, 23915, 23913, 23911, 23906, 23899)

                // Keyword Lists
                val spaceToGroundKeywords = listOf("space to ground")
                val newsKeywords = listOf("space to ground", "roscosmos weekly", "TWAN", "NASA 2025", "ESA", "Briefing", "space symposium", "jared isaacman")
                val talkShowKeywords = listOf("quantum earth", "molli and max", "jared isaacman", "tech for humanity", "interplanetary quest", "voyager space")
                val documentarySeriesKeywords = listOf("Apollo", "Quest", "Tech for humanity")
                val scienceFictionKeywords = listOf("Star Trek", "Rubikon", "Superman", "space cadet", "the first ten thousand days", "insurrection", "the martian", "red planet", "mission to mars") 

                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                // Fetch existing IDs to avoid duplicates
                val snapshot = db.collection("itv_assets").get().await()
                val existingIds = snapshot.documents.map { it.id }.toSet()
                
                val videos = itvRepository.getVideos()
                val movies = itvRepository.getMovies()
                val tvShows = itvRepository.getTVShows()

                val allPosts = videos.map { Pair(it, "videos") } + 
                               movies.map { Pair(it, "movies") } + 
                               tvShows.map { Pair(it, "tvshows") }

                allPosts.forEach { (post, cat) ->
                    val postIdStr = post.id.toString()
                    if (!existingIds.contains(postIdStr)) {
                        val tags = mutableListOf<String>()
                        if (post.id in liveTvIds) tags.add("LiveTV")
                        if (post.id in top10Ids) tags.add("Our Top 10")
                        if (post.id in bingeVideoIds) tags.add("Binge Videos")
                        if (post.id in sciFiUniverseIds) tags.add("The sci-fi universe")
                        if (post.id in documentaryFilmIds) tags.add("Documentry Film")

                        val titleLower = post.title.rendered.lowercase()
                        if (spaceToGroundKeywords.any { titleLower.contains(it.lowercase()) }) tags.add("space-to-ground Report")
                        if (newsKeywords.any { titleLower.contains(it.lowercase()) }) tags.add("News")
                        if (talkShowKeywords.any { titleLower.contains(it.lowercase()) }) tags.add("Talk show")
                        if (documentarySeriesKeywords.any { titleLower.contains(it.lowercase()) }) tags.add("Doccumentry series")
                        if (scienceFictionKeywords.any { titleLower.contains(it.lowercase()) }) tags.add("Science Fiction")
                        
                        if (cat == "movies") tags.add("Must-watch space epic")
                        if (cat == "tvshows") tags.add("Binge- Epic series")

                        val rowName = tags.firstOrNull() ?: cat

                        val isTrailer = post.title.rendered.contains("trailer", true) || post.content.rendered.contains("trailer", true)

                        val data = hashMapOf(
                            "asset_id" to postIdStr,
                            "category" to cat,
                            "description" to post.content.rendered,
                            "genre" to "",
                            "imageUrl" to listOf(post.getDisplayImageUrl()),
                            "isTrailer" to isTrailer,
                            "membership_level" to listOf("Free"),
                            "row_name" to rowName,
                            "tags" to tags,
                            "title" to post.title.rendered,
                            "type" to cat,
                            "videoUrl" to post.getEffectiveVideoUrl()
                        )

                        db.collection("itv_assets").document(postIdStr).set(data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun categorizeItems(
        ids: Set<String>,
        firebasePosts: List<Pair<Post, List<String>>>
    ): Map<String, List<Post>> {
        val result = mutableMapOf<String, List<Post>>()
        
        result["Videos"] = firebasePosts.filter { it.first.id.toString() in ids && it.second.contains("videos") }.map { it.first }
        result["Movies"] = firebasePosts.filter { it.first.id.toString() in ids && it.second.contains("movies") }.map { it.first }
        result["TV Shows"] = firebasePosts.filter { it.first.id.toString() in ids && it.second.contains("tvshows") }.map { it.first }
        
        return result
    }
}
