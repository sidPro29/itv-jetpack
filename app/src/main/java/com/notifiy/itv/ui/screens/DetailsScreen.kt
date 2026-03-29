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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.onFocusChanged
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.ui.components.MovieCard
import com.notifiy.itv.ui.viewmodel.DetailsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(
    id: Int,
    title: String,
    description: String = "",
    imageUrl: String?,
    isVideoAvailable: Boolean = true,
    viewModel: DetailsViewModel = hiltViewModel(),
    onPlayClick: () -> Unit,
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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        // Background Image Fixed
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.4f
        )

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
                          style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                          color = Color.White.copy(alpha = 0.9f),
                          maxLines = 5,
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
                                    onClick = onPlayClick,
                                    colors = ButtonDefaults.colors(
                                        containerColor = Color(0xFF0F4098),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("▶ Click to Watch", fontWeight = FontWeight.Bold)
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


