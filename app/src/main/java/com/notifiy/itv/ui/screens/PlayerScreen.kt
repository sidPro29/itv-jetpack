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
import androidx.tv.material3.Surface
import com.notifiy.itv.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions

@OptIn(UnstableApi::class)
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

    val currentVideoUrl = videoUrl // Stable reference for smart casting
    val isYouTube = currentVideoUrl.contains("youtube.com") || currentVideoUrl.contains("youtu.be")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Extract Video ID
    // Supports: /embed/, v=, /v/, youtu.be/
    val videoId = if (isYouTube) {
        Regex("(?:v=|/embed/|youtu\\.be/|/v/)([^#&?]+)").find(currentVideoUrl)?.groupValues?.get(1)
    } else null
    
    // Fallback logic if Youtube but ID extraction fails (should rarely happen)
    // If extraction fails, we can't use the library easily, so we might need fallback.
    // For now assuming ID is found for valid links.

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
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        AsyncImage(
            model = R.drawable.logo,
            contentDescription = "Logo",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 32.dp)
                .width(140.dp)
                .height(40.dp),
            contentScale = ContentScale.Fit
        )
    }
}
