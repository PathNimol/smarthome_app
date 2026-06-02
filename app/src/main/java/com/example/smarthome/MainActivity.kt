package com.example.smarthome

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.example.smarthome.ui.notifications.createNotificationChannel
import com.example.smarthome.ui.theme.SmartHomeTheme
import com.example.smarthome.viewmodel.NotificationViewModel
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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val isLoggedIn = auth.currentUser != null

        setContent {
            SmartHomeTheme {
                SmartHomeApp(
                    isLoggedIn    = isLoggedIn,
                    deepLinkRoute = intent.getStringExtra("navigate_to")
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun SmartHomeApp(isLoggedIn: Boolean, deepLinkRoute: String? = null) {
    val navController    = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute     = currentBackStack?.destination?.route

    // ── App-scoped NotificationViewModel ─────────────────────────────────
    // Hoisted here so it's alive on ALL screens, not just the Alerts tab.
    // This means:
    //   • Phone heads-up banners fire on every screen (ViewModel already does this).
    //   • unreadCount is available to the bottom bar from anywhere in the app.
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    val notifState by notificationViewModel.uiState.collectAsState()
    val unreadCount = notifState.unreadCount

    val bottomRoutes  = listOf(Routes.DASHBOARD, Routes.DEVICES, Routes.ALERTS)
    val showBottomBar = currentRoute in bottomRoutes

    // Alerts tab shows a numeric badge when there are unread notifications
    val navItems = listOf(
        NavItem(route = Routes.DASHBOARD, label = "Home",    icon = Icons.Default.Home),
        NavItem(route = Routes.DEVICES,   label = "Devices", icon = Icons.Default.Devices),
        NavItem(
            route      = Routes.ALERTS,
            label      = "Alerts",
            icon       = Icons.Default.Notifications,
            badgeCount = unreadCount          // ← drives the red badge on the tab
        )
    )

    LaunchedEffect(deepLinkRoute) {
        deepLinkRoute?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState    = true
            }
        }
    }

    fun goToTab(route: String) {
        if (currentRoute == route) return
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState    = true
        }
    }

    // Logout handler: sign out and go back to Login, clearing the back stack
    fun handleLogout(signOut: () -> Unit) {
        signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SmartBottomBar(
                    currentRoute = currentRoute,
                    items        = navItems,
                    onNavigate   = { route -> goToTab(route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN,
            modifier         = Modifier.padding(innerPadding)
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
                    onNavigateToNotifications = { goToTab(Routes.ALERTS)  },
                    onLogout                  = { handleLogout { /* auth.signOut() called inside DashboardScreen via AuthViewModel */ } }
                )
            }

            composable(Routes.DEVICES) {
                DeviceControlScreen()
            }

            composable(Routes.ALERTS) {
                // Pass the already-hoisted ViewModel so the Alerts screen
                // shares the same instance (same unread state, no double-fetch).
                NotificationScreen(
                    onNavigateToDashboard = { goToTab(Routes.DASHBOARD) },
                    onNavigateToDevices   = { goToTab(Routes.DEVICES)   },
                    viewModel             = notificationViewModel
                )
            }
        }
    }
}