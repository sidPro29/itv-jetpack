package com.notifiy.itv.data.repository

import com.notifiy.itv.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItvRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getVideos() = apiService.getVideos()
    suspend fun getMovies() = apiService.getMovies()
    suspend fun getTVShows() = apiService.getTVShows()
}
