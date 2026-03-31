package com.notifiy.itv.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.notifiy.itv.data.model.NewsArticle
import com.notifiy.itv.ui.viewmodel.NewsViewModel

private const val TAG = "siddharthaLogs"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SpaceNewsScreen(
    viewModel: NewsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onArticleClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    Log.d(TAG, "SpaceNewsScreen: Composing — articles=${uiState.articles.size}")

    // Pagination: load more near end
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            last >= total - 3 && total > 0
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) viewModel.loadNextPage()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {
        // ─── LEFT PANEL: Article Cards ─────────────────────────────────
        Box(modifier = Modifier.weight(0.67f).fillMaxHeight().focusGroup()) {
            when {
                uiState.isLoading && uiState.articles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color(0xFF6C63FF),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Loading Space News...", color = Color(0xFF888888))
                        }
                    }
                }
                uiState.error != null && uiState.articles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed to load news", color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                onClick = { viewModel.refresh() },
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color(0xFF6C63FF),
                                    focusedContainerColor = Color(0xFF8B83FF),
                                    contentColor = Color.White,
                                    focusedContentColor = Color.White
                                ),
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp))
                            ) {
                                Text("Retry", modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        item {
                            Text(
                                text = "News Articles",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(uiState.articles, key = { it.id }) { article ->
                            NewsListCard(
                                article = article,
                                onClick = {
                                    Log.d(TAG, "SpaceNewsScreen: Card clicked id=${article.id}")
                                    onArticleClick(article.id)
                                }
                            )
                        }

                        // Bottom loading spinner for pagination
                        if (uiState.isLoading && uiState.articles.isNotEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = Color(0xFF6C63FF),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── DIVIDER ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(11.dp)
                .fillMaxHeight()
                .background(Color(0xFF222232))
        )

        // ─── RIGHT PANEL: Search + Recent Posts ────────────────────────
        val sidebarArticles = if (uiState.searchQuery.isBlank()) uiState.articles else uiState.searchResults

        Column(
            modifier = Modifier
                .weight(0.33f)
                .fillMaxHeight()
                .focusGroup() // keeps D-pad navigation contained within right panel
                .background(Color(0xFF0E0E16))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Search
            Text(
                "Search",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            NewsSearchBar(
                query = uiState.searchQuery,
                isSearching = uiState.isSearching,
                onQueryChange = { viewModel.onSearchQueryChanged(it) }
            )

            Spacer(Modifier.height(24.dp))

            // Recent / Search Results heading
            Text(
                text = if (uiState.searchQuery.isBlank()) "Recent Posts" else "Search Results",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(Modifier.height(10.dp))

            // Scrollable list of recent/search result links
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val displayList = sidebarArticles.take(if (uiState.searchQuery.isBlank()) 8 else 10)
                items(displayList, key = { "side_${it.id}" }) { article ->
                    RecentPostLink(
                        article = article,
                        onClick = {
                            Log.d(TAG, "SpaceNewsScreen: Sidebar link clicked id=${article.id}")
                            onArticleClick(article.id)
                        }
                    )
                }
                if (uiState.isSearching) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color(0xFF6C63FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Magazine-style Landscape Card ────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NewsListCard(article: NewsArticle, onClick: () -> Unit) {
    val thumb = article.getThumbnailUrl()
    val excerpt = article.getCleanExcerpt()

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF13131E),
            focusedContainerColor = Color(0xFF1C1C2E),
            contentColor = Color.White,
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(1.5.dp, Color(0xFF6C63FF)))
        ),
        modifier = Modifier.fillMaxWidth().height(130.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            if (thumb.isNotEmpty()) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(112.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(112.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF1A1A4E), Color(0xFF0D0D28)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛸", fontSize = 32.sp)
                }
            }

            // Text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = article.getCleanTitle(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                    if (excerpt.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = excerpt,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp
                        )
                    }
                }
                // Date + category row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📅 ${article.getFormattedDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C63FF),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "NEWS",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF888888),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─── Search Bar ────────────────────────────────────────────────────────
@Composable
fun NewsSearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(
                    color = if (isFocused) Color(0xFF1C1C2E) else Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text("Search articles...", color = Color(0xFF555555), fontSize = 12.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                cursorBrush = SolidColor(Color(0xFF6C63FF)),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }
        // Search button
        Box(
            modifier = Modifier
                .height(40.dp)
                .background(Color(0xFF6C63FF), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSearching) "..." else "Search",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Sidebar Recent Post Link ──────────────────────────────────────────
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RecentPostLink(article: NewsArticle, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0xFF1A1A2E),
            contentColor = Color(0xFFCCCCCC),
            focusedContentColor = Color.White
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "›",
                color = Color(0xFF6C63FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 6.dp, top = 1.dp)
            )
            Text(
                text = article.getCleanTitle(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCCCCCC),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
    }
}
