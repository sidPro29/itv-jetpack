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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
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
import com.notifiy.itv.ui.theme.Background

@OptIn(ExperimentalComposeUiApi::class)
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
            .clipToBounds()
    ) {
        // Background Image (Always present as fallback/underlay)
        val imageUrl = focusedItem?.portraitPoster


        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null, // decorative
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.85f)
                    .alpha(if (isVideoPlaying) 0f else 0.6f) // Hide image when video plays
            )
        }

        // Background Video Player
        if (isVideoPlaying && focusedItem != null) {
            val videoUrl = focusedItem?.getEffectiveVideoUrl() ?: ""
            Log.i("siddharthaverma", "ImmersiveList: videoUrl = $videoUrl")
            if (videoUrl.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()
                    .fillMaxHeight(.85f)) {
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

@Composable
private fun BackgroundVideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val isYouTube = videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")
    val isWebPlayer = videoUrl.contains(".php") || videoUrl.contains("webvideocore")
    
    val videoId = if (isYouTube) {
        val trimmedUrl = videoUrl.trim()
        Regex("(?:v=|/embed/|youtu\\.be/|/v/)([^#&? ]+)").find(trimmedUrl)?.groupValues?.get(1)
    } else null

    if (videoId != null) {
        Log.d("ImmersiveList", "Playing YouTube video ID: $videoId from URL: $videoUrl")
    }

    if (isYouTube && videoId != null) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    
                    val listener = object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId, 0f)
                            youTubePlayer.unMute() 
                            youTubePlayer.setVolume(100)
                        }
                    }
                    val options = IFramePlayerOptions.Builder()
                        .controls(0) // Hide controls
                        .rel(0)
                        .origin("https://interplanetary.tv") // Match PlayerScreen
                        .ivLoadPolicy(3) // Hide video annotations
                        .ccLoadPolicy(0) // Hide captions
                        .build()
                    initialize(listener, options)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .scale(1.35f)
                .clipToBounds()
        )
    } else if (isWebPlayer) {
        // Use WebView for web-based players (like .php URLs)
        AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    val webView = this
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    
                    // Enable third party cookies, often needed for embedded players
                    android.webkit.CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(webView, true)
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        databaseEnabled = true
                        allowContentAccess = true
                        allowFileAccess = true
                        
                        // Desktop agent usually forces a more robust player
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                        
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                            val msg = message?.message() ?: ""
                            if (msg.contains("Error", ignoreCase = true)) {
                                Log.e("WebViewPlayer", "JS Error: $msg at ${message?.sourceId()}:${message?.lineNumber()}")
                            } else {
                                Log.d("WebViewPlayer", "JS Console: $msg")
                            }
                            return true
                        }
                    }
                    
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            Log.d("WebViewPlayer", "Page finished: $url - Injected Play Commands via Wrapper")
                        }

                        override fun onReceivedSslError(view: android.webkit.WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                            handler?.proceed()
                        }
                    }
                    
                    val embedHtml = """
                        <html>
                        <body style="margin:0;padding:0;background:black;">
                            <div style="position: relative; padding-bottom: 56.25%; height: 100vh; width: 100vw; overflow: hidden;">
                                <iframe src="$videoUrl" 
                                        style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" 
                                        title="Interplanetary.tv Live" 
                                        allow="autoplay; fullscreen" 
                                        allowfullscreen>
                                </iframe>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    
                    Log.d("WebViewPlayer", "Loading HTML Embed for: $videoUrl")
                    loadDataWithBaseURL("https://interplanetary.tv", embedHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .scale(1.35f)
                .clipToBounds()
        )
    } else {
        // ExoPlayer for direct streams
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                playWhenReady = true
                volume = 1f // Play sound
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
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
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
