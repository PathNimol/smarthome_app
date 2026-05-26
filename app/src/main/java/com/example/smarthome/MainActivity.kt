package com.example.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smarthome.ui.components.NavItem
import com.example.smarthome.ui.components.SmartBottomBar
import com.example.smarthome.ui.dashboard.DashboardScreen
import com.example.smarthome.ui.devices.DeviceControlScreen
import com.example.smarthome.ui.login.LoginScreen
import com.example.smarthome.ui.notifications.NotificationScreen
import com.example.smarthome.ui.theme.SmartHomeTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

object Routes {
    const val LOGIN     = "login"
    const val DASHBOARD = "dashboard"
    const val DEVICES   = "devices"
    const val ALERTS    = "alerts"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeTheme {
                SmartHomeApp(isLoggedIn = auth.currentUser != null)
            }
        }
    }
}

@Composable
fun SmartHomeApp(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val bottomRoutes = listOf(Routes.DASHBOARD, Routes.DEVICES, Routes.ALERTS)
    val showBottomBar = currentRoute in bottomRoutes

    val navItems = listOf(
        NavItem(route = Routes.DASHBOARD, label = "Home",    icon = Icons.Default.Home),
        NavItem(route = Routes.DEVICES,   label = "Devices", icon = Icons.Default.Devices),
        NavItem(route = Routes.ALERTS,    label = "Alerts",  icon = Icons.Default.Notifications)
    )

    // ── Shared tab navigation function ───────────────────────────────────────
    // Used by BOTH bottom bar AND any in-page button (bell, "see all", device card)
    // Ensures flat back stack — no tab stacks on top of another tab
    fun goToTab(route: String) {
        if (currentRoute == route) return  // already here, do nothing
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SmartBottomBar(
                    currentRoute = currentRoute,
                    items = navItems,
                    onNavigate = { route -> goToTab(route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToDevices       = { goToTab(Routes.DEVICES) },
                    onNavigateToNotifications = { goToTab(Routes.ALERTS) }
                )
            }
            composable(Routes.DEVICES) {
                DeviceControlScreen()
            }
            composable(Routes.ALERTS) {
                NotificationScreen()
            }
        }
    }
}