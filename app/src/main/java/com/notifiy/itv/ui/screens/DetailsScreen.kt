package com.notifiy.itv.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.ui.components.MovieCard
import com.notifiy.itv.ui.components.BackgroundVideoPlayer
import com.notifiy.itv.ui.viewmodel.DetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(
    id: Int,
    title: String,
    description: String = "",
    imageUrl: String?,
    isVideoAvailable: Boolean = true,
    viewModel: DetailsViewModel = hiltViewModel(),
    onPlayClick: (String) -> Unit,
    onSubscribeClick: () -> Unit,
    onMovieClick: (Post) -> Unit
) {
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val isInPlaylist by viewModel.isInPlaylist.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val post by viewModel.post.collectAsState()
    val postTags by viewModel.postTags.collectAsState()
    val recommendedMovies by viewModel.recommendedMovies.collectAsState()
    val upcomingMovies by viewModel.upcomingMovies.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(id) {
        viewModel.loadDetails(id)
    }

    var backgroundVideoUrl by remember { mutableStateOf("") }
    var isVideoResolving by remember { mutableStateOf(false) }
    var isVideoReady by remember { mutableStateOf(false) }

    // Clear the background player URL when this screen leaves composition
    // so BackgroundVideoPlayer is destroyed and resources are freed
    DisposableEffect(Unit) {
        onDispose {
            backgroundVideoUrl = ""
            isVideoReady = false
        }
    }

    LaunchedEffect(post) {
        val currentPost = post ?: return@LaunchedEffect
        isVideoResolving = true
        isVideoReady = false
        backgroundVideoUrl = ""
        try {
            var clipId: String? = null
            var directUrl: String? = null

            val type = currentPost.type.lowercase()
            // Background: for movies show trailer, for tvshow/video show the main clip
            if (type == "movie" || type == "movies") {
                clipId = currentPost.trailer?.get("clipId") as? String
                if (clipId.isNullOrEmpty()) {
                    directUrl = currentPost.trailer?.get("ytUrl") as? String
                        ?: currentPost.trailer?.get("youtube") as? String
                }
            } else if (type == "tvshow" || type == "tvshows") {
                clipId = currentPost.videos?.get("clipId") as? String
                if (clipId.isNullOrEmpty()) {
                    directUrl = currentPost.videos?.get("ytUrl") as? String
                        ?: currentPost.videos?.get("youtube") as? String
                }
            } else if (type == "video") {
                clipId = currentPost.videos?.get("clipId") as? String
                if (clipId.isNullOrEmpty()) {
                    directUrl = currentPost.videos?.get("ytUrl") as? String
                        ?: currentPost.videos?.get("youtube") as? String
                }
            }

            val resolved: String = when {
                !clipId.isNullOrEmpty() -> {
                    val playbackUrl = "https://api.interplanetary.tv/api/media-assets/playback/$clipId"
                    withContext(Dispatchers.IO) {
                        try {
                            val client = okhttp3.OkHttpClient.Builder()
                                .followRedirects(true).followSslRedirects(true).build()
                            val request = okhttp3.Request.Builder()
                                .url("$playbackUrl?format=json").build()
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val body = response.body?.string() ?: return@withContext ""
                                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                                    val map: Map<String, Any> = com.google.gson.Gson().fromJson(body, mapType)
                                    map["url"] as? String ?: ""
                                } else ""
                            }
                        } catch (e: Exception) {
                            Log.e("DetailsScreen", "SVP API error: ${e.message}")
                            ""
                        }
                    }
                }
                !directUrl.isNullOrEmpty() -> directUrl ?: ""
                else -> ""
            }

            // Set on main thread after withContext returns — no IO-thread state mutation
            backgroundVideoUrl = resolved
        } catch (e: Exception) {
            Log.e("DetailsScreen", "Error resolving details background video: ${e.message}", e)
        } finally {
            isVideoResolving = false
        }
    }


    val imageAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVideoReady) 0f else 0.4f,
        label = "DetailsImageAlpha"
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        
        // Background Layer (Video or Image fallback)
        Box(modifier = Modifier.fillMaxSize()) {
            if (backgroundVideoUrl.isNotEmpty() && !isVideoResolving) {
                BackgroundVideoPlayer(
                    videoUrl = backgroundVideoUrl, 
                    volume = 0f,
                    onVideoReady = { isVideoReady = it }
                )
            }
            
            // Poster image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = imageAlpha
            )
            
            // Loader Spinner
            if (backgroundVideoUrl.isNotEmpty() && !isVideoReady) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color(0xFF0F4098),
                        modifier = Modifier.size(45.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 300f
                    )
                )
        )

        TvLazyColumn(
            modifier = Modifier.fillMaxSize(),
            pivotOffsets = androidx.tv.foundation.PivotOffsets(parentFraction = 0.68f, childFraction = 0f)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 150.dp, start = 50.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = postTags.takeIf { it.isNotBlank() } ?: "Category • Genre",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = description,
                        modifier = Modifier.fillMaxWidth(0.3f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🗣️ English (UK)", color = Color.White, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isVideoAvailable) {
                            if (viewModel.canWatch()) {
                                Button(
                                    onClick = {
                                        // Always use the post's main video URL (videos.clipId), not
                                        // backgroundVideoUrl which for movies holds the *trailer* URL.
                                        // PlayerScreen resolves the SVP API URL (/media-assets/playback/)
                                        // itself, so passing the raw effective URL is correct.
                                        val playUrl = post?.getEffectiveVideoUrl() ?: ""
                                        onPlayClick(playUrl)
                                    },
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF0F4098),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("▶ Click now to Watch", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onSubscribeClick,
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF0F4098),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("👑 Subscribe to Watch", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Circular buttons
                        Button(
                            onClick = { 
                                viewModel.toggleWatchlist(id)
                                val message = if (!isInWatchlist) "Added to Watchlist" else "Removed from Watchlist"
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = ButtonDefaults.shape(CircleShape),
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(top = 7.dp)
                        ) {
                            Text(if (isInWatchlist) "✓" else "+", fontSize = 20.sp)
                        }

                        Button(
                            onClick = { 
                                viewModel.toggleLiked(id)
                                val message = if (!isLiked) "Added to Liked" else "Removed from Liked"
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = ButtonDefaults.shape(CircleShape),
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(top = 7.dp)
                        ) {
                            Text(if (isLiked) "❤️" else "👍", fontSize = 18.sp)
                        }

                        Button(
                            onClick = { 
                                viewModel.togglePlaylist(id)
                                val message = if (!isInPlaylist) "Added to Playlist" else "Removed from Playlist"
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = ButtonDefaults.shape(CircleShape),
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(top = 7.dp)
                        ) {
                            Text("🔗", fontSize = 18.sp)
                        }
                    }
                }
            }

            if (recommendedMovies.isNotEmpty()) {
                item {
                    Section(
                        title = "Recommended",
                        items = recommendedMovies,
                        onClick = onMovieClick
                    )
                }
            }

            if (upcomingMovies.isNotEmpty()) {
                item {
                    Section(
                        title = "Upcoming",
                        items = upcomingMovies,
                        onClick = onMovieClick
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
