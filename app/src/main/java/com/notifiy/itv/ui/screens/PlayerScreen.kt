package com.notifiy.itv.ui.screens

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.tv.material3.Surface
import com.notifiy.itv.R
import com.notifiy.itv.data.util.VideoUrlManager
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions

@OptIn(UnstableApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoUrl: String?
) {
    if (videoUrl.isNullOrEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
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

    val currentVideoUrl = VideoUrlManager.fixVideoUrl(videoUrl) // Fix IP-based URLs before playback
    val isYouTube = currentVideoUrl.contains("youtube.com") || currentVideoUrl.contains("youtu.be")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("We're having trouble playing this video right now.") }
    var technicalError by remember { mutableStateOf("") }
    
    // Extract Video ID
    // Supports: /embed/, v=, /v/, youtu.be/
    val videoId = if (isYouTube) {
        Regex("(?:v=|/embed/|youtu\\.be/|/v/)([^#&?]+)").find(currentVideoUrl)?.groupValues?.get(1)
    } else null
    
    // Fallback logic if Youtube but ID extraction fails (should rarely happen)
    // If extraction fails, we can't use the library easily, so we might need fallback.
    // For now assuming ID is found for valid links.

    var isLoading by remember { mutableStateOf(true) }

    Log.d("PlayerScreen", "Original URL: $currentVideoUrl")
    Log.d("PlayerScreen", "Extracted Video ID: $videoId")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isYouTube && videoId != null) {
            // YouTube Player Library
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
                                if (state == PlayerConstants.PlayerState.PLAYING) {
                                    isLoading = false
                                }
                            }
                            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                val techMsg = "YouTube Player Error: $error"
                                Log.e("itvplaybackerror", "Failed URL: $currentVideoUrl | $techMsg")
                                errorMessage = "YouTube video playback failed."
                                technicalError = techMsg
                                hasError = true
                                isLoading = false
                            }
                        }
                        
                        val options = IFramePlayerOptions.Builder()
                            .controls(1)
                            .rel(0)
                            .origin("https://interplanetary.tv")
                            //.ivLoadPolicy(3)
                            //.ccLoadPolicy(1)
                            .build()
                            
            initialize(listener, options)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (currentVideoUrl.contains(".php") || currentVideoUrl.contains("webvideocore")) {
            // Web-based player (like webvideocore .php links)
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            databaseEnabled = true
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onReceivedSslError(view: android.webkit.WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                                handler?.proceed()
                            }
                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                // Webview doesn't easily know when video *actually* starts inside iframe, 
                                // but page finish is the best generic signal.
                                isLoading = false
                            }
                            override fun onReceivedError(view: android.webkit.WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                val techMsg = "WebView Error: ${error?.description}"
                                Log.e("itvplaybackerror", "Failed URL: $currentVideoUrl | $techMsg")
                                errorMessage = "Failed to load the embedded video stream."
                                technicalError = techMsg
                                hasError = true
                                isLoading = false
                            }
                        }
                        
                        val embedHtml = """
                            <html>
                            <body style="margin:0;padding:0;background:black;">
                                <div style="position: relative; padding-bottom: 56.25%; height: 100vh; width: 100vw; overflow: hidden;">
                                    <iframe src="$currentVideoUrl" 
                                            style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" 
                                            title="Interplanetary.tv Live" 
                                            allow="autoplay; fullscreen" 
                                            allowfullscreen>
                                    </iframe>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        loadDataWithBaseURL("https://interplanetary.tv", embedHtml, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Standard ExoPlayer (or fallback for non-ID youtube links)
            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                isLoading = false
                            }
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            val techMsg = "ExoPlayer Error: ${error.message}"
                            Log.e("itvplaybackerror", "Failed URL: $currentVideoUrl | $techMsg")
                            errorMessage = "The video format is unsupported or the link is broken."
                            technicalError = techMsg
                            hasError = true
                            isLoading = false
                        }
                    })
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    exoPlayer.release()
                }
            }

            LaunchedEffect(currentVideoUrl) {
                val mediaItem = MediaItem.fromUri(Uri.parse(currentVideoUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }

            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Logo - Increased size by another 20% (from 182x52 to ~218x62)
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

        // Loader
        if (isLoading && !hasError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF0066FF),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // OTT Error Overlay
        if (hasError) {
            val isDebuggable = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(hasError) {
                try {
                    focusRequester.requestFocus()
                } catch (e: Exception) {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE60A0A0A)),
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
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFBBBBBB)
                    )
                    
                    if (isDebuggable) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "DEV INFO (Will not show in prod):\nURL: $currentVideoUrl\n$technicalError",
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
