package com.notifiy.itv.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.notifiy.itv.data.model.Post
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.notifiy.itv.ui.theme.Background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImmersiveList(
    title: String,
    items: List<Post>,
    onItemClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val liveAsset = remember(items) {
        items.find { it.id == 587 || it.mongoId == "587" || it.title.rendered.lowercase().contains("live") } ?: items.firstOrNull()
    }

    var focusedItem by remember { mutableStateOf(items.firstOrNull()) }
    var isListFocused by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(liveAsset != null) }
    var isVideoReady by remember { mutableStateOf(false) }

    LaunchedEffect(liveAsset) {
        isVideoPlaying = liveAsset != null
        isVideoReady = false
    }

    val imageAlpha by animateFloatAsState(
        targetValue = if (isVideoReady) 0f else 0.6f,
        label = "ImmersiveImageAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .clipToBounds()
    ) {
        // Background Image (Always present as fallback/underlay)
        val imageUrl = liveAsset?.portraitPoster ?: items.firstOrNull()?.portraitPoster

        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.85f)
                    .alpha(imageAlpha)
            )
        }

        // Background Video Player (Plays the live asset continuously)
        if (isVideoPlaying && liveAsset != null) {
            val videoUrl = liveAsset.getEffectiveVideoUrl()
            if (videoUrl.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(.85f)) {
                    BackgroundVideoPlayer(
                        videoUrl = videoUrl,
                        volume = 0f,
                        onVideoReady = { isVideoReady = it }
                    )
                }
            }
        }

        // Small Buffering Spinner while loading background video
        if (isVideoPlaying && !isVideoReady) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.85f),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = com.notifiy.itv.ui.theme.Blue,
                    modifier = Modifier.size(40.dp)
                )
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
                            Background.copy(alpha = 0.5f),
                            Background
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 35.dp, bottom = 8.dp)
            )

            val displayItem = liveAsset ?: items.firstOrNull()
            displayItem?.let { item ->
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
                modifier = Modifier
                    .onFocusChanged { focusState ->
                        isListFocused = focusState.hasFocus
                    }
                    .focusProperties {
                        exit = { direction ->
                            if (direction == FocusDirection.Right || direction == FocusDirection.Next) {
                                FocusRequester.Cancel
                            } else {
                                FocusRequester.Default
                            }
                        }
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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun BackgroundVideoPlayer(
    videoUrl: String,
    volume: Float = 0f,
    onVideoReady: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var resolvedUrl by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(true) }

    LaunchedEffect(videoUrl) {
        onVideoReady(false)
        isResolving = true
        resolvedUrl = ""

        val result: String = when {
            // SVP playback API — resolve to direct HLS/DASH stream
            videoUrl.contains("/media-assets/playback/") -> {
                withContext(Dispatchers.IO) {
                    try {
                        val client = okhttp3.OkHttpClient.Builder()
                            .followRedirects(true).followSslRedirects(true).build()
                        val request = okhttp3.Request.Builder()
                            .url("$videoUrl?format=json").build()
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: return@withContext ""
                                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                                val map: Map<String, Any> = com.google.gson.Gson().fromJson(body, mapType)
                                map["url"] as? String ?: ""
                            } else ""
                        }
                    } catch (e: Exception) {
                        Log.e("BackgroundVideoPlayer", "SVP API error: ${e.message}")
                        ""
                    }
                }
            }
            // popplayer.php — extract clipId from ?it= and resolve via SVP API
            videoUrl.contains("popplayer.php") -> {
                val clipId = android.net.Uri.parse(videoUrl).getQueryParameter("it")
                if (!clipId.isNullOrEmpty()) {
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
                            Log.e("BackgroundVideoPlayer", "popplayer resolve error: ${e.message}")
                            ""
                        }
                    }
                } else ""
            }
            // Direct stream URL (HLS, DASH, YouTube) — use as-is
            else -> videoUrl
        }

        // Both state writes happen on the main thread after withContext returns
        resolvedUrl = result
        isResolving = false
    }

    if (isResolving) return

    val finalUrl = resolvedUrl
    val isYouTube = finalUrl.contains("youtube.com") || finalUrl.contains("youtu.be")

    val videoId = if (isYouTube) {
        val trimmedUrl = finalUrl.trim()
        Regex("(?:v=|/embed/|youtu\\.be/|/v/)([^#& ]+)").find(trimmedUrl)?.groupValues?.get(1)
    } else null

    if (isYouTube && videoId != null) {
        // ── YouTube background player ─────────────────────────────────────────
        // YouTubePlayerView manages its own lifecycle via LifecycleObserver.
        // We must remove the observer on dispose to prevent memory leaks.
        DisposableEffect(lifecycleOwner) {
            onDispose {
                // Observer removal is handled by YouTubePlayerView itself when
                // release() is called — but we keep this block to ensure the
                // composable lifecycle is properly tracked.
            }
        }

        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)

                    val listener = object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId, 0f)
                            if (volume == 0f) youTubePlayer.mute() else youTubePlayer.unMute()
                            onVideoReady(true)
                        }
                    }
                    val options = IFramePlayerOptions.Builder()
                        .controls(0)
                        .rel(0)
                        .origin("https://interplanetary.tv")
                        .ivLoadPolicy(3)
                        .ccLoadPolicy(0)
                        .build()
                    initialize(listener, options)
                }
            },
            update = { /* no-op: video ID doesn't change without full recomposition */ },
            onRelease = { ytView ->
                // Called by AndroidView when the composable leaves composition —
                // properly releases the YouTubePlayerView and removes lifecycle observer
                ytView.release()
            },
            modifier = Modifier
                .fillMaxSize()
                .scale(1.35f)
                .clipToBounds()
        )
    } else {
        // ── Native ExoPlayer background player ───────────────────────────────
        val exoPlayer = remember(finalUrl) {
            ExoPlayer.Builder(context).build().apply {
                this.volume = volume
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                playWhenReady = true
            }
        }

        // Pause/resume with app lifecycle to avoid background resource usage
        DisposableEffect(lifecycleOwner, exoPlayer) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                    Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            }
        }

        val playerListener = remember(exoPlayer) {
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        onVideoReady(true)
                    }
                }
            }
        }

        DisposableEffect(exoPlayer, playerListener) {
            exoPlayer.addListener(playerListener)
            onDispose {
                exoPlayer.removeListener(playerListener)
            }
        }

        LaunchedEffect(finalUrl) {
            if (finalUrl.isNotEmpty()) {
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(finalUrl)))
                exoPlayer.prepare()
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            onRelease = { playerView ->
                // Detach player from view before release to avoid "released player" warnings
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
