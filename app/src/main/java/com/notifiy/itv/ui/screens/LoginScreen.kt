package com.notifiy.itv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.notifiy.itv.R
import com.notifiy.itv.ui.theme.Blue
import com.notifiy.itv.ui.theme.TextPrimary
import com.notifiy.itv.ui.theme.TextSecondary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val moviePosters = listOf(
        "https://image.tmdb.org/t/p/original/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
        "https://image.tmdb.org/t/p/original/qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg",
        "https://image.tmdb.org/t/p/original/vZloFAK7NmvMGKE7VkF5UHaz0I.jpg",
        "https://image.tmdb.org/t/p/original/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg",
        "https://image.tmdb.org/t/p/original/kqjL17yufvn9OVLyXYpvtyrFfak.jpg",
        "https://image.tmdb.org/t/p/original/d5NXSklXo0qyIYkgV94XAgMIckC.jpg",
        "https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
        "https://image.tmdb.org/t/p/original/kuf6dutpsT0vSVehic3EZIqkOBt.jpg",
        "https://image.tmdb.org/t/p/original/iuFNMS8U5cb6xfzi51Dbkovj7vM.jpg",
        "https://image.tmdb.org/t/p/original/jRXYjXNq0Cs2TcJjLkki24MLp7u.jpg",
        "https://image.tmdb.org/t/p/original/wWba3TaojhK7NdycRhoQpsG0FaH.jpg",
        "https://image.tmdb.org/t/p/original/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
        "https://image.tmdb.org/t/p/original/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
        "https://image.tmdb.org/t/p/original/qNBAXBIQlnOThrVvA6mA2B5ggV6.jpg",
        "https://image.tmdb.org/t/p/original/vZloFAK7NmvMGKE7VkF5UHaz0I.jpg",
        "https://image.tmdb.org/t/p/original/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg"
    )

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Poster Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) {
            items(moviePosters) { url ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(0.7f)
                        .alpha(0.3f),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Gradient & Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Login Form
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(450.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Use AsyncImage to support GIF logo
                AsyncImage(
                    model = R.drawable.logo,
                    contentDescription = "Logo",
                    modifier = Modifier.height(30.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "interplanetary.tv",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Username Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Username or Email Address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF262626))
                        .padding(horizontal = 16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (username.isEmpty()) {
                                Text(
                                    text = "Enter username",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Password Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF262626))
                        .padding(horizontal = 16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Blue),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (password.isEmpty()) {
                                Text(
                                    text = "Enter password",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            // Forgot Password
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "Forgot your password?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Blue,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Button
            Button(
                onClick = { viewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.colors(
                    containerColor = Blue,
                    contentColor = Color.White,
                    focusedContainerColor = Color(0xFF0044BB),
                    focusedContentColor = Color.White
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
                enabled = uiState !is LoginUiState.Loading
            ) {
                if (uiState is LoginUiState.Loading) {
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Error Message
            if (uiState is LoginUiState.Error) {
                Text(
                    text = (uiState as LoginUiState.Error).message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
