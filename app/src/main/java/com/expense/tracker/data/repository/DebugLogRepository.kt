package com.expense.tracker.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugLogRepository @Inject constructor() {
    private val _logs = MutableStateFlow("")
    val logs: StateFlow<String> = _logs.asStateFlow()

    fun appendLog(message: String) {
        _logs.update { current ->
            // Keep roughly 50KB or last 500 lines to avoid OOM over time
            val newLog = if (current.isNotEmpty()) "$current\n$message" else message
            if (newLog.length > 50000) {
                newLog.takeLast(45000) // Truncate old logs
            } else {
                newLog
            }
        }
    }
    
    fun clearLogs() {
        _logs.value = ""
    }
}
