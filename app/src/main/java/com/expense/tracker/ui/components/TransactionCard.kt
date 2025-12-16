package com.expense.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expense.tracker.domain.model.Category
import com.expense.tracker.domain.model.Transaction
import com.expense.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card displaying a single transaction with tags
 */
@Composable
fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val typeColor = if (transaction.isDebit) DebitIndicator else CreditIndicator
    val prefix = if (transaction.isDebit) "-" else "+"
    
    // Detect payment method from source/description
    val paymentMethod = detectPaymentMethod(transaction.source, transaction.description)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(getCategoryColor(transaction.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = transaction.category.displayName,
                    tint = getCategoryColor(transaction.category),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title
                Text(
                    text = transaction.merchant ?: transaction.description?.take(30) ?: transaction.category.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Tags row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Payment method tag
                    TransactionTag(
                        text = paymentMethod.label,
                        color = paymentMethod.color
                    )
                    
                    // Category tag
                    TransactionTag(
                        text = transaction.category.displayName,
                        color = getCategoryColor(transaction.category)
                    )
                    
                    // Source tag (bank/wallet)
                    if (transaction.source.isNotBlank() && transaction.source.length <= 10) {
                        TransactionTag(
                            text = transaction.source,
                            color = Color(0xFF868E96)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Time
                Text(
                    text = formatTime(transaction.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$prefix₹${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Text(
                    text = if (transaction.isDebit) "Spent" else "Received",
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Small colored tag for transaction metadata
 */
@Composable
fun TransactionTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Payment method with color
 */
data class PaymentMethod(val label: String, val color: Color)

/**
 * Detect payment method from source and description
 */
private fun detectPaymentMethod(source: String, description: String?): PaymentMethod {
    val text = "${source} ${description ?: ""}".uppercase()
    
    return when {
        text.contains("UPI") || text.contains("GPAY") || text.contains("PHONEPE") || 
        text.contains("PAYTM") || text.contains("BHIM") -> 
            PaymentMethod("UPI", Color(0xFF5C6BC0))
        
        text.contains("CREDIT") || text.contains("CC") -> 
            PaymentMethod("Credit Card", Color(0xFFEF5350))
        
        text.contains("DEBIT") || text.contains("DC") || text.contains("ATM") -> 
            PaymentMethod("Debit Card", Color(0xFF42A5F5))
        
        text.contains("NEFT") || text.contains("RTGS") || text.contains("IMPS") -> 
            PaymentMethod("Bank Transfer", Color(0xFF26A69A))
        
        text.contains("CASH") -> 
            PaymentMethod("Cash", Color(0xFF66BB6A))
        
        text.contains("EMI") -> 
            PaymentMethod("EMI", Color(0xFFFF7043))
        
        text.contains("WALLET") -> 
            PaymentMethod("Wallet", Color(0xFFAB47BC))
        
        else -> PaymentMethod("Bank", Color(0xFF78909C))
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format("%.1fL", amount / 100000)
        amount >= 1000 -> String.format("%.1fK", amount / 1000)
        else -> String.format("%.0f", amount)
    }
}
