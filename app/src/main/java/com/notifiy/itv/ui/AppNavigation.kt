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
import com.notifiy.itv.ui.screens.NewsDetailScreen
import com.notifiy.itv.ui.screens.PlaceholderScreen
import com.notifiy.itv.ui.screens.PlayerScreen
import com.notifiy.itv.ui.screens.SearchScreen
import com.notifiy.itv.ui.screens.SpaceNewsScreen
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
    val activePlan by mainViewModel.activePlan.collectAsState()
    val refreshTrigger by mainViewModel.refreshTrigger.collectAsState()
    
    val dropdownItems = listOf("TV Shows", "Movies", "News Videos", "Videos", "Documentary Films", "Documentary Series", "Science-Fiction")
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
                        } else if (tab == "News") {
                            isDropdownOpen = false
                            navController.navigate("News") {
                                popUpTo("Home")
                                launchSingleTop = true
                            }
                        } else if (tab == "Home" || tab == "Plans & Advertise") {
                           isDropdownOpen = false
                           val targetRoute = if (tab == "Plans & Advertise") "Plans" else tab
                           navController.navigate(targetRoute) {
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
                    onProfileClick = { navController.navigate("Profile") },
                    onSubscribeClick = { navController.navigate("Plans") },
                    onAllVideosClick = { isDropdownOpen = !isDropdownOpen },
                    isDropdownOpen = isDropdownOpen,
                    isLoggedIn = isLoggedIn,
                    activePlan = activePlan
                )
            }

            val navigateToDetails: (com.notifiy.itv.data.model.Post) -> Unit = { post ->
                val encodedUrl = URLEncoder.encode(post.getDisplayImageUrl(), StandardCharsets.UTF_8.toString()).replace("+", "%20")
                val cleanTitle = post.title.rendered.replace(Regex("<[^>]*>"), "").trim()
                val encodedTitle = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8.toString()).replace("+", "%20")
                val videoUrl = post.getEffectiveVideoUrl()
                val encodedVideoUrl = if (videoUrl.isNotEmpty()) URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()).replace("+", "%20") else ""
                
                val cleanDescription = (post.description ?: "").replace(Regex("<[^>]*>"), "").trim()
                val encodedDescription = URLEncoder.encode(cleanDescription, StandardCharsets.UTF_8.toString()).replace("+", "%20")
                
                navController.navigate("Details/${post.id}/$encodedTitle/$encodedUrl?videoUrl=$encodedVideoUrl&description=$encodedDescription")
            }

            NavHost(
                navController = navController,
                startDestination = "Home",
                modifier = Modifier.weight(1f)
            ) {
                composable("Home") {
                    val homeViewModel: com.notifiy.itv.ui.viewmodel.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    
                    // Refresh data when trigger changes or screen starts
                    LaunchedEffect(refreshTrigger, Unit) {
                        homeViewModel.loadData()
                    }

                    HomeScreen(
                        viewModel = homeViewModel,
                        onMovieClick = navigateToDetails
                    )
                }
                composable("News") {
                    val newsViewModel: com.notifiy.itv.ui.viewmodel.NewsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    SpaceNewsScreen(
                        viewModel = newsViewModel,
                        onArticleClick = { articleId ->
                            navController.navigate("NewsDetail/$articleId")
                        }
                    )
                }
                composable(
                    route = "NewsDetail/{articleId}",
                    arguments = listOf(navArgument("articleId") { type = NavType.IntType })
                ) {
                    val detailViewModel: com.notifiy.itv.ui.viewmodel.NewsDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val newsViewModel: com.notifiy.itv.ui.viewmodel.NewsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    NewsDetailScreen(
                        detailViewModel = detailViewModel,
                        newsViewModel = newsViewModel,
                        onArticleClick = { articleId ->
                            navController.navigate("NewsDetail/$articleId")
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable("TV Shows") {
                    CatalogScreen(
                        title = "TV Shows",
                        type = "TV Shows",
                        onMovieClick = navigateToDetails
                    )
                }

                composable("Movies") { 
                    CatalogScreen(
                        title = "Movies", 
                        type = "Movies",
                        onMovieClick = navigateToDetails
                    ) 
                }
                composable(
                    route = "Plans?redirectTo={redirectTo}",
                    arguments = listOf(navArgument("redirectTo") { defaultValue = "Plans" })
                ) { backStackEntry ->
                    val redirectTo = backStackEntry.arguments?.getString("redirectTo") ?: "Plans"
                    if (!isLoggedIn) {
                        LaunchedEffect(Unit) {
                            navController.navigate("Login?redirectTo=$redirectTo") {
                                popUpTo("Plans") { inclusive = true }
                            }
                        }
                    } else {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        com.notifiy.itv.ui.screens.PlansScreen(
                            onPaymentError = { error ->
                                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onPaymentSuccess = {
                                mainViewModel.updateLoginStatus() // Refresh plan status
                                android.widget.Toast.makeText(context, "Payment Successful! Plan Activated.", android.widget.Toast.LENGTH_LONG).show()
                                navController.navigate("Home") {
                                    popUpTo("Plans") { inclusive = true }
                                }
                            }
                        )
                    }
                }

                
                // Dropdown items
                composable("News Videos") {
                    CatalogScreen(
                        title = "News Videos",
                        type = "News",
                        onMovieClick = navigateToDetails
                    )
                }
                composable("Videos") { 
                    CatalogScreen(
                        title = "Videos", 
                        type = "Videos",
                        onMovieClick = navigateToDetails
                    ) 
                }
                composable("Documentary Films") { 
                    CatalogScreen(
                        title = "Documentary Films", 
                        type = "Documentary Films",
                        onMovieClick = navigateToDetails
                    ) 
                }
                composable("Documentary Series") { 
                    CatalogScreen(
                        title = "Documentary Series", 
                        type = "Documentary Series",
                        onMovieClick = navigateToDetails
                    ) 
                }
                composable("Science-Fiction") { 
                    CatalogScreen(
                        title = "Science-Fiction", 
                        type = "Science-Fiction",
                        onMovieClick = navigateToDetails
                    ) 
                }

                composable("Search") { 
                    SearchScreen(
                        onMovieClick = navigateToDetails
                    )
                }
                composable(
                    route = "Login?redirectTo={redirectTo}",
                    arguments = listOf(navArgument("redirectTo") { defaultValue = "Home" })
                ) { backStackEntry ->
                    val redirectTo = backStackEntry.arguments?.getString("redirectTo") ?: "Home"
                    com.notifiy.itv.ui.screens.LoginScreen(
                        onLoginSuccess = {
                            mainViewModel.updateLoginStatus()
                            navController.navigate(redirectTo) {
                                popUpTo("Login") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSignupClick = {
                            navController.navigate("Signup?redirectTo=$redirectTo")
                        }
                    )
                }
                composable(
                    route = "Signup?redirectTo={redirectTo}",
                    arguments = listOf(navArgument("redirectTo") { defaultValue = "Home" })
                ) { backStackEntry ->
                    val redirectTo = backStackEntry.arguments?.getString("redirectTo") ?: "Home"
                    com.notifiy.itv.ui.screens.SignupScreen(
                        onSignupSuccess = {
                            mainViewModel.updateLoginStatus()
                            navController.navigate(redirectTo) {
                                popUpTo("Signup") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onLoginClick = {
                            navController.navigate("Login?redirectTo=$redirectTo")
                        }
                    )
                }


                composable("Profile") {
                    if (!isLoggedIn) {
                        LaunchedEffect(Unit) {
                            navController.navigate("Login") {
                                popUpTo("Profile") { inclusive = true }
                            }
                        }
                    } else {
                        com.notifiy.itv.ui.screens.ProfileScreen(
                            onLogoutConfirm = {
                                mainViewModel.logout()
                                navController.navigate("Home") {
                                    popUpTo("Home") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onMovieClick = navigateToDetails
                        )
                    }
                }

                // Details
                composable(
                    route = "Details/{id}/{title}/{imageUrl}?videoUrl={videoUrl}&description={description}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.IntType },
                        navArgument("title") { type = NavType.StringType },
                        navArgument("imageUrl") { type = NavType.StringType },
                        navArgument("videoUrl") { 
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("description") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("id") ?: 0
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                    val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                    val description = backStackEntry.arguments?.getString("description") ?: ""
                    
                    DetailsScreen(
                        id = id,
                        title = title,
                        description = description,
                        imageUrl = imageUrl,
                        isVideoAvailable = videoUrl.isNotEmpty(),
                        onPlayClick = { 
                            val encodedVideoUrl = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8.toString()).replace("+", "%20")
                            navController.navigate("Player?videoUrl=$encodedVideoUrl") 
                        },
                        onSubscribeClick = {
                            navController.navigate("Plans")
                        },
                        onMovieClick = navigateToDetails
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
