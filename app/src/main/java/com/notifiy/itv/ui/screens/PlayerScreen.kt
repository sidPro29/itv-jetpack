package com.notifiy.itv.ui.screens

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.net.Uri
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.Surface
import com.notifiy.itv.R
import com.notifiy.itv.data.util.VideoUrlManager
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves any kind of raw video URL to a directly playable stream URL.
 *
 * Handles:
 *  1. `/media-assets/playback/<clipId>` → hits the SVP API, returns m3u8/mpd
 *  2. `popplayer.php?it=<clipId>`       → extracts clipId, same SVP API call
 *  3. YouTube URLs                       → returned as-is (handled by YouTubePlayer)
 *  4. Direct HLS/DASH stream URLs        → returned as-is
 */
private suspend fun resolvePlayableUrl(rawUrl: String): String {
    val fixed = VideoUrlManager.fixVideoUrl(rawUrl)

    // Case 1: SVP playback API URL
    if (fixed.contains("/media-assets/playback/")) {
        return fetchSvpStreamUrl(fixed) ?: fixed
    }

    // Case 2: popplayer.php — extract the `it` param (= clipId) and resolve via SVP API
    if (fixed.contains("popplayer.php")) {
        val clipId = Uri.parse(fixed).getQueryParameter("it")
        if (!clipId.isNullOrEmpty()) {
            val playbackUrl = "https://api.interplanetary.tv/api/media-assets/playback/$clipId"
            val resolved = fetchSvpStreamUrl(playbackUrl)
            if (!resolved.isNullOrEmpty()) return resolved
        }
        // Could not resolve popplayer URL — return empty so error state is shown
        return ""
    }

    // Case 3 & 4: YouTube or direct stream — pass through as-is
    return fixed
}

/** Calls the SVP JSON endpoint and extracts the stream URL. */
private suspend fun fetchSvpStreamUrl(playbackApiUrl: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            val request = okhttp3.Request.Builder()
                .url("$playbackApiUrl?format=json")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    val map: Map<String, Any> = com.google.gson.Gson().fromJson(body, mapType)
                    map["url"] as? String
                } else null
            }
        } catch (e: Exception) {
            Log.e("PlayerScreen", "SVP API error for $playbackApiUrl: ${e.message}")
            null
        }
    }
}

