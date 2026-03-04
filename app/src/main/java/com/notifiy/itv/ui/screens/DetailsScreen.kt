package com.notifiy.itv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.notifiy.itv.ui.viewmodel.DetailsViewModel

@Composable
fun DetailsScreen(
    id: Int,
    title: String,
    description: String = "",
    imageUrl: String?,
    isVideoAvailable: Boolean = true,
    viewModel: DetailsViewModel = hiltViewModel(),
    onPlayClick: () -> Unit
) {
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val isInPlaylist by viewModel.isInPlaylist.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(id) {
        viewModel.checkStatus(id)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.3f
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight()
                .padding(start = 50.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVideoAvailable) {
                    Button(onClick = onPlayClick) {
                        Text("Play Video")
                    }
                } else {
                    Text("No video available", color = Color.Gray)
                }

                Button(
                    onClick = { 
                        viewModel.toggleWatchlist(id)
                        val message = if (!isInWatchlist) "Added to Watchlist" else "Removed from Watchlist"
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isInWatchlist) "Watchlist ✓" else "Watchlist")
                }

                Button(
                    onClick = { 
                        viewModel.toggleLiked(id)
                        val message = if (!isLiked) "Added to Liked" else "Removed from Liked"
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isLiked) "Liked ❤️" else "Like")
                }

                Button(
                    onClick = { 
                        viewModel.togglePlaylist(id)
                        val message = if (!isInPlaylist) "Added to Playlist" else "Removed from Playlist"
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isInPlaylist) "Playlist ✓" else "Playlist")
                }
            }
        }
    }
}
