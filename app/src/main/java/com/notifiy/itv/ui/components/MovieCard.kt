package com.notifiy.itv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.notifiy.itv.data.model.Post

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageUrl = post.portraitImage?.medium?.takeIf { it.isNotEmpty() }
        ?: post.portraitPoster?.takeIf { it.isNotEmpty() }
        ?: post._embedded?.featuredMedia?.firstOrNull()?.sourceUrl
        ?: ""

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1A1A1A),
            focusedContainerColor = Color(0xFF1A1A1A),
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.LightGray))
        ),
        modifier = modifier
            .width(150.dp) // Adjusted for TV
            .aspectRatio(2f/3f)
            .padding(8.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = post.title.rendered,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            Text(
                text = post.title.rendered,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                ),
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            )
        }
    }
}
