package com.expense.tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.expense.tracker.domain.model.Category

/**
 * Maps Category to Material Icon
 */
fun getCategoryIcon(category: Category): ImageVector {
    return when (category.iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        "local_gas_station" -> Icons.Default.LocalGasStation
        "medical_services" -> Icons.Default.MedicalServices
        "lightbulb" -> Icons.Default.Lightbulb
        "home" -> Icons.Default.Home
        "checkroom" -> Icons.Default.Checkroom
        "movie" -> Icons.Default.Movie
        "directions_car" -> Icons.Default.DirectionsCar
        "subscriptions" -> Icons.Default.Subscriptions
        "credit_card" -> Icons.Default.CreditCard
        "shield" -> Icons.Default.Shield
        "trending_up" -> Icons.Default.TrendingUp
        "smartphone" -> Icons.Default.Smartphone
        "account_balance" -> Icons.Default.AccountBalance
        "payments" -> Icons.Default.Payments
        "school" -> Icons.Default.School
        "category" -> Icons.Default.Category
        else -> Icons.Default.Category
    }
}
