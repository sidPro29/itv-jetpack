package com.notifiy.itv.data.repository

import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItvRepository @Inject constructor(
    private val apiService: ApiService
) {
    // In-memory cache variables to store data
    private var cachedVideos: List<Post>? = null
    private var cachedMovies: List<Post>? = null
    private var cachedTvShows: List<Post>? = null

    suspend fun getVideos(): List<Post> {
        // Return cached data if available
        internalCachedVideos()?.let { return it }
        
        // Fetch from server if not cached
        val response = apiService.getVideos()
        cachedVideos = response
        return response
    }

    suspend fun getMovies(): List<Post> {
        internalCachedMovies()?.let { return it }
        
        val response = apiService.getMovies()
        cachedMovies = response
        return response
    }

    suspend fun getTVShows(): List<Post> {
        internalCachedTvShows()?.let { return it }
        
        val response = apiService.getTVShows()
        cachedTvShows = response
        return response
    }

    // Helper functions for safe property access
    private fun internalCachedVideos() = cachedVideos
    private fun internalCachedMovies() = cachedMovies
    private fun internalCachedTvShows() = cachedTvShows
}
