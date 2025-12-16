package com.expense.tracker.domain.model

/**
 * India-specific expense categories with Material icon names
 */
enum class Category(val displayName: String, val iconName: String) {
    // Daily Needs
    FOOD_DINING("Food & Dining", "restaurant"),
    GROCERY("Grocery", "shopping_cart"),
    FUEL("Fuel", "local_gas_station"),
    MEDICAL("Medical", "medical_services"),
    
    // Utilities & Bills
    UTILITIES("Utilities & Bills", "lightbulb"),
    RENT("Rent", "home"),
    
    // Lifestyle
    FASHION("Fashion & Shopping", "checkroom"),
    ENTERTAINMENT("Entertainment", "movie"),
    TRAVEL("Travel & Transport", "directions_car"),
    SUBSCRIPTIONS("Subscriptions", "subscriptions"),
    
    // Finance
    EMI_LOAN("EMI / Loan", "credit_card"),
    CREDIT_CARD("Credit Card", "credit_card"),
    INSURANCE("Insurance", "shield"),
    INVESTMENTS("Investments", "trending_up"),
    
    // Transfers
    UPI_TRANSFER("UPI Transfer", "smartphone"),
    BANK_TRANSFER("Bank Transfer", "account_balance"),
    
    // Income
    INCOME("Income", "payments"),
    
    // Other
    EDUCATION("Education", "school"),
    OTHER("Other", "category");
    
    companion object {
        fun fromString(value: String): Category {
            return entries.find { 
                it.name.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true)
            } ?: OTHER
        }
    }
}
