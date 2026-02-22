package com.notifiy.itv.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.remote.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItvRepository @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
) {
    // In-memory cache variables to store data
    private var cachedVideos: List<Post>? = null
    private var cachedMovies: List<Post>? = null
    private var cachedTvShows: List<Post>? = null

    private val gson = Gson()

    suspend fun getVideos(): List<Post> {
        // 1. Check Memory Cache
        internalCachedVideos()?.let { return it }
        
        // 2. Check File Cache
        val fileData = readFromCache("videos.json")
        if (fileData != null) {
            cachedVideos = fileData
            return fileData
        }
        
        // 3. Fetch from Server
        val response = apiService.getVideos()
        cachedVideos = response
        saveToCache("videos.json", response)
        return response
    }

    suspend fun getMovies(): List<Post> {
        internalCachedMovies()?.let { return it }
        
        val fileData = readFromCache("movies.json")
        if (fileData != null) {
            cachedMovies = fileData
            return fileData
        }
        
        val response = apiService.getMovies()
        cachedMovies = response
        saveToCache("movies.json", response)
        return response
    }

    suspend fun getTVShows(): List<Post> {
        internalCachedTvShows()?.let { return it }
        
        val fileData = readFromCache("tvshows.json")
        if (fileData != null) {
            cachedTvShows = fileData
            return fileData
        }
        
        val response = apiService.getTVShows()
        cachedTvShows = response
        saveToCache("tvshows.json", response)
        return response
    }

    suspend fun clearCache() {
        cachedVideos = null
        cachedMovies = null
        cachedTvShows = null
        withContext(Dispatchers.IO) {
            File(context.filesDir, "videos.json").delete()
            File(context.filesDir, "movies.json").delete()
            File(context.filesDir, "tvshows.json").delete()
        }
    }

    // Helper functions for safe property access
    private fun internalCachedVideos() = cachedVideos
    private fun internalCachedMovies() = cachedMovies
    private fun internalCachedTvShows() = cachedTvShows

    // File Cache Logic
    private suspend fun saveToCache(filename: String, data: List<Post>) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, filename)
                val json = gson.toJson(data)
                file.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun readFromCache(filename: String): List<Post>? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, filename)
                if (file.exists()) {
                    val json = file.readText()
                    val type = object : TypeToken<List<Post>>() {}.type
                    gson.fromJson(json, type)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
