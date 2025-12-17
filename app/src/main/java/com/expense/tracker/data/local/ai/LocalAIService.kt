package com.expense.tracker.data.local.ai

import android.content.Context
import android.util.Log
import com.expense.tracker.domain.model.Category
import com.expense.tracker.domain.model.ExpenseType
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LocalAIService"
private const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"

/**
 * Classification result from local AI
 */
data class ClassificationResult(
    val category: Category,
    val merchant: String?,
    val expenseType: ExpenseType,
    val confidence: Float
)

/**
 * Insight result from local AI
 */
data class InsightResult(
    val observation: String,
    val comparison: String?,
    val suggestion: String?
)

/**
 * SMS verification result
 */
enum class SmsIntent {
    FINANCIAL_TRANSACTION,
    INFORMATIONAL,
    UNKNOWN
}

data class SmsVerificationResult(
    val intent: SmsIntent,
    val isTransaction: Boolean,
    val transactionType: String?,
    val amount: Double?,
    val confidence: Float,
    val reason: String,
    val rawResponse: String? = null
)

/**
 * Local AI service using Google AI Edge (MediaPipe GenAI) for on-device LLM inference.
 * Uses Gemma model for categorization and insights.
 */
@Singleton
class LocalAIService @Inject constructor() {
    
    private var isModelLoaded = false
    private var modelPath: String? = null
    private var llmInference: LlmInference? = null
    
