package com.expense.tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================
// Dark Color Scheme - Purple GenZ Theme
// ============================================
private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurplePrimaryDark,
    onPrimaryContainer = PurpleSecondary,
    secondary = AccentPink,
    onSecondary = Color.White,
    secondaryContainer = AccentPink.copy(alpha = 0.3f),
    onSecondaryContainer = AccentPink,
    tertiary = AccentCyan,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DebitRed,
    onError = Color.White,
    outline = PurplePrimary.copy(alpha = 0.5f),
    outlineVariant = DarkSurfaceVariant
)

// ============================================
// Light Color Scheme - Purple GenZ Theme
// ============================================
private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleSecondary,
    onPrimaryContainer = PurplePrimaryDark,
    secondary = AccentPink,
    onSecondary = Color.White,
    secondaryContainer = AccentPink.copy(alpha = 0.2f),
    onSecondaryContainer = AccentPink,
    tertiary = AccentCyan,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = DebitRed,
    onError = Color.White,
    outline = PurplePrimary.copy(alpha = 0.3f),
    outlineVariant = LightSurfaceVariant
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

// Gradient brushes for GenZ UI
object AppGradients {
    val primaryGradient = Brush.linearGradient(
        colors = listOf(GradientPurpleStart, GradientPurpleEnd)
    )
    
    val darkBackgroundGradient = Brush.verticalGradient(
        colors = listOf(GradientDarkStart, GradientDarkEnd)
    )
    
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            PurplePrimary.copy(alpha = 0.1f),
            AccentPink.copy(alpha = 0.05f)
        )
    )
    
    val glowGradient = Brush.radialGradient(
        colors = listOf(
            PurplePrimary.copy(alpha = 0.3f),
            Color.Transparent
        )
    )
}

@Composable
fun ExpenseTrackerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,  // Force dark mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use dark theme - app is dark mode only
    val darkTheme = true
    
    // Always use our custom purple dark theme
    val colorScheme = DarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use dark purple for status bar regardless of theme for consistent branding
            window.statusBarColor = if (darkTheme) DarkBackground.toArgb() else PurplePrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
