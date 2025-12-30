package com.expense.tracker.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.data.local.ai.LocalAIService
import com.expense.tracker.domain.repository.ITransactionRepository
import com.expense.tracker.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ITransactionRepository,
    private val localAIService: LocalAIService,
    private val userPreferencesRepository: com.expense.tracker.data.repository.UserPreferencesRepository,
    private val debugLogRepository: com.expense.tracker.data.repository.DebugLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Theme settings
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    // App lock setting
    private val _appLockEnabled = MutableStateFlow(true)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()
    
    init {
        loadData()
        observePreferences()
        observeLogs()
    }
    
    private fun observeLogs() {
        viewModelScope.launch {
            debugLogRepository.logs.collect { logs ->
                _uiState.update { it.copy(debugLog = logs) }
            }
        }
    }
    
    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.isAiEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isAiEnabled = isEnabled) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getTransactionCount().collect { count ->
                _uiState.update { current ->
                    current.copy(
                        transactionCount = count,
                        isModelDownloaded = localAIService.isReady() // Reuse this field to mean "API Ready"
                    )
                }
            }
        }
    }
    
    fun checkAiStatus(context: Context) {
         viewModelScope.launch {
             localAIService.initialize(context)
             _uiState.update { it.copy(isModelDownloaded = localAIService.isReady()) }
         }
    }
    
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        _uiState.update { it.copy(themeMode = mode) }
    }
    
    fun setAiEnabled(enabled: Boolean) {
        // Clear all data when switching modes to ensure clean state
        clearAllData()
        viewModelScope.launch {
            userPreferencesRepository.setAiEnabled(enabled)
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAll()
            _uiState.update { it.copy(transactionCount = 0) }
        }
    }
}
