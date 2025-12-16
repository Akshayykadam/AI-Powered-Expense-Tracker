package com.expense.tracker.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.domain.model.Transaction
import com.expense.tracker.domain.model.TransactionType
import com.expense.tracker.domain.repository.ITransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class CalendarUiState(
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedDate: Int? = null,
    val dailySpending: Map<Int, Double> = emptyMap(),
    val maxDailySpending: Double = 0.0,
    val selectedDayTransactions: List<Transaction> = emptyList(),
    val selectedDaySpent: Double = 0.0,
    val selectedDayReceived: Double = 0.0,
    val allTransactions: List<Transaction> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: ITransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    init {
        loadTransactions()
    }
    
    private fun loadTransactions() {
        viewModelScope.launch {
            repository.getAllTransactions().collect { transactions ->
                _uiState.update { it.copy(allTransactions = transactions) }
                calculateDailySpending()
            }
        }
    }
    
    private fun calculateDailySpending() {
        val transactions = _uiState.value.allTransactions
        val year = _uiState.value.currentYear
        val month = _uiState.value.currentMonth
        
        val calendar = Calendar.getInstance()
        val dailyTotals = mutableMapOf<Int, Double>()
        
        transactions.forEach { txn ->
            calendar.timeInMillis = txn.timestamp
            val txnYear = calendar.get(Calendar.YEAR)
            val txnMonth = calendar.get(Calendar.MONTH)
            val txnDay = calendar.get(Calendar.DAY_OF_MONTH)
            
            if (txnYear == year && txnMonth == month && txn.type == TransactionType.DEBIT) {
                dailyTotals[txnDay] = (dailyTotals[txnDay] ?: 0.0) + txn.amount
            }
        }
        
        val maxSpending = dailyTotals.values.maxOrNull() ?: 0.0
        
        _uiState.update { 
            it.copy(
                dailySpending = dailyTotals,
                maxDailySpending = maxSpending
            )
        }
        
        // Refresh selected day if one is selected
        _uiState.value.selectedDate?.let { selectDate(it) }
    }
    
    fun selectDate(day: Int) {
        val year = _uiState.value.currentYear
        val month = _uiState.value.currentMonth
        val calendar = Calendar.getInstance()
        
        val dayTransactions = _uiState.value.allTransactions.filter { txn ->
            calendar.timeInMillis = txn.timestamp
            calendar.get(Calendar.YEAR) == year &&
            calendar.get(Calendar.MONTH) == month &&
            calendar.get(Calendar.DAY_OF_MONTH) == day
        }.sortedByDescending { it.timestamp }
        
        val spent = dayTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val received = dayTransactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        
        _uiState.update {
            it.copy(
                selectedDate = day,
                selectedDayTransactions = dayTransactions,
                selectedDaySpent = spent,
                selectedDayReceived = received
            )
        }
    }
    
    fun previousMonth() {
        val current = _uiState.value
        val newMonth: Int
        val newYear: Int
        
        if (current.currentMonth == 0) {
            newMonth = 11
            newYear = current.currentYear - 1
        } else {
            newMonth = current.currentMonth - 1
            newYear = current.currentYear
        }
        
        _uiState.update {
            it.copy(
                currentMonth = newMonth,
                currentYear = newYear,
                selectedDate = null,
                selectedDayTransactions = emptyList()
            )
        }
        calculateDailySpending()
    }
    
    fun nextMonth() {
        val current = _uiState.value
        val newMonth: Int
        val newYear: Int
        
        if (current.currentMonth == 11) {
            newMonth = 0
            newYear = current.currentYear + 1
        } else {
            newMonth = current.currentMonth + 1
            newYear = current.currentYear
        }
        
        _uiState.update {
            it.copy(
                currentMonth = newMonth,
                currentYear = newYear,
                selectedDate = null,
                selectedDayTransactions = emptyList()
            )
        }
        calculateDailySpending()
    }
}
