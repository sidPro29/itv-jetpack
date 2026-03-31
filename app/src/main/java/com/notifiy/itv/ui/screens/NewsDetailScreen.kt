package com.notifiy.itv.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.notifiy.itv.data.model.NewsArticle
import com.notifiy.itv.ui.viewmodel.NewsDetailViewModel
import com.notifiy.itv.ui.viewmodel.NewsViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.focusable

private const val DETAIL_TAG = "siddharthaLogs"

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    detailViewModel: NewsDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    newsViewModel: NewsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onArticleClick: (Int) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val detailState by detailViewModel.uiState.collectAsState()
    val newsState by newsViewModel.uiState.collectAsState()

    // Scroll state for the article content column
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Log.d(DETAIL_TAG, "NewsDetailScreen: Composing — isLoading=${detailState.isLoading}, id=${detailState.article?.id}")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0F))
    ) {

        // ─── LEFT: Article content ─────────────────────────────────────
        // Use Box + Column + verticalScroll instead of LazyColumn.
        // LazyColumn relies on focus movement for scroll; since most article
        // content (text, image) is not focusable, D-pad DOWN never scrolled.
        // onPreviewKeyEvent explicitly scrolls on D-pad DOWN/UP.
        Box(
            modifier = Modifier
                .weight(0.67f)
                .fillMaxHeight()
                .focusGroup()  // keeps D-pad focus inside this panel
                .onPreviewKeyEvent { ke ->
                    // Instead of full consume (true), we return false to allow the focus system
                    // to find focusable targets (like Prev/Next buttons).
                    when {
                        ke.key == Key.DirectionDown && ke.type == KeyEventType.KeyDown -> {
                            Log.d(DETAIL_TAG, "NewsDetailScreen: D-pad DOWN — scrollBy 260f")
                            scope.launch { scrollState.animateScrollBy(260f) }
                            false // Don't consume — allow focus to search for items below
                        }
                        ke.key == Key.DirectionUp && ke.type == KeyEventType.KeyDown -> {
                            if (scrollState.value > 0) {
                                scope.launch { scrollState.animateScrollBy(-260f) }
                                false
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
        ) {
            when {
                // ── Loading ───────────────────────────────────────────
                detailState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color(0xFF6C63FF),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Loading article...", color = Color(0xFF888888))
                        }
                    }
                }

                // ── Error ─────────────────────────────────────────────
                detailState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Failed to load article",
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                onClick = {
                                    Log.d(DETAIL_TAG, "NewsDetailScreen: Retry clicked")
                                    detailViewModel.loadArticle()
                                },
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

                // ── Article Content ───────────────────────────────────
                else -> {
                    val article = detailState.article

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)  // regular scroll — works without focus
                            .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                    ) {

                        // ── Back button (focusable) ───────────────────
                        Surface(
                            onClick = {
                                Log.d(DETAIL_TAG, "NewsDetailScreen: Back clicked")
                                onBackClick()
                            },
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color(0xFF1A1A2E),
                                contentColor = Color(0xFF6C63FF),
                                focusedContentColor = Color.White
                            ),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp)),
                            modifier = Modifier
                                .padding(bottom = 14.dp)
                                .focusProperties { right = FocusRequester.Cancel }  // Stay in left panel
                        ) {
                            Text(
                                "← Back to News",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp
                            )
                        }

                        if (article != null) {

                            // ── Meta row: Author | Date | NEWS ────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                Text(
                                    "👤 INTERPLANETARYTV",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp
                                )
                                Text("  |  ", color = Color(0xFF444444), fontSize = 11.sp)
                                Text(
                                    "📅 ${article.getFormattedDate()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp
                                )
                                Text("  |  ", color = Color(0xFF444444), fontSize = 11.sp)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1C1C3A), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "NEWS",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF6C63FF),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // ── Title ─────────────────────────────────
                            Text(
                                text = article.getCleanTitle(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    lineHeight = 32.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // ── Hero image ────────────────────────────
                            val thumb = article.getThumbnailUrl()
                            if (thumb.isNotEmpty()) {
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.height(24.dp))
                            }

                            val content = article.getCleanContent()
                            if (content.isNotEmpty()) {
                                // Make content focusable so focus system can "stop" here while scrolling down
                                // This provides a better bridge between top back button and bottom nav buttons
                                var isTextFocused by remember { mutableStateOf(false) }
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 24.sp,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isTextFocused) Color.White else Color(0xFFCCCCCC),
                                    modifier = Modifier
                                        .padding(bottom = 20.dp)
                                        .focusable()
                                        .onFocusChanged { isTextFocused = it.isFocused }
                                        .focusProperties { right = FocusRequester.Cancel }
                                )
                            }

                            // ── Tags ──────────────────────────────────
                            val tags = article.getTags()
                            if (tags.isNotEmpty()) {
                                Text(
                                    "Tags",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF888888),
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    fontSize = 11.sp
                                )
                                FlowTagRow(tags = tags)
                                Spacer(Modifier.height(32.dp))
                            }

                            // ── Post Navigation: Previous / Next ────────
                            val allArticles = if (newsState.searchQuery.isBlank()) newsState.articles else newsState.searchResults
                            val currentIndex = allArticles.indexOfFirst { it.id == article.id }
                            val prevArticle = if (currentIndex > 0) allArticles[currentIndex - 1] else null
                            val nextArticle = if (currentIndex != -1 && currentIndex < allArticles.size - 1) allArticles[currentIndex + 1] else null

                            if (prevArticle != null || nextArticle != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Previous Post
                                    if (prevArticle != null) {
                                        Surface(
                                            onClick = { onArticleClick(prevArticle.id) },
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = Color.Transparent,
                                                focusedContainerColor = Color(0xFF1A1A2E),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = prevArticle.getCleanTitle(),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "← Previous Post",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF6C63FF),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }

                                    // Next Post
                                    if (nextArticle != null) {
                                        Surface(
                                            onClick = { onArticleClick(nextArticle.id) },
                                            colors = ClickableSurfaceDefaults.colors(
                                                containerColor = Color.Transparent,
                                                focusedContainerColor = Color(0xFF1A1A2E),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = nextArticle.getCleanTitle(),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 12.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "Next Post →",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF6C63FF),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
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
        Column(
            modifier = Modifier
                .weight(0.33f)
                .fillMaxHeight()
                .focusGroup()
                .background(Color(0xFF0E0E16))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                "Search",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))

            NewsSearchBar(
                query = newsState.searchQuery,
                isSearching = newsState.isSearching,
                onQueryChange = { newsViewModel.onSearchQueryChanged(it) }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (newsState.searchQuery.isBlank()) "Recent Posts" else "Search Results",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))

            val sidebarList = if (newsState.searchQuery.isBlank())
                newsState.articles.take(8)
            else
                newsState.searchResults

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sidebarList, key = { "detail_side_${it.id}" }) { article ->
                    RecentPostLink(
                        article = article,
                        onClick = {
                            Log.d(DETAIL_TAG, "NewsDetailScreen: Sidebar link clicked id=${article.id}")
                            onArticleClick(article.id)
                        }
                    )
                }
                if (newsState.isSearching) {
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

// ─── Flowing tags row ────────────────────────────────────────────────
@Composable
fun FlowTagRow(tags: List<String>) {
    val chunked = tags.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chunked.forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1C1C2E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
