package com.expense.tracker.ui.screens.home

import com.expense.tracker.domain.model.Transaction

/**
 * UI state for the Home screen
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val todayInflow: Double = 0.0,
    val todayOutflow: Double = 0.0,
    val netBalance: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val totalTransactionCount: Int = 0,
    val unclassifiedCount: Int = 0,
    val hasSmsPermission: Boolean = false,
    val error: String? = null,
    
    // AI Status
    val isModelDownloaded: Boolean = false,
    val aiInsight: String? = null,
    val isLoadingInsight: Boolean = false,
    
    // Debugging
    val processingMode: String = "RULES",
    val debugLog: String = ""
)
