package com.example.smarthome.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.smarthome.R

// ── Source Sans Pro ───────────────────────────────────────────────────────────

val SourceSansPro = FontFamily(
    Font(R.font.source_sans_pro_light,    FontWeight.Light),
    Font(R.font.source_sans_pro_regular,  FontWeight.Normal),
    Font(R.font.source_sans_pro_semibold, FontWeight.SemiBold),
    Font(R.font.source_sans_pro_bold,     FontWeight.Bold),
)

// ── Typography ────────────────────────────────────────────────────────────────

val Typography = Typography(
    headlineLarge  = TextStyle(fontFamily = SourceSansPro, fontSize = 28.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = SourceSansPro, fontSize = 22.sp, fontWeight = FontWeight.Bold,     letterSpacing = (-0.3).sp),
    titleLarge     = TextStyle(fontFamily = SourceSansPro, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = TextStyle(fontFamily = SourceSansPro, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge      = TextStyle(fontFamily = SourceSansPro, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium     = TextStyle(fontFamily = SourceSansPro, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall      = TextStyle(fontFamily = SourceSansPro, fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelSmall     = TextStyle(fontFamily = SourceSansPro, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    labelMedium    = TextStyle(fontFamily = SourceSansPro, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
)

// ── Light Color Scheme (primary — matches reference) ─────────────────────────

private val LightColorScheme = lightColorScheme(
    primary             = BrandBlue,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFDDEEFF),
    onPrimaryContainer  = BrandBlueDark,
    secondary           = SuccessGreen,
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFDCF5E4),
    onSecondaryContainer= Color(0xFF1A5C2A),
    background          = PageBackground,
    onBackground        = TextPrimary,
    surface             = CardLight,
    onSurface           = TextPrimary,
    surfaceVariant      = Color(0xFFE5E5EA),
    onSurfaceVariant    = TextSecondary,
    outline             = DividerColor,
    error               = ErrorRed,
)


// ── Theme ─────────────────────────────────────────────────────────────────────

@Composable
fun SmartHomeTheme(
    content: @Composable () -> Unit
) {
    // Force light theme to match the reference design
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}