package com.example.callsilencer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.callsilencer.ui.navigation.BottomNavigationBar
import com.example.callsilencer.ui.screens.BlockedListScreen
import com.example.callsilencer.ui.screens.HomeScreen
import com.example.callsilencer.ui.screens.ScheduleScreen
import com.example.callsilencer.ui.screens.SettingsScreen
import com.example.callsilencer.ui.screens.SpamScreen
import com.example.callsilencer.ui.viewmodel.AuthViewModel

@Composable
fun CallSilencerApp(
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(onNavigateToBlocked = { navController.navigate("blocklist") })
            }
            composable("blocklist") { BlockedListScreen() }
            composable("schedule") { ScheduleScreen() }
            composable("spam") { SpamScreen() }
            composable("settings") {
                SettingsScreen(
                    authViewModel = authViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle
                )
            }
        }
    }
}