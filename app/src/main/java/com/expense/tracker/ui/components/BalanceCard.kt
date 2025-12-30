package com.expense.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.tracker.ui.theme.*

/**
 * GenZ-styled Balance Card with gradient and animations
 */
@Composable
fun BalanceCard(
    title: String,
    amount: Double,
    type: BalanceType,
    modifier: Modifier = Modifier
) {
    val (gradientColors, iconColor, icon) = when (type) {
        BalanceType.INFLOW -> Triple(
            listOf(CreditGreen.copy(alpha = 0.2f), CreditGreen.copy(alpha = 0.05f)),
            CreditGreen,
            Icons.Default.ArrowDownward
        )
        BalanceType.OUTFLOW -> Triple(
            listOf(DebitRed.copy(alpha = 0.2f), DebitRed.copy(alpha = 0.05f)),
            DebitRed,
            Icons.Default.ArrowUpward
        )
        BalanceType.NET -> Triple(
            listOf(PurplePrimary.copy(alpha = 0.2f), AccentPink.copy(alpha = 0.1f)),
            if (amount >= 0) CreditGreen else DebitRed,
            if (amount >= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
        )
    }
    
    val prefix = when (type) {
        BalanceType.INFLOW -> "+"
        BalanceType.OUTFLOW -> "-"
        BalanceType.NET -> if (amount >= 0) "+" else ""
    }
    
    val shape = RoundedCornerShape(20.dp)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        iconColor.copy(alpha = 0.3f),
                        iconColor.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Animated amount
            AnimatedCounter(
                targetValue = kotlin.math.abs(amount),
                prefix = "$prefix₹",
                style = MaterialTheme.typography.headlineSmall,
                color = iconColor
            )
        }
    }
}

enum class BalanceType {
    INFLOW,
    OUTFLOW,
    NET
}

/**
 * Hero Balance Card - Large display for total balance
 */
@Composable
fun HeroBalanceCard(
    balance: Double,
    label: String = "Total Balance",
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PurplePrimary,
                        PurplePrimaryDark,
                        AccentPink.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AnimatedCounter(
                targetValue = balance,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                durationMs = 1000
            )
        }
    }
}
