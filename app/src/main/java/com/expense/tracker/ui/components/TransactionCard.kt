package com.expense.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
 * GenZ-styled Transaction Card with glassmorphism
 */
@Composable
fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val typeColor = if (transaction.isDebit) DebitRed else CreditGreen
    val prefix = if (transaction.isDebit) "-" else "+"
    val paymentMethod = detectPaymentMethod(transaction.source, transaction.description)
    val categoryEmoji = getCategoryEmoji(transaction.category)
    
    val shape = RoundedCornerShape(16.dp)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            getCategoryColor(transaction.category).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = shape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji category indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(getCategoryColor(transaction.category).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryEmoji,
                        fontSize = 22.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                // Transaction details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant ?: transaction.description?.take(25) ?: transaction.category.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Tags row with modern pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModernTag(
                            text = paymentMethod.label,
                            color = paymentMethod.color
                        )
                        ModernTag(
                            text = transaction.category.displayName,
                            color = getCategoryColor(transaction.category)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formatTime(transaction.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                // Amount with type indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$prefix₹${formatAmount(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                    Text(
                        text = if (transaction.isDebit) "Spent" else "Received",
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Modern pill-shaped tag
 */
@Composable
fun ModernTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
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

// Legacy alias
@Composable
fun TransactionTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) = ModernTag(text, color, modifier)

data class PaymentMethod(val label: String, val color: Color)

private fun detectPaymentMethod(source: String, description: String?): PaymentMethod {
    val text = "${source} ${description ?: ""}".uppercase()
    
    return when {
        text.contains("UPI") || text.contains("GPAY") || text.contains("PHONEPE") || 
        text.contains("PAYTM") || text.contains("BHIM") -> 
            PaymentMethod("UPI", PurplePrimary)
        
        text.contains("CREDIT") || text.contains("CC") -> 
            PaymentMethod("Credit Card", AccentPink)
        
        text.contains("DEBIT") || text.contains("DC") || text.contains("ATM") -> 
            PaymentMethod("Debit Card", AccentCyan)
        
        text.contains("NEFT") || text.contains("RTGS") || text.contains("IMPS") -> 
            PaymentMethod("Bank Transfer", CategoryTransfers)
        
        text.contains("CASH") -> 
            PaymentMethod("Cash", CreditGreen)
        
        text.contains("EMI") -> 
            PaymentMethod("EMI", CategoryEntertainment)
        
        text.contains("WALLET") -> 
            PaymentMethod("Wallet", PurpleSecondary)
        
        else -> PaymentMethod("Bank", Color(0xFF6B7280))
    }
}

private fun getCategoryEmoji(category: Category): String = when (category) {
    Category.FOOD_DINING -> "🍔"
    Category.GROCERY -> "🛒"
    Category.FUEL -> "⛽"
    Category.MEDICAL -> "💊"
    Category.UTILITIES -> "💡"
    Category.RENT -> "🏠"
    Category.FASHION -> "👗"
    Category.ENTERTAINMENT -> "🎮"
    Category.TRAVEL -> "✈️"
    Category.SUBSCRIPTIONS -> "📺"
    Category.EMI_LOAN -> "💳"
    Category.CREDIT_CARD -> "💳"
    Category.INSURANCE -> "🛡️"
    Category.INVESTMENTS -> "📈"
    Category.UPI_TRANSFER -> "📱"
    Category.BANK_TRANSFER -> "🏦"
    Category.INCOME -> "💰"
    Category.EDUCATION -> "📚"
    Category.OTHER -> "📝"
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatAmount(amount: Double): String = when {
    amount >= 100000 -> String.format("%.1fL", amount / 100000)
    amount >= 1000 -> String.format("%.1fK", amount / 1000)
    else -> String.format("%.0f", amount)
}
