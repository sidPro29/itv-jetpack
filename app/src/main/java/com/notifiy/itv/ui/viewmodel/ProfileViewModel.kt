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

                val allVideos = itvRepository.getVideos()
                val allMovies = itvRepository.getMovies()
                val allTvShows = itvRepository.getTVShows()
                
                val purchases = stripeRepository.getUserPurchases()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = userName,
                        userEmail = userEmail,
                        activePlan = activePlan,
                        purchases = purchases,
                        watchlist = categorizeItems(watchlistIds, allVideos, allMovies, allTvShows),
                        playlist = categorizeItems(playlistIds, allVideos, allMovies, allTvShows),
                        liked = categorizeItems(likedIds, allVideos, allMovies, allTvShows)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun categorizeItems(
        ids: Set<String>,
        videos: List<Post>,
        movies: List<Post>,
        tvShows: List<Post>
    ): Map<String, List<Post>> {
        val result = mutableMapOf<String, List<Post>>()
        
        result["Videos"] = videos.filter { it.id.toString() in ids }
        result["Movies"] = movies.filter { it.id.toString() in ids }
        result["TV Shows"] = tvShows.filter { it.id.toString() in ids }
        
        return result
    }
}
