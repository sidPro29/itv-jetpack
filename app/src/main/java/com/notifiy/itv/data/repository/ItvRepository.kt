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
        return try {
            cachedVideos ?: run {
                val response = apiService.getVideos()
                val results = response.results
                cachedVideos = results
                results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMovies(): List<Post> {
        return try {
            cachedMovies ?: run {
                val response = apiService.getMovies()
                val results = response.results
                cachedMovies = results
                results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTVShows(): List<Post> {
        return try {
            cachedTvShows ?: run {
                val response = apiService.getTVShows()
                val results = response.results
                cachedTvShows = results
                results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Combined method to replace the old Firebase Asset sync.
     * Maps all assets from WP to a list of pairs with their filtering tags.
     */
    suspend fun getAllAssetsWithTags(): List<Pair<Post, List<String>>> {
        val videos = getVideos()
        val movies = getMovies()
        val tvShows = getTVShows()

        val allPosts = videos + movies + tvShows
        
        return allPosts.map { post ->
            val tags = mutableListOf<String>()
            
            // Add category as search tag
            post.category.let { 
                tags.add(it) 
                // Add plural versions for backward compatibility with UI filters
                when (it.lowercase()) {
                    "video" -> tags.add("videos")
                    "movie" -> tags.add("movies")
                    "tvshow" -> tags.add("tvshows")
                }
            }
            
            // Add tags from the API (split by comma)
            post.tag?.let { t -> 
                tags.addAll(t.split(",").map { it.trim() }.filter { it.isNotEmpty() }) 
            }
            
            // Add genre from the API (split by comma)
            post.genre?.let { g -> 
                tags.addAll(g.split(",").map { it.trim() }.filter { it.isNotEmpty() }) 
            }
            
            // Fallback: If "Top 10" or other critical categories are in the title/desc, add them
            // This ensures the Home Stacks still work even if tags are temporarily missing in API
            val titleDesc = "${post.title} ${post.description}".lowercase()
            
            if (titleDesc.contains("top 10")) tags.add("Our Top 10")
            if (titleDesc.contains("live")) tags.add("LiveTV")
            if (titleDesc.contains("space-to-ground") || titleDesc.contains("space to ground")) tags.add("space-to-ground Report")
            if (titleDesc.contains("news")) tags.add("News")
            if (titleDesc.contains("talk show")) tags.add("Talk show")
            if (titleDesc.contains("documentary series")) tags.add("Doccumentry series")
            if (titleDesc.contains("documentary film")) tags.add("Documentry Film")
            if (titleDesc.contains("science fiction")) tags.add("Science Fiction")
            
            Pair(post, tags.distinct())
        }
    }

    // Retaining for backward compatibility with ViewModel names
    suspend fun getFirebasePosts(): List<Pair<Post, List<String>>> {
        return getAllAssetsWithTags()
    }

    suspend fun clearCache() {
        cachedVideos = null
        cachedMovies = null
        cachedTvShows = null
    }
}
