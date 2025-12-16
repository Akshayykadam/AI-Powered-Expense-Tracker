package com.expense.tracker.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.data.local.ai.DownloadProgress
import com.expense.tracker.data.local.ai.ModelDownloadManager
import com.expense.tracker.domain.repository.ITransactionRepository
import com.expense.tracker.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ITransactionRepository,
    private val modelDownloadManager: ModelDownloadManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    val downloadProgress: StateFlow<DownloadProgress> = modelDownloadManager.downloadProgress
    
    // Theme settings
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    private val _dynamicColor = MutableStateFlow(true)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()
    
    // App lock setting
    private val _appLockEnabled = MutableStateFlow(true)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            repository.getTransactionCount().collect { count ->
                _uiState.update { current ->
                    current.copy(
                        transactionCount = count,
                        isAiEnabled = modelDownloadManager.downloadProgress.value.isComplete,
                        isModelDownloaded = modelDownloadManager.downloadProgress.value.isComplete
                    )
                }
            }
        }
    }
    
    fun checkModelStatus(context: Context) {
        val isDownloaded = modelDownloadManager.isModelDownloaded(context)
        _uiState.update { 
            it.copy(
                isModelDownloaded = isDownloaded,
                isAiEnabled = isDownloaded
            )
        }
    }
    
    fun startModelDownload(context: Context) {
        viewModelScope.launch {
            modelDownloadManager.startDownload(context)
        }
    }
    
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        _uiState.update { it.copy(themeMode = mode) }
    }
    
    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        _uiState.update { it.copy(dynamicColorEnabled = enabled) }
    }
    
    fun setAppLockEnabled(enabled: Boolean) {
        _appLockEnabled.value = enabled
        _uiState.update { it.copy(appLockEnabled = enabled) }
    }
    
    fun setAiEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isAiEnabled = enabled) }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAll()
            // Force update the count to 0 immediately while Flow catches up
            _uiState.update { it.copy(transactionCount = 0) }
        }
    }
    
    /**
     * Called to signal other screens to reload data
     */
    private val _dataCleared = MutableStateFlow(false)
    val dataCleared: StateFlow<Boolean> = _dataCleared.asStateFlow()
    
    fun acknowledgeDataCleared() {
        _dataCleared.value = false
    }
}
