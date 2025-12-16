package com.expense.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.tracker.ui.theme.CreditIndicator
import com.expense.tracker.ui.theme.DebitIndicator

/**
 * Card displaying a balance value (inflow, outflow, or net)
 */
@Composable
fun BalanceCard(
    title: String,
    amount: Double,
    type: BalanceType,
    modifier: Modifier = Modifier
) {
    val amountColor = when (type) {
        BalanceType.INFLOW -> CreditIndicator
        BalanceType.OUTFLOW -> DebitIndicator
        BalanceType.NET -> if (amount >= 0) CreditIndicator else DebitIndicator
    }
    
    val prefix = when (type) {
        BalanceType.INFLOW -> "+"
        BalanceType.OUTFLOW -> "-"
        BalanceType.NET -> if (amount >= 0) "+" else ""
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "$prefix₹${String.format("%,.2f", kotlin.math.abs(amount))}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

enum class BalanceType {
    INFLOW,
    OUTFLOW,
    NET
}
