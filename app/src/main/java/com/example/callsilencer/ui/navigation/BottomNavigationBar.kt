package com.example.callsilencer.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.callsilencer.ui.theme.MutedDark
import com.example.callsilencer.ui.theme.NavBackground
import com.example.callsilencer.ui.theme.Primary

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentDestination = navController.currentBackStackEntry?.destination

    NavigationBar(
        containerColor = NavBackground,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            NavigationItem("home", "Home", Icons.Default.Home),
            NavigationItem("blocklist", "Block List", Icons.Default.List),
            NavigationItem("schedule", "Schedule", Icons.Default.Schedule),
            NavigationItem("spam", "Spam", Icons.Default.Warning),
            NavigationItem("settings", "Settings", Icons.Default.Settings)
        )

        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.route
            } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) Primary else MutedDark
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        color = if (selected) Primary else MutedDark
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    unselectedIconColor = MutedDark,
                    indicatorColor = Color(0xFF1A2040)
                )
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)