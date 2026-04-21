package com.TioRico.company

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.TioRico.company.ui.screens.*
import com.TioRico.company.ui.theme.TioRicoTheme
import com.TioRico.company.viewmodel.AuthViewModel
import com.TioRico.company.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TioRicoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TioRicoApp()
                }
            }
        }
    }
}

@Composable
fun TioRicoApp(
    authViewModel: AuthViewModel = viewModel(),
    gameViewModel: GameViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (currentUser == null) "login" else "lobby"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { navController.navigate("lobby") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = { 
                    navController.navigate("lobby") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("lobby") {
            LobbyScreen(
                authViewModel = authViewModel,
                gameViewModel = gameViewModel,
                onGameCreated = { navController.navigate("game") },
                onSignOut = { 
                    val roomId = gameViewModel.room.value?.id
                    val userId = authViewModel.getCurrentUserId()
                    if (roomId != null && userId != null) {
                        gameViewModel.leaveRoom(roomId, userId)
                    }
                    navController.navigate("login") {
                        popUpTo("lobby") { inclusive = true }
                    }
                }
            )
        }
        composable("game") {
            val room by gameViewModel.room.collectAsState()
            val roomId = room?.id
            val userId = authViewModel.getCurrentUserId()
            
            GameScreen(
                authViewModel = authViewModel,
                gameViewModel = gameViewModel,
                onGameEnd = { 
                    navController.navigate("result") {
                        // Keep lobby in stack but replace game with result
                        popUpTo("game") { inclusive = true }
                    }
                },
                onExit = { 
                    if (roomId != null && userId != null) {
                        gameViewModel.leaveRoom(roomId, userId)
                    }
                    navController.navigate("lobby") {
                        popUpTo("lobby") { inclusive = true }
                    }
                }
            )
        }
        composable("result") {
            val room by gameViewModel.room.collectAsState()
            val roomId = room?.id
            val userId = authViewModel.getCurrentUserId()
            
            ResultScreen(
                gameViewModel = gameViewModel,
                onPlayAgain = {
                    if (roomId != null && userId != null) gameViewModel.leaveRoom(roomId, userId)
                    navController.navigate("lobby") {
                        popUpTo("lobby") { inclusive = true }
                    }
                },
                onExit = {
                    if (roomId != null && userId != null) gameViewModel.leaveRoom(roomId, userId)
                    navController.navigate("lobby") {
                        popUpTo("lobby") { inclusive = true }
                    }
                }
            )
        }
    }
}