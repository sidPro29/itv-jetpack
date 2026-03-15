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
                val assetId = doc.getString("asset_id") ?: doc.id.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val wpPost = allWpPosts[assetId]
                
                // Handle nested structure: row_name, tags, type might be inside membership_level map
                val membershipLevelRaw = doc.get("membership_level")
                val membershipLevelMap = membershipLevelRaw as? Map<*, *>
                
                val title = doc.getString("title")?.takeIf { it.isNotEmpty() } 
                    ?: membershipLevelMap?.get("title")?.toString()?.takeIf { it.isNotEmpty() }
                    ?: wpPost?.title?.rendered ?: ""
                
                val desc = doc.getString("description")?.takeIf { it.isNotEmpty() } 
                    ?: membershipLevelMap?.get("description")?.toString()?.takeIf { it.isNotEmpty() }
                    ?: wpPost?.content?.rendered ?: ""
                
                val vUrl = doc.getString("videoUrl")?.takeIf { it.isNotEmpty() } 
                    ?: membershipLevelMap?.get("videoUrl")?.toString()?.takeIf { it.isNotEmpty() }
                    ?: wpPost?.getEffectiveVideoUrl() ?: ""
                
                // Flexible Image URL handling
                val imageUrlRaw = doc.get("imageUrl") ?: membershipLevelMap?.get("imageUrl")
                val imageUrl = when (imageUrlRaw) {
                    is List<*> -> imageUrlRaw.firstOrNull()?.toString()
                    is String -> imageUrlRaw
                    else -> null
                }?.takeIf { it.isNotEmpty() } ?: wpPost?.getDisplayImageUrl() ?: ""

                // Extract tags, row_name, type from top level OR nested membership_level map
                val tagsRaw = doc.get("tags") ?: membershipLevelMap?.get("tags")
                val stringTags = when (tagsRaw) {
                    is List<*> -> tagsRaw.map { it.toString() }
                    is String -> tagsRaw.split(",").map { it.trim() }
                    else -> emptyList()
                }
                
                val category = doc.getString("category") ?: membershipLevelMap?.get("category")?.toString() ?: ""
                val genre = doc.getString("genre") ?: membershipLevelMap?.get("genre")?.toString() ?: ""
                val rowName = doc.getString("row_name") ?: membershipLevelMap?.get("row_name")?.toString() ?: ""
                val type = doc.getString("type") ?: membershipLevelMap?.get("type")?.toString() ?: ""
                
                val membershipLevel = when (membershipLevelRaw) {
                    is List<*> -> membershipLevelRaw.map { it.toString() }
                    is Map<*, *> -> {
                        // If it's a map, the actual "level" name might be in a field like 'name' or just use a default
                        listOf(membershipLevelMap?.get("name")?.toString() ?: "premium")
                    }
                    is String -> membershipLevelRaw.split(",").map { it.trim() }
                    else -> wpPost?.membershipLevel
                }

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
                    portraitImage = null,
                    membershipLevel = membershipLevel,
                    subtitles = wpPost?.subtitles,
                    _embedded = null
                )
                
                val filteringTags = (stringTags + listOf(category, genre, rowName, type))
                    .filter { it.isNotBlank() }
                    .distinct()
                
                android.util.Log.d("FirebaseSync", "Mapped asset $assetId with tags: $filteringTags")
                Pair(mappedPost, filteringTags)
            }

            cachedFirebasePosts = firebasePosts
            android.util.Log.d("FirebaseSync", "Finished syncing ${firebasePosts.size} posts")
            firebasePosts
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getFirebaseAssetRaw(assetId: Int): Map<String, Any>? {
        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val queryById = db.collection("itv_assets").whereEqualTo("asset_id", assetId.toString()).get().await()
            val doc = if (!queryById.isEmpty) {
                queryById.documents.first()
            } else {
                db.collection("itv_assets").document(assetId.toString()).get().await()
            }
            doc.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateAssetInFirebase(
        assetId: String,
        videoUrl: String,
        imageUrl: String,
        membershipLevel: String,
        rowName: String,
        tags: String
    ) {
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val queryById = db.collection("itv_assets").whereEqualTo("asset_id", assetId).get().await()
            val docRef = if (!queryById.isEmpty) {
                queryById.documents.first().reference
            } else {
                db.collection("itv_assets").document(assetId)
            }

            val updateMap = mutableMapOf<String, Any>(
                "videoUrl" to videoUrl,
                "imageUrl" to imageUrl,
                "membership_level" to listOf(membershipLevel),
                "row_name" to rowName,
                "tags" to tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            )
            
            docRef.set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
            clearCache()
        } catch (e: Exception) {
            e.printStackTrace()
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
