package com.notifiy.itv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.ui.components.ImmersiveList
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
        TvLazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Background),
            contentPadding = PaddingValues(top = 0.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            pivotOffsets = PivotOffsets(parentFraction = 0.6f, childFraction = 0f)
        ) {
            if (state.liveTv.isNotEmpty()) {
                item { 
                    ImmersiveList(
                        title = "LiveTV", 
                        items = state.liveTv, 
                        onItemClick = onMovieClick
                    ) 
                }
            }
            if (state.top10.isNotEmpty()) {
                item { Section(title = "Our Top 10", items = state.top10, onClick = onMovieClick) }
            }
            if (state.bingeVideos.isNotEmpty()) {
                item { Section(title = "Binge Videos", items = state.bingeVideos, onClick = onMovieClick) }
            }
            if (state.bingeEpicSeries.isNotEmpty()) {
                item { Section(title = "Binge- Epic series", items = state.bingeEpicSeries, onClick = onMovieClick) }
            }
            if (state.mustWatchSpaceEpic.isNotEmpty()) {
                item { Section(title = "Must-watch space epic", items = state.mustWatchSpaceEpic, onClick = onMovieClick) }
            }
            if (state.spaceToGround.isNotEmpty()) {
                item { Section(title = "space-to-ground Report", items = state.spaceToGround, onClick = onMovieClick) }
            }
            if (state.sciFiUniverse.isNotEmpty()) {
                item { Section(title = "The sci-fi universe", items = state.sciFiUniverse, onClick = onMovieClick) }
            }
            if (state.news.isNotEmpty()) {
                item { Section(title = "News", items = state.news, onClick = onMovieClick) }
            }
            if (state.talkShows.isNotEmpty()) {
                item { Section(title = "Talk show", items = state.talkShows, onClick = onMovieClick) }
            }
            if (state.documentarySeries.isNotEmpty()) {
                item { Section(title = "Doccumentry series", items = state.documentarySeries, onClick = onMovieClick) }
            }
            if (state.documentaryFilms.isNotEmpty()) {
                item { Section(title = "Documentry Film", items = state.documentaryFilms, onClick = onMovieClick) }
            }
            if (state.scienceFiction.isNotEmpty()) {
                item { Section(title = "Science Fiction", items = state.scienceFiction, onClick = onMovieClick) }
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
            modifier = Modifier.padding(start = 35.dp),
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 50.dp, start = 25.dp)
        ) {
            items(items) { post ->
                MovieCard(
                    post = post, 
                    onClick = { onClick(post) },
                    width = 250.dp,
                    aspectRatio = 16f / 9f
                )
            }
        }
    }
}
