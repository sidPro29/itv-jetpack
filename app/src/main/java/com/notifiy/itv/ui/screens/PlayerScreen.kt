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
import androidx.tv.material3.Surface
import com.notifiy.itv.R
import com.notifiy.itv.ui.theme.Surface

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

    val isYouTube = videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Extract Video ID
    // Supports: /embed/, v=, /v/, youtu.be/
    val videoId = if (isYouTube) {
        Regex("(?:v=|/embed/|youtu\\.be/|/v/)([^#&?]+)").find(videoUrl)?.groupValues?.get(1)
    } else null
    
    // Fallback logic if Youtube but ID extraction fails (should rarely happen)
    // If extraction fails, we can't use the library easily, so we might need fallback.
    // For now assuming ID is found for valid links.

    android.util.Log.d("PlayerScreen", "Original URL: $videoUrl")
    android.util.Log.d("PlayerScreen", "Extracted Video ID: $videoId")

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
//                modifier = Modifier.fillMaxSize()
//            )
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
