package com.expense.tracker.ui.screens.home

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.data.local.ai.LocalAIService
import com.expense.tracker.data.local.categorization.MerchantCategorizer
import com.expense.tracker.data.local.sms.ParseResult
import com.expense.tracker.data.local.sms.SmsParser
import com.expense.tracker.data.local.sms.SmsReader
import com.expense.tracker.domain.model.Transaction
import com.expense.tracker.domain.model.TransactionType
import com.expense.tracker.domain.repository.ITransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.delay
import com.expense.tracker.data.local.ai.SmsMessageRequest
import javax.inject.Inject

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ITransactionRepository,
    private val smsReader: SmsReader,
    private val smsParser: SmsParser,
    private val merchantCategorizer: MerchantCategorizer,
    private val localAIService: LocalAIService,
    private val userPreferencesRepository: com.expense.tracker.data.repository.UserPreferencesRepository,
    private val debugLogRepository: com.expense.tracker.data.repository.DebugLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private var hasInitialAuditRun = false
    
    init {
        loadData()
        observePreferences()
    }
    
    private fun observePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.isAiEnabled.collect { isEnabled ->
                val newMode = if (isEnabled) "HYBRID" else "RULES"
                if (_uiState.value.processingMode != newMode) {
                     Log.d(TAG, "Preference changed, updating mode to: $newMode")
                     _uiState.update { it.copy(processingMode = newMode) }
                     
                     // If mode changed to RULES, we might need to refresh if we were stuck
                }
            }
        }
    }
    
    fun toggleProcessingMode() {
        // Now controlled by Settings basically, but for dev toggle in debug:
        val currentIsAi = _uiState.value.processingMode == "HYBRID"
        val newIsAi = !currentIsAi
        
        viewModelScope.launch {
            userPreferencesRepository.setAiEnabled(newIsAi)
        }
    }
    
    // Flag to prevent loadData from hiding the spinner during a refresh operation
    @Volatile
    private var isRefreshing = false

    private fun loadData() {
        viewModelScope.launch {
            val (startOfDay, endOfDay) = getTodayRange()
            
            combine(
                repository.getTotalInflow(startOfDay, endOfDay),
                repository.getTotalOutflow(startOfDay, endOfDay),
                repository.getNetBalance(startOfDay, endOfDay),
                repository.getAllTransactions()
            ) { inflow, outflow, netBalance, transactions ->
                HomeUiData(inflow, outflow, netBalance, transactions)
            }.combine(
                combine(
                    repository.getTransactionCount(),
                    repository.getUnclassifiedCount()
                ) { count, unclassified -> count to unclassified }
            ) { data, counts ->
                _uiState.update { current ->
                    current.copy(
                        // Only hide loading if we are NOT currently refreshing
                        isLoading = if (isRefreshing) true else false,
                        todayInflow = data.inflow,
                        todayOutflow = data.outflow,
                        netBalance = data.netBalance,
                        recentTransactions = data.transactions.take(20),
                        allTransactions = data.transactions,
                        totalTransactionCount = counts.first,
                        unclassifiedCount = counts.second
                    )
                }
            }.collect()
        }
    }
    
    private data class HomeUiData(
        val inflow: Double,
        val outflow: Double,
        val netBalance: Double,
        val transactions: List<Transaction>
    )
    
    /**
     * Check if model is already downloaded
     */
    override fun onCleared() {
        super.onCleared()
        localAIService.release()
    }

    /**
     * Check if model is already downloaded
     */
    fun checkModelStatus(context: Context) {
        viewModelScope.launch {
            try {
                val isReady = localAIService.initialize(context)
                _uiState.update { it.copy(isModelDownloaded = isReady) } // Reuse flag for API Ready

                if (isReady && _uiState.value.processingMode == "HYBRID" && _uiState.value.hasSmsPermission) {
                     // Only run automatically ONCE per session
                     if (!hasInitialAuditRun) {
                         debugLogRepository.appendLog("Gemini AI Ready. Starting initial SMS processing...")
                         refreshSms(context)
                         hasInitialAuditRun = true
                     }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkModelStatus", e)
            }
        }
    }
    
    // Removed startModelDownload
    
    // Old toggleProcessingMode removed in favor of preference-based toggle
    
    /**
     * Clear existing data and reprocess SMS with AI
     */
    private suspend fun reprocessWithAI(context: Context) {
        // Ensure AI is initialized before reprocessing
        if (localAIService.initialize(context)) {
             Log.d(TAG, "AI model ready - reprocessing SMS with strict AI verification")
             onSmsPermissionGranted(context.contentResolver)
        } else {
             Log.e(TAG, "Failed to initialize AI for reprocessing")
        }
    }

    // ... refreshSms ...
    fun refreshSms(context: Context) {
        viewModelScope.launch {
            Log.d(TAG, "=== REFRESH STARTED === Mode=${_uiState.value.processingMode}")
            
            // Only initialize AI if we are in HYBRID mode
            if (_uiState.value.processingMode != "RULES") {
                 Log.d(TAG, "HYBRID Mode: Checking AI status...")
                 if (!localAIService.isReady()) {
                    Log.d(TAG, "AI not ready, attempting to initialize...")
                     val initialized = localAIService.initialize(context)
                     if (!initialized) {
                         val errorMsg = "Failed to initialize Gemini AI. Check API Key."
                         Log.e(TAG, errorMsg)
                         _uiState.update { it.copy(error = errorMsg) }
                         return@launch
                     }
                     _uiState.update { it.copy(isModelDownloaded = true) } // Reuse flag
                }
            } else {
                 Log.d(TAG, "RULES Mode: Skipping AI initialization")
            }
            
            Log.d(TAG, "Starting SMS processing...")
            processSmsWithAI(context.contentResolver)
        }
    }
    
    /**
     * Internal function that processes SMS - handles both RULES and HYBRID modes
     */
    private suspend fun processSmsWithAI(contentResolver: ContentResolver) {
        withContext(Dispatchers.IO) {
            try {
            isRefreshing = true // Mark start of refresh
            
            _uiState.update { it.copy(
                isLoading = true, 
                error = null, 
                processingProgress = 0f,
                totalSmsToProcess = 0,
                debugLog = "Starting processing in ${_uiState.value.processingMode} mode...",
                
                // Clear data visuals as requested
                todayInflow = 0.0,
                todayOutflow = 0.0,
                netBalance = 0.0,
                recentTransactions = emptyList(),
                allTransactions = emptyList(),
                totalTransactionCount = 0,
                unclassifiedCount = 0
            ) }
            
            // Clear DB data as per user request (Fresh Start)
            repository.deleteAll()
            Log.d(TAG, "Cleared all existing transaction data.")
            
            val smsList = smsReader.readFinancialSms(contentResolver)
            Log.d(TAG, "Read ${smsList.size} SMS messages")
            
            _uiState.update { it.copy(totalSmsToProcess = smsList.size) }
            
            val validTransactions = mutableListOf<ParseResult.Success>()
            var processedCount = 0
            var rejectedCount = 0
            var parseFailedCount = 0
            
            val isRulesMode = _uiState.value.processingMode == "RULES"
            val aiBatchPending = mutableListOf<SmsMessageRequest>()
            val aiBatchSourceResults = mutableMapOf<String, ParseResult.Success>()
            
            for ((index, sms) in smsList.withIndex()) {
                yield()
                if (index % 5 == 0) delay(10)

                // STAGE 1: Try Strict Rules (Fast, No AI)
                val strictResult = smsParser.parse(sms, useStrictRules = true)
                
                if (strictResult is ParseResult.Success) {
                    validTransactions.add(strictResult)
                    processedCount++
                    continue
                }

                // STAGE 2: If Strict failed, and we are in HYBRID mode, collect for Batch AI
                if (!isRulesMode) {
                    val looseResult = smsParser.parse(sms, useStrictRules = false)
                    if (looseResult is ParseResult.Success) {
                        val smsId = "sms_${sms.date}_${sms.address.hashCode()}"
                        aiBatchPending.add(SmsMessageRequest(smsId, sms.body))
                        aiBatchSourceResults[smsId] = looseResult
                    } else {
                        parseFailedCount++
                    }
                } else {
                    parseFailedCount++
                }
                
                // Update progress for rules stage
                if (index > 0 && index % 10 == 0) {
                    val progress = index.toFloat() / smsList.size.toFloat() * 0.3f // First 30% is rules
                    _uiState.update { it.copy(processingProgress = progress) }
                }
            }

            // STAGE 3: Process AI Batch in chunks of 10
            if (aiBatchPending.isNotEmpty()) {
                _uiState.update { it.copy(debugLog = it.debugLog + "\nSending ${aiBatchPending.size} messages to AI in batches...") }
                
                aiBatchPending.chunked(10).forEach { chunk ->
                    val results = localAIService.processSmsBatch(chunk)
                    results.forEach { verification ->
                        val sourceResult = aiBatchSourceResults[verification.id]
                        if (verification.isTransaction && sourceResult != null) {
                            val finalType = when (verification.transactionType) {
                                "DEBIT" -> TransactionType.DEBIT
                                "CREDIT" -> TransactionType.CREDIT
                                else -> sourceResult.type
                            }
                            
                            // Use AI-suggested category if available
                            val aiCategory = verification.category?.let { 
                                com.expense.tracker.domain.model.Category.fromString(it) 
                            }
                            
                            val aiVerifiedResult = sourceResult.copy(
                                type = finalType,
                                merchant = verification.merchant ?: sourceResult.merchant,
                                description = verification.merchant ?: sourceResult.description
                            )
                            validTransactions.add(aiVerifiedResult)
                        } else if (!verification.isTransaction) {
                            rejectedCount++
                        }
                    }
                    processedCount += chunk.size
                    val progress = 0.3f + (processedCount.toFloat() / smsList.size.toFloat() * 0.7f) // Remaining 70% is AI
                    _uiState.update { it.copy(
                        processingProgress = progress,
                        debugLog = it.debugLog + "\nProcessed ${processedCount}/${smsList.size}..."
                    ) }
                }
            }
            
            // STAGE 4: Bulk insert new transactions
            val newTransactions = mutableListOf<Transaction>()
            for (parsed in validTransactions) {
                if (!repository.existsByHash(parsed.rawSmsHash)) {
                    val categorizationResult = merchantCategorizer.categorize(
                        parsed.merchant, 
                        parsed.description
                    )
                    
                    newTransactions.add(Transaction(
                        timestamp = parsed.timestamp,
                        amount = parsed.amount,
                        type = parsed.type, 
                        source = parsed.source,
                        description = parsed.description ?: "",
                        category = categorizationResult.category,
                        rawSmsHash = parsed.rawSmsHash,
                        fullBody = parsed.fullBody
                    ))
                }
            }
            
            if (newTransactions.isNotEmpty()) {
                repository.insertTransactions(newTransactions)
            }
            
            val summary = "DONE. SMS=${smsList.size}, Valid=${validTransactions.size}, New=${newTransactions.size}. (Rules Mode: $isRulesMode)"
            Log.d(TAG, summary)
            
            isRefreshing = false // Done refreshing
            
            _uiState.update { 
                it.copy(
                    debugLog = it.debugLog + "\n\n" + summary,
                    isLoading = false,
                    processingProgress = 1f,
                    error = null
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "SMS processing failed", e)
            isRefreshing = false // Error means done
            _uiState.update { 
                it.copy(
                    isLoading = false, 
                    error = "Failed to process SMS: ${e.message}"
                ) 
            }
        }
        }
    }
    
    /**
     * Check if using AI or rule-based
     */
    fun isUsingAI(): Boolean {
        return localAIService.isReady()
    }
    

    /**
     * Called when SMS permission is granted.
     * NOW AI-GATED: Only processes if AI is ready.
     */
    fun onSmsPermissionGranted(contentResolver: ContentResolver) {
        viewModelScope.launch {
            // Simply trigger the main processing logic which respects the current mode
            // We assume this is called when permission is newly granted
            _uiState.update { it.copy(hasSmsPermission = true) }
            processSmsWithAI(contentResolver)
        }
    }
    
    fun updateSmsPermissionStatus(hasPermission: Boolean) {
        _uiState.update { it.copy(hasSmsPermission = hasPermission) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return startOfDay to endOfDay
    }
    
    /**
     * Fetch AI-generated insight for the home widget
     */
    fun fetchAIInsight() {
        if (!localAIService.isReady()) {
            _uiState.update { it.copy(aiInsight = "Enable AI in Settings to get spending insights ✨") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInsight = true) }
            
            try {
                val insight = localAIService.generateInsight()
                _uiState.update { 
                    it.copy(
                        aiInsight = insight ?: "Looking good! Keep tracking your expenses 💰",
                        isLoadingInsight = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate insight", e)
                _uiState.update { 
                    it.copy(
                        aiInsight = "Couldn't generate insight. Try again later.",
                        isLoadingInsight = false
                    )
                }
            }
        }
    }
}
