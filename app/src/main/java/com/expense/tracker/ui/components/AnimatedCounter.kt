package com.expense.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

/**
 * Animated Counter - Smoothly animates between number values
 * Perfect for balance displays
 */
@Composable
fun AnimatedCounter(
    targetValue: Double,
    modifier: Modifier = Modifier,
    prefix: String = "₹",
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = MaterialTheme.colorScheme.onBackground,
    durationMs: Int = 800
) {
    var previousValue by remember { mutableDoubleStateOf(0.0) }
    
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(
            durationMillis = durationMs,
            easing = FastOutSlowInEasing
        ),
        label = "counterAnimation"
    )
    
    LaunchedEffect(targetValue) {
        previousValue = targetValue
    }
    
    val formattedValue = remember(animatedValue) {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 0
        "$prefix${formatter.format(animatedValue.toLong())}"
    }
    
    Text(
        text = formattedValue,
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * Animated Balance Display - Large animated balance with label
 */
@Composable
fun AnimatedBalanceDisplay(
    balance: Double,
    label: String,
    modifier: Modifier = Modifier,
    balanceColor: Color = MaterialTheme.colorScheme.onBackground,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
        AnimatedCounter(
            targetValue = balance,
            style = MaterialTheme.typography.displaySmall,
            color = balanceColor
        )
    }
}

/**
 * Compact Animated Counter - For smaller displays
 */
@Composable
fun CompactAnimatedCounter(
    targetValue: Double,
    modifier: Modifier = Modifier,
    prefix: String = "₹",
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    AnimatedCounter(
        targetValue = targetValue,
        modifier = modifier,
        prefix = prefix,
        style = MaterialTheme.typography.titleLarge,
        color = color,
        durationMs = 500
    )
}

/**
 * Percentage Counter - Animates percentage values
 */
@Composable
fun AnimatedPercentage(
    targetValue: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "percentAnimation"
    )
    
    Text(
        text = "${animatedValue.toInt()}%",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}