    /**
     * Check if AI model is downloaded
     */
    fun isModelDownloaded(context: Context): Boolean {
        val modelFile = File(context.getExternalFilesDir(null), MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() > 100_000_000
    }
    
    /**
     * Get model file path
     */
    fun getModelPath(context: Context): File {
        return File(context.getExternalFilesDir(null), MODEL_FILENAME)
    }
    
    /**
     * Check if AI is ready to use
     */
    fun isReady(): Boolean = isModelLoaded && llmInference != null
    
    /**
     * Initialize the AI model using MediaPipe GenAI
     */
    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        val modelFile = getModelPath(context)
        if (!modelFile.exists()) {
            Log.w(TAG, "Model file not found: ${modelFile.absolutePath}")
            return@withContext false
        }
        
        try {
            modelPath = modelFile.absolutePath
            Log.d(TAG, "Loading Gemma model from: $modelPath")
            
            // Configure LLM Inference options (minimal required settings)
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath!!)
                .setMaxTokens(256)
                .build()
            
            // Create LLM Inference instance
            llmInference = LlmInference.createFromOptions(context, options)
            
            isModelLoaded = true
            Log.d(TAG, "✅ Gemma model loaded successfully via MediaPipe GenAI")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe GenAI", e)
            isModelLoaded = false
            false
        }
    }
    
    /**
     * Classify a transaction using local AI
     */
    suspend fun classifyTransaction(
        amount: Double,
        type: String,
        description: String?,
        merchant: String?
    ): ClassificationResult? = withContext(Dispatchers.IO) {
        if (!isReady() || description.isNullOrBlank()) {
            return@withContext null
        }
        
        try {
            val prompt = buildCategorizationPrompt(amount, type, description, merchant)
            val response = generateResponse(prompt)
            parseCategorizationResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed", e)
            null
        }
    }
    
    /**
     * Verify if SMS is a financial transaction
     */
    suspend fun verifySmsIntent(smsBody: String): SmsVerificationResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            return@withContext SmsVerificationResult(
                intent = SmsIntent.UNKNOWN,
                isTransaction = false,
                transactionType = null,
                amount = null,
                confidence = 0f,
                reason = "Model not loaded",
                rawResponse = null
            )
        }
        
        try {
            val prompt = buildSmsVerificationPrompt(smsBody)
            val response = generateResponse(prompt)
            val result = parseSmsVerificationResponse(response)
            Log.d(TAG, "Parsed Result: ${result.intent} (${result.transactionType}) - Reason: ${result.reason}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "SMS verification failed", e)
            SmsVerificationResult(
                intent = SmsIntent.UNKNOWN,
                isTransaction = false,
                transactionType = null,
                amount = null,
                confidence = 0f,
                reason = "Error: ${e.message}",
                rawResponse = null
            )
        }
    }
    
    /**
     * Generate spending insight
     */
    suspend fun generateInsight(
        categoryTotals: Map<Category, Double>,
        periodLabel: String = "this week"
    ): InsightResult? = withContext(Dispatchers.IO) {
        if (!isReady() || categoryTotals.isEmpty()) {
            return@withContext null
        }
        
        try {
            val prompt = buildInsightPrompt(categoryTotals, periodLabel)
            val response = generateResponse(prompt)
            parseInsightResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Insight generation failed", e)
            null
        }
    }
    
    /**
     * Generate response using MediaPipe LLM Inference
     */
    /**
     * Generate response using MediaPipe LLM Inference
     */
    private fun generateResponse(prompt: String): String {
        val inference = llmInference ?: throw IllegalStateException("LLM not initialized")
        
        Log.d(TAG, "Generating response for prompt: ${prompt.take(100)}...")
        
        // Run inference
        val response = inference.generateResponse(prompt)
        
        Log.d(TAG, "RAW AI RESPONSE: [$response]")
        return response
    }
    
    /**
     * Build prompt for transaction categorization
     */
    private fun buildCategorizationPrompt(
        amount: Double,
        type: String,
        description: String?,
        merchant: String?
    ): String {
        return """<start_of_turn>user
Classify this Indian bank transaction into ONE category.

Transaction:
- Amount: ₹$amount
- Type: $type
- Description: ${description ?: "N/A"}
- Merchant: ${merchant ?: "N/A"}

Categories: FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT, BILLS, HEALTH, EDUCATION, TRAVEL, GROCERIES, FUEL, RECHARGE, EMI, TRANSFER, SALARY, INVESTMENT, OTHER

Reply with ONLY the category name, nothing else.<end_of_turn>
<start_of_turn>model
"""
    }
    
    /**
     * Build prompt for SMS verification
     */
    private fun buildSmsVerificationPrompt(smsBody: String): String {
        return """<start_of_turn>user
You are a strict SMS classifier for an expense tracker app. 
Classify this SMS: Is money ACTUALLY moving RIGHT NOW?

SMS: $smsBody

DEBIT = Money is being DEBITED RIGHT NOW. Look for words like: "debited", "paid", "sent", "withdrawn", "transferred", "spent"
CREDIT = Money is being CREDITED RIGHT NOW. Look for words like: "credited", "received", "refunded", "deposited"
INFORMATIONAL = Everything else! Including:
- Payment REQUESTS ("has requested money", "Click to approve")
- Bill DUE reminders ("Bill was due", "Pay within X days")
- Account OPENED notifications ("has been opened", "account created")
- FD/RD confirmations (unless it says "debited")
- Balance checks, OTPs, offers, alerts

EXAMPLES (study these carefully):
"Deposit Acct for Rs.50000 has been opened" → INFORMATIONAL (account opened, not debit)
"OpenAI has sent you an AutoPay request for Rs.399" → INFORMATIONAL (request, not paid yet)
"Bill of Rs.1140 was due. Pay within 15 days" → INFORMATIONAL (reminder, not paid)
"ZEPTO has requested money Rs.438" → INFORMATIONAL (request, not debited)
"Rs.500 debited from your account" → DEBIT (actual debit)
"Rs.1000 credited to your account" → CREDIT (actual credit)
"Recharge successful Rs.199" → DEBIT (money was spent)

KEY RULE: If it says "request", "due", "opened", "created", "approve" → INFORMATIONAL
Only say DEBIT if money HAS LEFT the account. Only say CREDIT if money HAS ENTERED.

Reply with ONE word: DEBIT, CREDIT, or INFORMATIONAL<end_of_turn>
<start_of_turn>model
"""
    }
    
    /**
     * Build prompt for spending insight
     */
    private fun buildInsightPrompt(
        categoryTotals: Map<Category, Double>,
        periodLabel: String
    ): String {
        val totalsText = categoryTotals.entries
            .sortedByDescending { it.value }
            .take(5)
            .joinToString("\n") { "- ${it.key}: ₹${String.format("%.0f", it.value)}" }
            
        return """<start_of_turn>user
Analyze this spending data for $periodLabel and give ONE brief insight (max 20 words):

$totalsText

Focus on: highest spend category or saving tip.<end_of_turn>
<start_of_turn>model
"""
    }
    
    /**
     * Parse categorization response
     */
    private fun parseCategorizationResponse(response: String): ClassificationResult? {
        // Extract category from response
        val cleanResponse = response.trim()
            .uppercase()
            .replace(Regex("[^A-Z_]"), "")
            .take(20)
        
        val category = try {
            Category.entries.find { cleanResponse.contains(it.name) } ?: Category.OTHER
        } catch (e: Exception) {
            Category.OTHER
        }
        
        return ClassificationResult(
            category = category,
            merchant = null,
            expenseType = ExpenseType.WANT,
            confidence = 0.85f
        )
    }
    
    /**
     * Parse SMS verification response
     */
    private fun parseSmsVerificationResponse(response: String): SmsVerificationResult {
        val cleanResponse = response.trim().uppercase()
        
        // Check for DEBIT, CREDIT, or INFORMATIONAL in the response
        val isDebit = cleanResponse.contains("DEBIT")
        val isCredit = cleanResponse.contains("CREDIT") && !cleanResponse.contains("CREDITED TO FD") // FD credit is actually a debit from account
        val isInformational = cleanResponse.contains("INFORMATIONAL")
        
        // Determine intent and transaction type
        val (intent, transactionType, isTransaction) = when {
            isDebit -> Triple(SmsIntent.FINANCIAL_TRANSACTION, "DEBIT", true)
            isCredit -> Triple(SmsIntent.FINANCIAL_TRANSACTION, "CREDIT", true)
            isInformational -> Triple(SmsIntent.INFORMATIONAL, null, false)
            else -> Triple(SmsIntent.UNKNOWN, null, false) // Unknown = don't process
        }
        
        return SmsVerificationResult(
            intent = intent,
            isTransaction = isTransaction,
            transactionType = transactionType,
            amount = null,
            confidence = 0.85f,
            reason = response.take(50),
            rawResponse = response
        )
    }
    
    /**
     * Parse insight response
     */
    private fun parseInsightResponse(response: String): InsightResult {
        // Clean up response - remove any special tokens
        val cleanResponse = response.trim()
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\n+"), " ")
            .trim()
        
        return InsightResult(
            observation = cleanResponse.ifEmpty { "Your spending looks balanced." },
            comparison = null,
            suggestion = null
        )
    }
    
    /**
     * Release model resources
     */
    fun release() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing LLM", e)
        }
        llmInference = null
        isModelLoaded = false
        modelPath = null
    }
}
