package com.livevault.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.livevault.core.ui.theme.DarkBg
import com.livevault.core.ui.theme.DarkSurface
import com.livevault.core.ui.theme.PrimaryRed
import com.livevault.core.ui.theme.TextMuted
import com.livevault.core.ui.theme.TextPrimary
import com.livevault.feature.auth.LoginScreen
import com.livevault.feature.auth.RegisterScreen
import com.livevault.feature.channel.AddChannelScreen
import com.livevault.feature.home.HomeScreen
import com.livevault.feature.library.LibraryScreen
import com.livevault.feature.notifications.NotificationsScreen
import com.livevault.feature.paywall.PaywallScreen
import com.livevault.feature.player.PlayerScreen
import com.livevault.feature.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object AddChannel : Screen("add_channel")
    object Notifications : Screen("notifications")
    object Paywall : Screen("paywall")
    object Player : Screen("player/{recordingId}") {
        fun createRoute(recordingId: String) = "player/$recordingId"
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home)
    object Library : BottomNavItem(Screen.Library.route, "Vault", Icons.Default.VideoLibrary)
    object Settings : BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
}

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Library,
        BottomNavItem.Settings
    )

    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
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
                                selectedIconColor = PrimaryRed,
                                selectedTextColor = PrimaryRed,
                                indicatorColor = PrimaryRed.copy(alpha = 0.15f),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            // Main Tabs
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToAddChannel = { navController.navigate(Screen.AddChannel.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToPlayer = { id -> navController.navigate(Screen.Player.createRoute(id)) },
                    onNavigateToChannelDetail = {}
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToPlayer = { id -> navController.navigate(Screen.Player.createRoute(id)) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToPaywall = { navController.navigate(Screen.Paywall.route) },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Add Channel
            composable(Screen.AddChannel.route) {
                AddChannelScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Notifications
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { id -> navController.navigate(Screen.Player.createRoute(id)) }
                )
            }

            // Paywall
            composable(Screen.Paywall.route) {
                PaywallScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Video Player with Deep Links
            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("recordingId") { type = NavType.StringType }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "livevault://recording/{recordingId}" },
                    navDeepLink { uriPattern = "https://livevault.app/recording/{recordingId}" }
                )
            ) {
                PlayerScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
