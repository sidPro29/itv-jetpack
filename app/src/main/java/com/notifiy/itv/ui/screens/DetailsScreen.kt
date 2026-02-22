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
    imageUrl: String?,
    isVideoAvailable: Boolean = true,
    viewModel: DetailsViewModel = hiltViewModel(),
    onPlayClick: () -> Unit
) {
    val isInWatchlist by viewModel.isInWatchlist.collectAsState()

    LaunchedEffect(id) {
        viewModel.checkWatchlistStatus(id)
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
            modifier = Modifier.fillMaxSize().run {
                 // Add alpha/dim
                 this
            },
            alpha = 0.3f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(50.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            
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
                    onClick = { viewModel.toggleWatchlist(id) }
                ) {
                    Text(if (isInWatchlist) "Remove from Watchlist" else "Add to Watchlist")
                }
            }
        }
    }
}
