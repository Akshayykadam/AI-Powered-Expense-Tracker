package com.expense.tracker.ui.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.domain.repository.ITransactionRepository
import com.expense.tracker.ui.components.Period
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val repository: ITransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()
    
    init {
        loadData(Period.DAY)
    }
    
    fun onPeriodSelected(period: Period) {
        _uiState.update { it.copy(selectedPeriod = period, isLoading = true) }
        loadData(period)
    }
    
    private fun loadData(period: Period) {
        viewModelScope.launch {
            val (startTime, endTime) = getPeriodRange(period)
            val label = getPeriodLabel(period)
            
            combine(
                repository.getTransactionsBetween(startTime, endTime),
                repository.getTotalInflow(startTime, endTime),
                repository.getTotalOutflow(startTime, endTime)
            ) { transactions, inflow, outflow ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        transactions = transactions,
                        totalInflow = inflow,
                        totalOutflow = outflow,
                        periodLabel = label
                    )
                }
            }.collect()
        }
    }
    
    private fun getPeriodRange(period: Period): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        // Set to start of day
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
            Period.DAY -> {
                // Already set to today
            }
            Period.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            }
            Period.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
            Period.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        return calendar.timeInMillis to endCalendar.timeInMillis
    }
    
    private fun getPeriodLabel(period: Period): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        
        return when (period) {
            Period.DAY -> "Today"
            Period.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val start = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val end = dateFormat.format(calendar.time)
                "$start - $end"
            }
            Period.MONTH -> monthFormat.format(calendar.time)
            Period.YEAR -> yearFormat.format(calendar.time)
        }
    }
}
