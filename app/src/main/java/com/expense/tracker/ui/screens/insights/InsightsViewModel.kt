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
            
            val currentTotals = _uiState.value.categoryTotals
            val periodLabel = when (_uiState.value.selectedPeriod) {
                Period.DAY -> "today"
                Period.WEEK -> "this week"
                Period.MONTH -> "this month"
                Period.YEAR -> "this year"
            }
            
            val result = localAIService.generateInsight(
                categoryTotals = currentTotals,
                periodLabel = periodLabel
            )
            
            _uiState.update { current ->
                current.copy(
                    isGeneratingInsight = false,
                    observation = result?.observation ?: "AI insight not available yet. Feature coming soon.",
                    comparison = result?.comparison,
                    suggestion = result?.suggestion
                )
            }
        }
    }
    
    private fun loadData(period: Period) {
        viewModelScope.launch {
            val (startTime, endTime) = getPeriodRange(period)
            
            repository.getCategoryTotals(startTime, endTime).collect { totals ->
                val totalSpending = totals.values.sum()
                
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        categoryTotals = totals,
                        totalSpending = totalSpending
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
}
