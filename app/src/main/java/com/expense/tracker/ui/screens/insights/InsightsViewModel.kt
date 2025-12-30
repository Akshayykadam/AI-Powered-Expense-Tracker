package com.expense.tracker.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.data.local.ai.LocalAIService
import com.expense.tracker.domain.repository.ITransactionRepository
import com.expense.tracker.ui.components.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: ITransactionRepository,
    private val localAIService: LocalAIService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    
    init {
        _uiState.update { it.copy(isAiConfigured = localAIService.isReady()) }
        loadData(Period.WEEK)
    }
    
    fun onPeriodSelected(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }
        loadData(period)
    }
    
    fun generateInsight() {
        viewModelScope.launch {
            if (!localAIService.isReady()) {
                _uiState.update { 
                    it.copy(
                        observation = "AI model not loaded. Download from Settings.",
                        isGeneratingInsight = false
                    )
                }
                return@launch
            }
            
            _uiState.update { it.copy(isGeneratingInsight = true) }
            
            val result = localAIService.generateInsight()
            
            _uiState.update { current ->
                current.copy(
                    isGeneratingInsight = false,
                    observation = result ?: "AI insight not available yet. Feature coming soon.",
                    comparison = null,
                    suggestion = null
                )
            }
        }
    }
    
    private fun loadData(period: Period) {
        viewModelScope.launch {
            val (startTime, endTime) = getPeriodRange(period)
            
            repository.getTransactionsBetween(startTime, endTime).collect { transactions ->
                // 1. Separate Debit & Credit
                val debits = transactions.filter { it.isDebit }
                val credits = transactions.filter { it.isCredit }
                
                // 2. Total Spending & Income
                val totalSpending = debits.sumOf { it.amount }
                val totalIncome = credits.sumOf { it.amount }
                
                // 3. Category Totals
                val categoryTotals = debits.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                
                // 4. Merchant Spending (Top 5)
                val merchantSpending = debits
                    .groupBy { txn -> 
                        // Improved extraction: Use merchant if available, else derive from description/body
                        val candidate = txn.merchant ?: extractMerchantName(txn.description ?: "", txn.fullBody ?: "")
                        candidate
                    }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)
                
                // 5. Income Sources (Top 5)
                val merchantIncome = credits
                    .groupBy { txn -> 
                         txn.merchant ?: extractMerchantName(txn.description ?: "", txn.fullBody ?: "")
                    }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)
                    
                // 6. Payment Mode Breakdown (Heuristic based)
                val paymentModeTotals = debits.groupBy { txn ->
                    val text = (txn.fullBody ?: txn.description ?: "").uppercase()
                    when {
                        text.contains("UPI") || text.contains("VPA") -> "UPI"
                        text.contains("XY") || text.contains("ENDING") || text.contains("CARD") -> "Card" // "ending XX" usually means card
                        text.contains("ATM") -> "Cash/ATM"
                        text.contains("NETBANKING") || text.contains("NEFT") || text.contains("IMPS") -> "Bank Transfer"
                        else -> "Other"
                    }
                }.mapValues { entry -> entry.value.sumOf { it.amount } }
                
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        categoryTotals = categoryTotals,
                        totalSpending = totalSpending,
                        totalIncome = totalIncome,
                        merchantSpending = merchantSpending,
                        merchantIncome = merchantIncome,
                        paymentModeTotals = paymentModeTotals
                    )
                }
            }
        }
    }
    
    private fun getPeriodRange(period: Period): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val endCalendar = Calendar.getInstance()
        endCalendar.add(Calendar.DAY_OF_MONTH, 1)
        endCalendar.set(Calendar.HOUR_OF_DAY, 0)
        endCalendar.set(Calendar.MINUTE, 0)
        endCalendar.set(Calendar.SECOND, 0)
        endCalendar.set(Calendar.MILLISECOND, 0)
        
        when (period) {
            Period.DAY -> { }
            Period.WEEK -> calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            Period.MONTH -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            Period.YEAR -> calendar.set(Calendar.DAY_OF_YEAR, 1)
        }
        
        return calendar.timeInMillis to endCalendar.timeInMillis
    }
    
    private fun extractMerchantName(description: String, fullBody: String): String {
        // Simple heuristic to extract a clean name
        val text = if (description.isNotBlank()) description else fullBody
        
        // Return first 20 chars capitalized if no specific logic matches
        return text.take(20).trim()
            .replace(Regex("""(?:UPI|VPA|Ref|No|Txn|Id|Date).*""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { "Unknown" }
    }
}
