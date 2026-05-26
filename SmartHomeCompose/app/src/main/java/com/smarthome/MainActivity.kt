// MainActivity.kt
package com.smarthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.smarthome.ui.components.NavItem
import com.smarthome.ui.components.SmartBottomBar
import com.smarthome.ui.dashboard.DashboardScreen
import com.smarthome.ui.devices.DeviceControlScreen
import com.smarthome.ui.login.LoginScreen
import com.smarthome.ui.notifications.NotificationScreen
import com.smarthome.ui.theme.SmartHomeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// ── Navigation routes ─────────────────────────────────────────────────────────

object Routes {
    const val LOGIN       = "login"
    const val DASHBOARD   = "dashboard"
    const val DEVICES     = "devices"
    const val ALERTS      = "alerts"
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

// ── Root App composable ───────────────────────────────────────────────────────

@Composable
fun SmartHomeApp(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val showBottomBar = currentRoute in listOf(Routes.DASHBOARD, Routes.DEVICES, Routes.ALERTS)

    val navItems = listOf(
        NavItem(Routes.DASHBOARD, "Home",    Icons.Default.Home),
        NavItem(Routes.DEVICES,   "Devices", Icons.Default.Devices),
        NavItem(Routes.ALERTS,    "Notification",  Icons.Default.Notifications)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SmartBottomBar(
                    currentRoute = currentRoute,
                    items = navItems,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Login ──────────────────────────────────────────────────────
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // ── Dashboard ──────────────────────────────────────────────────
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNotificationClick = { navController.navigate(Routes.ALERTS) },
                    onSeeAllNotifications = { navController.navigate(Routes.ALERTS) }
                )
            }

            // ── Device Control ─────────────────────────────────────────────
            composable(Routes.DEVICES) {
                DeviceControlScreen()
            }

            // ── Notifications ──────────────────────────────────────────────
            composable(Routes.ALERTS) {
                NotificationScreen()
            }
        }
    }
}

// ── SmartHomeApp.kt (Application class) ──────────────────────────────────────

// SmartHomeApp.kt
package com.smarthome

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartHomeApp : Application()
