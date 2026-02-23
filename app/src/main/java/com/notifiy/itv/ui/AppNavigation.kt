package com.notifiy.itv.ui
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.notifiy.itv.ui.components.TopBar
import com.notifiy.itv.ui.screens.CatalogScreen
import com.notifiy.itv.ui.screens.DetailsScreen
import com.notifiy.itv.ui.screens.HomeScreen
import com.notifiy.itv.ui.screens.PlaceholderScreen
import com.notifiy.itv.ui.screens.PlayerScreen
import com.notifiy.itv.ui.screens.SearchScreen
import com.notifiy.itv.ui.theme.Background
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppNavigation(
    mainViewModel: com.notifiy.itv.ui.viewmodel.MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val navController = rememberNavController()
    // var currentTab by remember { mutableStateOf("Home") } // Use currentRoute instead
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "Home"
    
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    val refreshTrigger by mainViewModel.refreshTrigger.collectAsState()
    
    val dropdownItems = listOf("News", "Videos", "Documentary Films", "Documentary Series", "Science-Fiction")
    var isDropdownOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!currentRoute.startsWith("Player") && !currentRoute.startsWith("Details")) {
                TopBar(
                    currentTab = currentRoute.substringBefore("/"), 
                    onTabSelected = { tab ->
                        if (dropdownItems.contains(tab)) {
                             isDropdownOpen = false
                             // Handle dropdown navigation logic if needed, currently placeholder
                             navController.navigate(tab) { launchSingleTop = true }
                        } else if (tab == "Home" || tab == "Movies" || tab == "TV Shows" || tab == "Plans" || tab == "Advertise") {
                           isDropdownOpen = false
                           navController.navigate(tab) {
                                popUpTo("Home")
                                launchSingleTop = true
                           }
                        } else if (tab == "All") {
                            // No-op, handled by onAllVideosClick
                        } else {
                             isDropdownOpen = false
                             navController.navigate(tab) { launchSingleTop = true }
                        }
                    },
                    onSearchClick = { navController.navigate("Search") },
                    onLoginClick = { navController.navigate("Login") },
                    onLogoutClick = { mainViewModel.logout() },
                    onSubscribeClick = { navController.navigate("Plans") },
                    onAllVideosClick = { isDropdownOpen = !isDropdownOpen },
                    isDropdownOpen = isDropdownOpen,
                    isLoggedIn = isLoggedIn
                )
            }

            NavHost(
                navController = navController,
                startDestination = "Home",
                modifier = Modifier.weight(1f)
            ) {
                composable("Home") {
                    val homeViewModel: com.notifiy.itv.ui.viewmodel.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    
                    // Refresh data when trigger changes
                    LaunchedEffect(refreshTrigger) {
                        if (refreshTrigger > 0) {
                            homeViewModel.loadData()
                        }
                    }

                    HomeScreen(
                        viewModel = homeViewModel,
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    )
                }
                composable("TV Shows") { 
                    CatalogScreen(
                        title = "TV Shows", 
                        type = "TV Shows",
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    ) 
                }
                composable("Movies") { 
                    CatalogScreen(
                        title = "Movies", 
                        type = "Movies",
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    ) 
                }
                composable("Plans") { PlaceholderScreen("Membership Plans") }
                composable("Advertise") { PlaceholderScreen("Advertise") }
                
                // Dropdown items
                // Dropdown items
                composable("News") { 
                    CatalogScreen(
                        title = "News", 
                        type = "News",
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    ) 
                }
                composable("Videos") { 
                    CatalogScreen(
                        title = "Videos", 
                        type = "Videos",
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    ) 
                }
                composable("Documentary Films") { 
                    CatalogScreen(
                        title = "Documentary Films", 
                        type = "Documentary Films",
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    ) 
                }
                composable("Documentary Series") { 
                    CatalogScreen(
                        title = "Documentary Series", 
                        type = "Documentary Series",
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    ) 
                }
                composable("Science-Fiction") { 
                    CatalogScreen(
                        title = "Science-Fiction", 
                        type = "Science-Fiction",
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    ) 
                }

                composable("Search") { 
                    SearchScreen(
                        onMovieClick = { post ->
                            val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString())
                            val encodedTitle = URLEncoder.encode(post.title.rendered, StandardCharsets.UTF_8.toString())
                            val videoUrl = post.getEffectiveVideoUrl()
                            val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()) else ""
                            
                            navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl")
                        }
                    )
                }
                composable("Login") { 
                    com.notifiy.itv.ui.screens.LoginScreen(
                        onLoginSuccess = {
                            mainViewModel.updateLoginStatus()
                            navController.navigate("Home") {
                                popUpTo("Login") { inclusive = true }
                            }
                        }
                    )
                }

                // Details
                composable(
                    route = "Details/{id}/{title}/{imageUrl}?videoUrl={videoUrl}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.IntType },
                        navArgument("title") { type = NavType.StringType },
                        navArgument("imageUrl") { type = NavType.StringType },
                        navArgument("videoUrl") { 
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: 0
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                    val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                    
                    DetailsScreen(
                        id = id,
                        title = title,
                        imageUrl = imageUrl,
                        isVideoAvailable = videoUrl.isNotEmpty(),
                        onPlayClick = { 
                            val encodedVideoUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString())
                            navController.navigate("Player?videoUrl=$encodedVideoUrl") 
                        }
                    )
                }

                composable(
                    route = "Player?videoUrl={videoUrl}",
                    arguments = listOf(
                        navArgument("videoUrl") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                    PlayerScreen(videoUrl = videoUrl)
                }
            }
        }

        // Overlay Dropdown
        if (isDropdownOpen) {
             Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 380.dp, top = 60.dp) // Estimating position based on screenshot
                    .width(220.dp)
                    .zIndex(20f)
                    .background(Color(0xFF1A1A1A), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
//                    .androidx.compose.foundation.border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    dropdownItems.forEach { item ->
                        // Simple clickable row
                        androidx.tv.material3.Surface(
                            onClick = {
                                isDropdownOpen = false
                                navController.navigate(item) { launchSingleTop = true }
                            },
                            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                                containerColor = Color.Transparent,
                                focusedContainerColor = Color(0xFF333333),
                                contentColor = Color.White,
                                focusedContentColor = Color.White
                            ),
                            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            androidx.tv.material3.Text(
                                text = item,
                                style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
