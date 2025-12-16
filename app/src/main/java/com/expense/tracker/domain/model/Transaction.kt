package com.expense.tracker.domain.model

/**
 * Transaction type - Debit (money going out) or Credit (money coming in)
 */
enum class TransactionType {
    DEBIT,
    CREDIT;
    
    companion object {
        fun fromString(value: String?): TransactionType {
            return when (value?.uppercase()) {
                "CREDIT", "CR", "CREDITED" -> CREDIT
                else -> DEBIT
            }
        }
    }
}

/**
 * Expense classification for behavioral insights
 */
enum class ExpenseType {
    NEED,       // Essential expenses (utilities, rent, healthcare)
    WANT,       // Discretionary spending (entertainment, dining out)
    TRANSFER;   // Money movement (not true expense)
    
    companion object {
        fun fromString(value: String?): ExpenseType {
            return when (value?.uppercase()) {
                "NEED" -> NEED
                "WANT" -> WANT
                "TRANSFER" -> TRANSFER
                else -> WANT
            }
        }
    }
}

/**
 * Domain model for a financial transaction.
 * This is the clean domain representation used in business logic.
 */
data class Transaction(
    val id: Long = 0,
    val timestamp: Long,
    val amount: Double,
    val type: TransactionType,
    val source: String,          // Bank/Wallet name (e.g., "HDFC", "GPay")
    val merchant: String? = null,
    val category: Category = Category.OTHER,
    val expenseType: ExpenseType? = null,
    val description: String? = null,
    val rawSmsHash: String,       // For deduplication
    val fullBody: String? = null  // Full SMS text
) {
    val isDebit: Boolean get() = type == TransactionType.DEBIT
    val isCredit: Boolean get() = type == TransactionType.CREDIT
    
    /**
     * Formatted amount with currency symbol
     */
    fun formattedAmount(): String {
        return "₹${String.format("%,.2f", amount)}"
    }
}
