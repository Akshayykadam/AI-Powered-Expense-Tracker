package com.expense.tracker.ui.screens.insights

import com.expense.tracker.domain.model.Category
import com.expense.tracker.ui.components.Period

/**
 * UI state for Insights screen
 */
data class InsightsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: Period = Period.WEEK,
    val categoryTotals: Map<Category, Double> = emptyMap(),
    val merchantSpending: List<Pair<String, Double>> = emptyList(), // Top brands spent
    val merchantIncome: List<Pair<String, Double>> = emptyList(),   // Top sources received
    val paymentModeTotals: Map<String, Double> = emptyMap(),        // UPI, Card, etc.
    val totalSpending: Double = 0.0,
    val totalIncome: Double = 0.0,
    val observation: String? = null,
    val comparison: String? = null,
    val suggestion: String? = null,
    val isAiConfigured: Boolean = false,
    val isGeneratingInsight: Boolean = false,
    val error: String? = null
)
