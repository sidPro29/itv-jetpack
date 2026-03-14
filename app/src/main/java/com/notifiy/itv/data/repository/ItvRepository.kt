package com.notifiy.itv.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.remote.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
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
    private var cachedFirebasePosts: List<Pair<Post, List<String>>>? = null

    private val gson = Gson()

    suspend fun getVideos(): List<Post> {
        return try {
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
            response
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMovies(): List<Post> {
        return try {
            internalCachedMovies()?.let { return it }
            
            val fileData = readFromCache("movies.json")
            if (fileData != null) {
                cachedMovies = fileData
                return fileData
            }
            
            val response = apiService.getMovies()
            cachedMovies = response
            saveToCache("movies.json", response)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTVShows(): List<Post> {
        return try {
            internalCachedTvShows()?.let { return it }
            
            val fileData = readFromCache("tvshows.json")
            if (fileData != null) {
                cachedTvShows = fileData
                return fileData
            }
            
            val response = apiService.getTVShows()
            cachedTvShows = response
            saveToCache("tvshows.json", response)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun clearCache() {
        cachedVideos = null
        cachedMovies = null
        cachedTvShows = null
        cachedFirebasePosts = null
        withContext(Dispatchers.IO) {
            File(context.filesDir, "videos.json").delete()
            File(context.filesDir, "movies.json").delete()
            File(context.filesDir, "tvshows.json").delete()
        }
    }

    suspend fun getFirebasePosts(): List<Pair<Post, List<String>>>? {
        cachedFirebasePosts?.let { return it }

        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = db.collection("itv_assets").get().await()
            
            if (snapshot.isEmpty) return null

            val videos = getVideos()
            val movies = getMovies()
            val tvShows = getTVShows()
            val allWpPosts = (videos + movies + tvShows).associateBy { it.id.toString() }

            val firebasePosts = snapshot.documents.mapNotNull { doc ->
                val assetId = doc.getString("asset_id") ?: return@mapNotNull null
                val wpPost = allWpPosts[assetId]
                
                val title = doc.getString("title")?.takeIf { it.isNotEmpty() } ?: wpPost?.title?.rendered ?: ""
                val desc = doc.getString("description")?.takeIf { it.isNotEmpty() } ?: wpPost?.content?.rendered ?: ""
                val vUrl = doc.getString("videoUrl")?.takeIf { it.isNotEmpty() } ?: wpPost?.getEffectiveVideoUrl() ?: ""
                val iUrlList = doc.get("imageUrl") as? List<*>
                val imageUrl = iUrlList?.firstOrNull()?.toString()?.takeIf { it.isNotEmpty() } ?: wpPost?.getDisplayImageUrl() ?: ""
                val tags = doc.get("tags") as? List<*> ?: emptyList<Any>()
                val stringTags = tags.map { it.toString() }
                val rowName = doc.getString("row_name") ?: ""
                val type = doc.getString("type") ?: ""
                val category = doc.getString("category") ?: ""
                
                val mappedPost = Post(
                    id = assetId.toIntOrNull() ?: wpPost?.id ?: System.currentTimeMillis().toInt(),
                    title = com.notifiy.itv.data.model.Rendered(title),
                    content = com.notifiy.itv.data.model.Rendered(desc),
                    excerpt = com.notifiy.itv.data.model.Rendered(desc),
                    date = wpPost?.date ?: "",
                    link = wpPost?.link ?: "",
                    featuredMedia = wpPost?.featuredMedia ?: 0,
                    videoUrl = vUrl,
                    videoEmbed = wpPost?.videoEmbed,
                    videoChoice = wpPost?.videoChoice,
                    portraitPoster = imageUrl,
                    portraitImage = null, // Always use portraitPoster
                    subtitles = wpPost?.subtitles,
                    _embedded = null // Always use portraitPoster
                )
                Pair(mappedPost, stringTags + listOf(rowName, type, category))
            }

            cachedFirebasePosts = firebasePosts
            firebasePosts
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
