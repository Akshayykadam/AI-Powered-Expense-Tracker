package com.expense.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.expense.tracker.ui.theme.*

/**
 * Animated Gradient Background - GenZ style with subtle movement
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = if (isDarkMode) {
        listOf(
            DarkBackground,
            DarkSurface,
            PurplePrimaryDark.copy(alpha = 0.15f),
            DarkBackground
        )
    } else {
        listOf(
            LightBackground,
            LightSurface,
            PurpleSecondary.copy(alpha = 0.2f),
            LightBackground
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = colors)
            ),
        content = content
    )
}

/**
 * Accent Gradient Background - More vibrant for hero sections
 */
@Composable
fun AccentGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PurplePrimary,
                        PurplePrimaryDark,
                        AccentPink.copy(alpha = 0.8f)
                    )
                )
            ),
        content = content
    )
}

/**
 * Mesh Gradient Background - Modern GenZ effect
 */
@Composable
fun MeshGradientBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PurplePrimary.copy(alpha = 0.3f),
                        AccentPink.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            ),
        content = content
    )
}
