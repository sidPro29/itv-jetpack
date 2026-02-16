package com.notifiy.itv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.ui.components.MovieCard
import com.notifiy.itv.ui.theme.Background
import com.notifiy.itv.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onMovieClick: (Post) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for loading indicator
            Text("Loading...", color = Color.White)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Background),
            contentPadding = PaddingValues(start = 35.dp, top = 20.dp, bottom = 30.dp),
            // Check margin
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (state.videos.isNotEmpty()) {
                item {
                    Section(title = "Featured Videos", items = state.videos, onClick = onMovieClick)
                }
            }
            if (state.movies.isNotEmpty()) {
                item {
                    Section(title = "Movies", items = state.movies, onClick = onMovieClick)
                }
            }
            if (state.tvShows.isNotEmpty()) {
                item {
                    Section(title = "TV Shows", items = state.tvShows, onClick = onMovieClick)
                }
            }
        }
    }
}


@Composable
fun Section(
    title: String,
    items: List<Post>,
    onClick: (Post) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 58.dp)
        ) {
            items(items) { post ->
                MovieCard(post = post, onClick = { onClick(post) })
            }
        }
    }
}
