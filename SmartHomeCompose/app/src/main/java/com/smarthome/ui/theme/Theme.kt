// ui/theme/Theme.kt
package com.smarthome.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = Teal700,
    onPrimary        = Color.White,
    primaryContainer = Teal50,
    onPrimaryContainer = Teal900,
    secondary        = Teal500,
    onSecondary      = Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal900,
    background       = WarmBackground,
    onBackground     = TextPrimary,
    surface          = CardWhite,
    onSurface        = TextPrimary,
    surfaceVariant   = Teal50,
    onSurfaceVariant = TextSecondary,
    outline          = BorderLight,
    error            = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary          = Teal200,
    onPrimary        = Teal900,
    primaryContainer = Teal800,
    onPrimaryContainer = Teal50,
    secondary        = Teal100,
    onSecondary      = Teal900,
    background       = DarkBackground,
    onBackground     = Color.White,
    surface          = DarkCard,
    onSurface        = Color.White,
    surfaceVariant   = DarkSurface,
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline          = DarkBorder,
)

val Typography = Typography(
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold,   lineHeight = 34.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold,   lineHeight = 30.sp),
    titleLarge     = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge      = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall      = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelSmall     = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun SmartHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
