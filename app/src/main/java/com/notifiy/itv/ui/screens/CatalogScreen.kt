package com.notifiy.itv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.notifiy.itv.ui.viewmodel.CatalogViewModel

@Composable
fun CatalogScreen(
    title: String,
    type: String, // "Movies" or "TV Shows"
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
    onMovieClick: (Post) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(type) {
        viewModel.loadData(type)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(start = 58.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {


        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = Color.White)
            }
        } else if (!state.error.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${state.error}", color = Color.Red)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(5) }) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
                items(state.items) { post ->
                    MovieCard(
                        post = post,
                        onClick = { onMovieClick(post) }
                    )
                }
            }
        }
    }
}
