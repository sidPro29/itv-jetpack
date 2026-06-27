package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.ItvPlan
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.repository.ItvRepository
import com.notifiy.itv.data.repository.SessionManager
import com.notifiy.itv.data.repository.StripeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: ItvRepository,
    private val stripeRepository: StripeRepository
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    private val _postTags = MutableStateFlow<String>("")
    val postTags = _postTags.asStateFlow()

    private val _activePlan = MutableStateFlow<String?>(null)
    val activePlan = _activePlan.asStateFlow()

    private val _recommendedMovies = MutableStateFlow<List<Post>>(emptyList())
    val recommendedMovies = _recommendedMovies.asStateFlow()

    private val _upcomingMovies = MutableStateFlow<List<Post>>(emptyList())
    val upcomingMovies = _upcomingMovies.asStateFlow()

    private val _isInWatchlist = MutableStateFlow(false)
    val isInWatchlist = _isInWatchlist.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked = _isLiked.asStateFlow()

    private val _isInPlaylist = MutableStateFlow(false)
    val isInPlaylist = _isInPlaylist.asStateFlow()

    private var cachedPlans: List<ItvPlan>? = null

    fun loadDetails(postId: Int) {
        _activePlan.value = sessionManager.fetchActivePlan()
        checkStatus(postId)
        viewModelScope.launch {
            // Load plans for pricing comparison
            cachedPlans = stripeRepository.getMembershipLevels()

            val movies = repository.getMovies()
            val videos = repository.getVideos()
            val tvShows = repository.getTVShows()
            val allPosts = movies + videos + tvShows
            
            val mappedPosts = repository.getAllAssetsWithTags()
            val matchedPost = mappedPosts.find { it.first.id == postId }
            
            if (matchedPost != null) {
                _post.value = matchedPost.first
                val tagsList = matchedPost.second
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
                    .distinct()
                    
                _postTags.value = tagsList.joinToString(" • ")
            } else {
                _post.value = allPosts.find { it.id == postId }
                _postTags.value = "Category • Genre"
            }
            
            _recommendedMovies.value = movies.shuffled().take(10)
            _upcomingMovies.value = movies.shuffled().take(10)
        }
    }

    fun checkStatus(id: Int) {
        _isInWatchlist.value = sessionManager.isInWatchlist(id)
        _isLiked.value = sessionManager.isLiked(id)
        _isInPlaylist.value = sessionManager.isInPlaylist(id)
    }

    fun toggleWatchlist(id: Int) {
        sessionManager.toggleWatchlist(id)
        _isInWatchlist.value = sessionManager.isInWatchlist(id)
    }

    fun toggleLiked(id: Int) {
        sessionManager.toggleLiked(id)
        _isLiked.value = sessionManager.isLiked(id)
    }

    fun togglePlaylist(id: Int) {
        sessionManager.togglePlaylist(id)
        _isInPlaylist.value = sessionManager.isInPlaylist(id)
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun canWatch(): Boolean {
        val currentPost = _post.value ?: return false
        val reqPlans = currentPost.membershipPlanList ?: return true
        if (reqPlans.isEmpty()) return true
        
        if (reqPlans.any { it.planName?.contains("free", ignoreCase = true) == true }) {
            return true
        }

        val activePlanName = sessionManager.fetchActivePlan() ?: return false
        val allPlans = cachedPlans ?: return false
        
        val activePlan = allPlans.find { it.name == activePlanName } ?: return false
        
        var contentMinAmount = Double.MAX_VALUE
        var directMatch = false
        
        for (req in reqPlans) {
            if (req.planName == activePlanName) {
                directMatch = true
                break
            }
            val foundPlan = allPlans.find { it.name == req.planName || it.id == req.planId }
            if (foundPlan != null && foundPlan.price < contentMinAmount) {
                contentMinAmount = foundPlan.price
            }
        }
        
        if (directMatch) return true
        if (contentMinAmount == Double.MAX_VALUE) return false
        
        return activePlan.price >= contentMinAmount
    }
}
