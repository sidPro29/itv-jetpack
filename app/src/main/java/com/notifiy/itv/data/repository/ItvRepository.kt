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

    private fun getInternalFile(filename: String): File {
        return File(context.filesDir, filename)
    }

    private fun loadFromInternalOrAssets(filename: String): List<Post> {
        return try {
            val internalFile = getInternalFile(filename)
            val jsonString = if (internalFile.exists()) {
                internalFile.readText()
            } else {
                context.assets.open(filename).bufferedReader().use { it.readText() }
            }
            val response = gson.fromJson(jsonString, com.notifiy.itv.data.model.AssetResponse::class.java)
            response.results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveToInternal(filename: String, posts: List<Post>) {
        try {
            val response = com.notifiy.itv.data.model.AssetResponse(
                page = 1,
                perPage = posts.size,
                total = posts.size,
                totalPages = 1,
                results = posts
            )
            val jsonString = gson.toJson(response)
            getInternalFile(filename).writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getInitialVideos(): List<Post> = withContext(Dispatchers.IO) {
        cachedVideos = loadFromInternalOrAssets("videos_data.json")
        cachedVideos ?: emptyList()
    }

    suspend fun getInitialMovies(): List<Post> = withContext(Dispatchers.IO) {
        cachedMovies = loadFromInternalOrAssets("movies_data.json")
        cachedMovies ?: emptyList()
    }

    suspend fun getInitialTVShows(): List<Post> = withContext(Dispatchers.IO) {
        cachedTvShows = loadFromInternalOrAssets("tvshows_data.json")
        cachedTvShows ?: emptyList()
    }

    suspend fun updateVideos(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val results = apiService.getVideos().results
            cachedVideos = results
            saveToInternal("videos_data.json", results)
            results
        } catch (e: Exception) {
            e.printStackTrace()
            cachedVideos ?: emptyList()
        }
    }

    suspend fun updateMovies(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val results = apiService.getMovies().results
            cachedMovies = results
            saveToInternal("movies_data.json", results)
            results
        } catch (e: Exception) {
            e.printStackTrace()
            cachedMovies ?: emptyList()
        }
    }

    suspend fun updateTVShows(): List<Post> = withContext(Dispatchers.IO) {
        try {
            val results = apiService.getTVShows().results
            cachedTvShows = results
            saveToInternal("tvshows_data.json", results)
            results
        } catch (e: Exception) {
            e.printStackTrace()
            cachedTvShows ?: emptyList()
        }
    }

    suspend fun getVideos(): List<Post> {
        return cachedVideos ?: getInitialVideos()
    }

    suspend fun getMovies(): List<Post> {
        return cachedMovies ?: getInitialMovies()
    }

    suspend fun getTVShows(): List<Post> {
        return cachedTvShows ?: getInitialTVShows()
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
            val titleStr = post.title.rendered.lowercase()
            val descStr = post.description?.lowercase() ?: ""
            val tagsStr = post.tag?.lowercase() ?: ""
            val genreStr = post.genre?.lowercase() ?: ""
            
            val combinedText = "$tagsStr $genreStr $descStr"
            
            if (tagsStr.contains("live 24/7")) tags.add("Live TV")
            if (tagsStr.contains("our top 10")) tags.add("Our Top 10")
            if (tagsStr.contains("binge videos")) tags.add("Binge Videos")
            // Item 4 & 5 (all tvshows, all movies) handled at ViewModel level or by category tag
            if (titleStr.contains("space to ground")) tags.add("Space-to-Ground Report")
            if (combinedText.contains("news")) tags.add("News")
            if (combinedText.contains("talk show")) tags.add("Talk-Shows")
            if (combinedText.contains("documentary series")) tags.add("Documentary Series")
            if (combinedText.contains("documentary film")) tags.add("Documentary Film")
            if (combinedText.contains("science fiction")) tags.add("Science-Fiction")
            
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

