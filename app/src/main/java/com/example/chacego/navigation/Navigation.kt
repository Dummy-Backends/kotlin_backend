package com.example.chacego.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chacego.ui.theme.Auth.AuthScreen
import com.example.chacego.ui.theme.Auth.AuthViewModel
import com.example.chacego.ui.theme.Lobby.LobbyScreen

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Lobby : Screen("lobby")
}

/**
 * Navigation graph that manages app navigation
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    onGoogleSignInClicked: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = authViewModel,
                onGoogleSignInClicked = onGoogleSignInClicked,
                onNavigateToLobby = {
                    navController.navigate(Screen.Lobby.route) {
                        // Clear the auth screen from back stack
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Lobby.route) {
            LobbyScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        // Clear the lobby from back stack
                        popUpTo(Screen.Lobby.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

