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
                        isModelDownloaded = localAIService.isReady(),
                        appVersion = com.expense.tracker.BuildConfig.VERSION_NAME
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
    
    // ==================== UPDATE CHECKER LOGIC ====================
    
    fun checkForUpdates() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isCheckingForUpdate = true, updateError = null) }
                
                val url = java.net.URL("https://api.github.com/repos/Akshayykadam/AI-Powered-Expense-Tracker/releases/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    
                    val tagName = json.getString("tag_name") // e.g., "v0.0.2"
                    val body = json.optString("body", "")
                    val assets = json.getJSONArray("assets")
                    
                    if (assets.length() > 0) {
                        val firstAsset = assets.getJSONObject(0)
                        val downloadUrl = firstAsset.getString("browser_download_url") // APK URL
                        
                        val currentVersion = com.expense.tracker.BuildConfig.VERSION_NAME
                        // Simple version comparison: if tag != current, assume update (or strictly > logic)
                        // Removing 'v' prefix if present
                        val cleanTag = tagName.removePrefix("v")
                        val cleanCurrent = currentVersion.removePrefix("v")
                        
                        if (cleanTag != cleanCurrent) {
                            _uiState.update { 
                                it.copy(
                                    updateAvailable = true,
                                    latestVersion = tagName,
                                    releaseNotes = body,
                                    downloadUrl = downloadUrl,
                                    isCheckingForUpdate = false
                                ) 
                            }
                        } else {
                            _uiState.update { it.copy(isCheckingForUpdate = false, updateAvailable = false) }
                        }
                    } else {
                         _uiState.update { it.copy(isCheckingForUpdate = false, updateError = "No APK found in release") }
                    }
                } else {
                    _uiState.update { it.copy(isCheckingForUpdate = false, updateError = "GitHub API Error: ${connection.responseCode}") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isCheckingForUpdate = false, updateError = "Check Failed: ${e.message}") }
            }
        }
    }
    
    fun downloadUpdate(context: Context) {
        val downloadUrl = uiState.value.downloadUrl
        if (downloadUrl.isEmpty()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f, updateError = null) }
                
                val request = android.app.DownloadManager.Request(android.net.Uri.parse(downloadUrl))
                    .setTitle("Downloading Update")
                    .setDescription("Downloading ${uiState.value.latestVersion}...")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "ExpenseTrackerUpdate.apk")
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val downloadId = downloadManager.enqueue(request)
                
                // Poll for progress
                var downloading = true
                while (downloading) {
                    val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                        val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        
                        if (bytesTotal > 0) {
                            val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                            _uiState.update { it.copy(downloadProgress = progress) }
                        }
                        
                        if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false
                            _uiState.update { it.copy(isDownloading = false, downloadProgress = 1f) }
                            
                            // Trigger Install
                            val uri = downloadManager.getUriForDownloadedFile(downloadId)
                            if (uri != null) {
                                installApk(context, uri)
                            } else {
                                _uiState.update { it.copy(updateError = "Download URI is null") }
                            }
                        } else if (status == android.app.DownloadManager.STATUS_FAILED) {
                             downloading = false
                             _uiState.update { it.copy(isDownloading = false, updateError = "Download Failed") }
                        }
                    }
                    cursor?.close()
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isDownloading = false, updateError = "Download Error: ${e.message}") }
            }
        }
    }
    
    private fun installApk(context: Context, uri: android.net.Uri) {
         try {
             val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
             
             // Convert to FileProvider URI if needed (required for API 24+)
             // Note: DownloadManager uri might be content:// or file://
             // If it is file://, we must convert it. If it is content:// (from DownloadManager), it might work directly 
             // BUT simpler to get the file path and define our own provider URI to be safe
             
             // For simplicity with DownloadManager, we often can use the URI directly if we grant permissions
             // However, DownloadManager's getUriForDownloadedFile returns a content:// URI that it manages.
             
             intent.setDataAndType(uri, "application/vnd.android.package-archive")
             intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
             intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
             
             context.startActivity(intent)
         } catch (e: Exception) {
             e.printStackTrace()
             _uiState.update { it.copy(updateError = "Install Failed: ${e.message}") }
         }
    }
}
