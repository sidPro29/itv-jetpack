package com.notifiy.itv.ui.components

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.ui.components.MovieCard
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@Composable
fun ImmersiveList(
    title: String,
    items: List<Post>,
    onItemClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    var focusedItem by remember { mutableStateOf(items.firstOrNull()) }
    var isListFocused by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    
    // Fallback to surface color if background not available or just use Black/Dark Gray
    val gradientColor = MaterialTheme.colorScheme.surface 

    // Handle Video Playback Delay
    LaunchedEffect(focusedItem, isListFocused) {
        isVideoPlaying = false
        if (isListFocused && focusedItem != null) {
            delay(3000) // 3 seconds delay
            isVideoPlaying = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp) 
    ) {
        // Background Image (Always present as fallback/underlay)
        val imageUrl = focusedItem?.let { post ->
            post.portraitImage?.large?.takeIf { it.isNotEmpty() }
                ?: post.portraitImage?.full?.takeIf { it.isNotEmpty() }
                ?: post._embedded?.featuredMedia?.firstOrNull()?.sourceUrl
                ?: post.portraitPoster
        }

        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null, // decorative
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isVideoPlaying) 0f else 0.6f) // Hide image when video plays
            )
        }

        // Background Video Player
        if (isVideoPlaying && focusedItem != null) {
            val videoUrl = focusedItem?.videoUrl
            if (!videoUrl.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize().alpha(0.6f)) {
                     BackgroundVideoPlayer(videoUrl = videoUrl)
                }
            }
        }

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            gradientColor.copy(alpha = 0.5f),
                            gradientColor
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 35.dp, bottom = 8.dp)
            )
            
            focusedItem?.let { item ->
                Text(
                    text = item.title.rendered,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 35.dp, bottom = 12.dp)
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(end = 50.dp, start = 25.dp),
                modifier = Modifier.onFocusChanged { focusState ->
                    isListFocused = focusState.hasFocus
                }
            ) {
                items(items) { post ->
                    MovieCard(
                        post = post,
                        onClick = { onItemClick(post) },
                        modifier = Modifier.onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                focusedItem = post
                            }
                        },
                        width = 150.dp, 
                        aspectRatio = 16f / 9f
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val isYouTube = videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")
    val videoId = if (isYouTube) {
        Regex("(?:v=|/embed/|youtu\\.be/|/v/)([^#&?]+)").find(videoUrl)?.groupValues?.get(1)
    } else null

    if (isYouTube && videoId != null) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    
                    val listener = object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId, 0f)
                        }
                    }
                    val options = IFramePlayerOptions.Builder()
                        .controls(0) // Hide controls
                        .rel(0)
                        .ivLoadPolicy(3) // Hide video annotations
                        .ccLoadPolicy(0) // Hide captions
                        .build()
                    initialize(listener, options)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // ExoPlayer
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                volume = 0f // Mute background video by default? User didn't specify, but usually background relative to safe
                playWhenReady = true
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        LaunchedEffect(videoUrl) {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill background
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
