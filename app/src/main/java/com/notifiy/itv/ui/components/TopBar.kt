package com.notifiy.itv.ui.components

import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.notifiy.itv.R
import com.notifiy.itv.ui.theme.Blue
import com.notifiy.itv.ui.theme.Primary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    onAllVideosClick: () -> Unit,
    isDropdownOpen: Boolean,
    isLoggedIn: Boolean,
    onLogoutClick: () -> Unit = {}
) {
    val tabs = listOf("Home", "All", "TV Shows", "Movies", "Plans", "Advertise")
    val dropdownItems = listOf("News", "Videos", "Documentary Films", "Documentary Series", "Science-Fiction")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Section: Logo & Subscribe
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = R.drawable.logo,
                contentDescription = "Logo",
                modifier = Modifier
                    .width(140.dp)
                    .height(40.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(16.dp))
            
            Surface(
                onClick = onSubscribeClick,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xCC3C3200),
                    focusedContainerColor = Color(0xCC3C3200),
                    contentColor = Color(0xFFFFD700),
                    focusedContentColor = Color(0xFFFFD700)
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "Subscribe",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Center Section: Navigation Tabs
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            tabs.forEach { tabName ->
                val tab = if (tabName == "All") "All Videos" else tabName
                val isSelected = currentTab == tab || (tab == "All Videos" && dropdownItems.contains(currentTab))
                
                TabItem(
                    text = if (tab == "All Videos") "$tab ▼" else tab,
                    isSelected = isSelected,
                    onClick = {
                        if (tab == "All Videos") {
                            onAllVideosClick()
                        } else {
                            onTabSelected(tab)
                        }
                    }
                )
            }
        }

        // Right Section: Search & Login
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onSearchClick,
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    contentColor = Color.White,
                    focusedContentColor = Color.Red
                ),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text("🔍", fontSize = 20.sp, modifier = Modifier.padding(8.dp))
            }

            Surface(
                onClick = if (isLoggedIn) onLogoutClick else onLoginClick,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isLoggedIn) Color.DarkGray else Blue,
                    focusedContainerColor = if (isLoggedIn) Color.Gray else Color(0xFF00008B),
                    contentColor = Color.White
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                border = ClickableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(2.dp, Color(0xFF00008B)))
                )
            ) {
                Text(
                    if (isLoggedIn) "Logout" else "Login",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color(0x33FFFFFF),
            contentColor = if (isSelected) Color.White else Color(0xFFAAAAAA),
            focusedContentColor = Color(0xFF00008B)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = text,
            style = if (isSelected) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold) 
                    else MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
