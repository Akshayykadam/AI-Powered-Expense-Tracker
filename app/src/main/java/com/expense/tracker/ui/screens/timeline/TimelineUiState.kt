package com.expense.tracker.ui.screens.timeline

import com.expense.tracker.domain.model.Transaction
import com.expense.tracker.ui.components.Period

/**
 * UI state for Timeline screen
 */
data class TimelineUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: Period = Period.DAY,
    val transactions: List<Transaction> = emptyList(),
    val totalInflow: Double = 0.0,
    val totalOutflow: Double = 0.0,
    val periodLabel: String = "Today"
)
