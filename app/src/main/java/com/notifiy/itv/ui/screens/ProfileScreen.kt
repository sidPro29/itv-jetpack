package com.notifiy.itv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.notifiy.itv.data.model.Post
import com.notifiy.itv.data.model.ItvPurchase
import com.notifiy.itv.ui.components.MovieCard
import com.notifiy.itv.ui.theme.Background
import com.notifiy.itv.ui.theme.Blue
import com.notifiy.itv.ui.viewmodel.ProfileViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutConfirm: () -> Unit,
    onMovieClick: (Post) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf("Watchlist") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Watchlist", "Playlist", "Liked", "Purchases", "Logout")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        // Profile Card
        Surface(
            onClick = { /* Account details */ },
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.005f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0066FF)))
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color(0xFF111111),
                contentColor = Color.White,
                focusedContainerColor = Color(0xFF111111)
            ),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                // Profile Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = uiState.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = uiState.userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (uiState.activePlan != null) "Current Plan : ${uiState.activePlan}" else "No active plan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF0066FF),
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    onClick = { /* Edit Profile */ },
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color(0xFF0044BB),
                        contentColor = Color.White,
                        focusedContainerColor = Color(0xFF0055DD)
                    ),
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.height(33.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Edit Profile",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Section: Tabs and Content
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar Tabs
            LazyColumn(
                modifier = Modifier
                    .width(180.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tabs) { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = {
                            if (tab == "Logout") {
                                showLogoutDialog = true
                            } else {
                                selectedTab = tab
                            }
                        },
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) Color(0xFF0033AA) else Color(0xFF151515),
                            focusedContainerColor = Color(0xFF0044BB),
                            contentColor = Color.White,
                            focusedContentColor = Color.White
                        ),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Right Content Area
            Column(modifier = Modifier.weight(1f)) {
                if (selectedTab != "Purchases") {
                    CategorizedContent(
                        items = when (selectedTab) {
                            "Playlist" -> uiState.playlist
                            "Liked" -> uiState.liked
                            "Watchlist" -> uiState.watchlist
                            else -> emptyMap()
                        },
                        tabName = selectedTab,
                        onMovieClick = onMovieClick
                    )
                } else {
                    PurchasesContent(uiState.purchases, uiState.activePlan)
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutConfirm()
                    },
                    scale = androidx.tv.material3.ButtonDefaults.scale(focusedScale = 1.05f),
                    colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = Color.DarkGray, focusedContainerColor = Color.Red)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Logout", color = Color.White)
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showLogoutDialog = false },
                    scale = androidx.tv.material3.ButtonDefaults.scale(focusedScale = 1.05f),
                    colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = Color.DarkGray, focusedContainerColor = Color.Red)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Cancel", color = Color.White)
                    }
                }
            },
            title = { Text("Logout Confirmation", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out?", color = Color.Gray) },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategorizedContent(
    items: Map<String, List<Post>>,
    tabName: String,
    onMovieClick: (Post) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Movie") }
    val categories = listOf("Movie", "Video", "Episode")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = { selectedCategory = category },
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                contentColor = if (isSelected) Color(0xFF0066FF) else Color(0x88AAAAAA),
                                focusedContainerColor = Color.DarkGray
                            ),
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(36.dp)
                                    .background(Color(0xFF0066FF), RoundedCornerShape(2.dp))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "Add to ${tabName}...",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF0066FF),
                modifier = Modifier.padding(end = 16.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Display Items
        val displayCategory = when(selectedCategory) {
            "Movie" -> "Movies"
            "Video" -> "Videos"
            "Episode" -> "TV Shows"
            else -> "Movies"
        }
        val currentItems = items[displayCategory] ?: emptyList()

        if (currentItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ${tabName.lowercase()} in $displayCategory.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Gray.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyRow (
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                items(currentItems.size) { index ->
                    MovieCard(
                        post = currentItems[index],
                        onClick = { onMovieClick(currentItems[index]) },
                        width = 170.dp,
                        aspectRatio = 0.75f
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PurchasesContent(purchases: List<ItvPurchase>, activePlan: String?) {
    Column {
        Text(
            "Transaction History",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (purchases.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You haven't made any purchases yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(purchases) { purchase ->
                    Surface(
                        onClick = {},
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color(0xFF151515),
                            contentColor = Color.White
                        ),
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF222222), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("€", color = Blue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(20.dp))
                                Column {
                                    Text(purchase.plan_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(purchase.purchase_date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${purchase.currency} ${purchase.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (purchase.plan_name == activePlan && purchase.status == "Success") {
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Text(
                                        "✓  Active",
                                        color = Color(0xFF00FF88),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
