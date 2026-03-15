package com.notifiy.itv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.repository.ItvRepository
import com.notifiy.itv.data.repository.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: ItvRepository
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

    fun loadDetails(postId: Int) {
        _activePlan.value = sessionManager.fetchActivePlan()
        checkStatus(postId)
        viewModelScope.launch {
            val movies = repository.getMovies()
            val videos = repository.getVideos()
            val tvShows = repository.getTVShows()
            val allPosts = movies + videos + tvShows
            
            // Fetch tags from Firebase
            val firebasePosts = repository.getFirebasePosts()
            val matchedFirebasePost = firebasePosts?.find { it.first.id == postId }
            
            if (matchedFirebasePost != null) {
                _post.value = matchedFirebasePost.first
                // The second item in the pair is the list of tags
                val tagsList = matchedFirebasePost.second
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
                    .distinct()
                    
                _postTags.value = tagsList.joinToString(" • ")
            } else {
                _post.value = allPosts.find { it.id == postId }
                _postTags.value = "Science • News • Space" // Fallback
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

    fun updateAsset(
        assetId: String,
        title: String,
        videoUrl: String,
        imageUrl: String,
        membershipLevel: String,
        rowName: String,
        tags: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateAssetInFirebase(assetId, title, videoUrl, imageUrl, membershipLevel, rowName, tags)
            loadDetails(assetId.toIntOrNull() ?: 0)
            onComplete()
        }
    }

    suspend fun getFirebaseAssetRaw(assetId: Int): Map<String, Any>? {
        return repository.getFirebaseAssetRaw(assetId)
    }
}