@OptIn(UnstableApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoUrl: String?
) {
    if (videoUrl.isNullOrEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No video URL provided",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
        }
        return
    }

    // All state is on the main thread — we collect the resolved URL here after the suspend call
    var resolvedUrl by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(true) }

    LaunchedEffect(videoUrl) {
        // Reset every time the URL changes
        isResolving = true
        resolvedUrl = ""

        val result = resolvePlayableUrl(videoUrl)  // suspends on IO, returns on main thread

        // Both assignments happen on the main thread after suspension — no race condition
        resolvedUrl = result
        isResolving = false

        Log.d("PlayerScreen", "Raw URL     : $videoUrl")
        Log.d("PlayerScreen", "Resolved URL: $result")
        Log.d("PlayerScreen", "Is YouTube  : ${result.contains("youtube.com") || result.contains("youtu.be")}")
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val currentVideoUrl = resolvedUrl
    val isYouTube = currentVideoUrl.contains("youtube.com") || currentVideoUrl.contains("youtu.be")

    val videoId = if (isYouTube) {
        Regex("(?:v=|/embed/|youtu\\.be/|/v/)([^#&?]+)").find(currentVideoUrl)?.groupValues?.get(1)
    } else null

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("We're having trouble playing this video right now.") }
    var technicalError by remember { mutableStateOf("") }

    // Show error immediately if resolution returned empty (e.g. popplayer with no clipId)
    LaunchedEffect(isResolving, resolvedUrl) {
        if (!isResolving && resolvedUrl.isEmpty()) {
            errorMessage = "This video is not available for playback on this device."
            hasError = true
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        if (!isResolving && currentVideoUrl.isNotEmpty()) {
            if (isYouTube && videoId != null) {
                // ── YouTube fallback player ──────────────────────────────────
                AndroidView(
                    factory = { ctx ->
                        YouTubePlayerView(ctx).apply {
                            lifecycleOwner.lifecycle.addObserver(this)
                            enableAutomaticInitialization = false

                            val listener = object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.loadVideo(videoId, 0f)
                                }
                                override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                                    if (state == PlayerConstants.PlayerState.PLAYING) isLoading = false
                                }
                                override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                    val techMsg = "YouTube error: $error"
                                    Log.e("itvplaybackerror", "URL: $currentVideoUrl | $techMsg")
                                    errorMessage = "YouTube video playback failed."
                                    technicalError = techMsg
                                    hasError = true
                                    isLoading = false
                                }
                            }
                            initialize(listener, IFramePlayerOptions.Builder()
                                .controls(1).rel(0).origin("https://interplanetary.tv").build())
                        }
                    },
                    onRelease = { it.release() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // ── Native ExoPlayer for HLS / DASH streams ──────────────────
                val exoPlayer = remember(currentVideoUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        playWhenReady = true
                    }
                }

                // Lifecycle-aware pause / resume
                DisposableEffect(lifecycleOwner, exoPlayer) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                            Lifecycle.Event.ON_RESUME -> if (!hasError) exoPlayer.play()
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

                // Listener in its own DisposableEffect — never leaked
                DisposableEffect(exoPlayer) {
                    val listener = object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) isLoading = false
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            val techMsg = "ExoPlayer: ${error.message}"
                            Log.e("itvplaybackerror", "URL: $currentVideoUrl | $techMsg")
                            errorMessage = "The video format is unsupported or the stream is broken."
                            technicalError = techMsg
                            hasError = true
                            isLoading = false
                        }
                    }
                    exoPlayer.addListener(listener)
                    onDispose { exoPlayer.removeListener(listener) }
                }

                // Load media once the resolved URL is ready
                LaunchedEffect(currentVideoUrl) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(currentVideoUrl)))
                    exoPlayer.prepare()
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            useController = true
                            controllerAutoShow = true
                            controllerHideOnTouch = false
                            isFocusable = true
                            isFocusableInTouchMode = true
                            descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
                            requestFocus()
                            setOnKeyListener { _, keyCode, event ->
                                if (event.action == KeyEvent.ACTION_DOWN) {
                                    when (keyCode) {
                                        KeyEvent.KEYCODE_DPAD_CENTER,
                                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                        KeyEvent.KEYCODE_MEDIA_PLAY,
                                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                            showController()
                                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT,
                                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                                            showController()
                                            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000)
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT,
                                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                                            showController()
                                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                        }
                    },
                    onRelease = { playerView -> playerView.player = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Logo ──────────────────────────────────────────────────────────────
        AsyncImage(
            model = R.drawable.logo,
            contentDescription = "Logo",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 32.dp)
                .width(218.dp)
                .height(62.dp),
            contentScale = ContentScale.Fit
        )

        // ── Loading spinner ───────────────────────────────────────────────────
        if ((isLoading || isResolving) && !hasError) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF0066FF),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // ── Error overlay ─────────────────────────────────────────────────────
        if (hasError) {
            val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(hasError) {
                try { focusRequester.requestFocus() } catch (e: Exception) {}
            }

            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xE60A0A0A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFFF4444),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Playback Error", style = MaterialTheme.typography.displayMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFBBBBBB))

                    if (isDebuggable) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "DEV INFO:\nRaw URL: $videoUrl\nResolved: $currentVideoUrl\n$technicalError",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFFF88)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                    Surface(
                        onClick = { backDispatcher?.onBackPressed() },
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color(0xFF0066FF),
                            focusedContainerColor = Color(0xFF0088FF),
                            contentColor = Color.White
                        ),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.height(48.dp).width(160.dp).focusRequester(focusRequester)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Go Back", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
