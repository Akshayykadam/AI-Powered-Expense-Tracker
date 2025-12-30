package com.expense.tracker.data.local.ai

import android.content.Context
import android.util.Log
import com.expense.tracker.BuildConfig
import com.expense.tracker.data.repository.TransactionRepository
import com.expense.tracker.domain.model.Category
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeminiAIService"

/**
 * Classification result from AI
 */
data class ClassificationResult(
    val category: Category,
    val merchant: String?,
    val isDebit: Boolean,
    val confidence: Float
)

/**
 * Insight result from AI
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
 * AI service using Google Gemini API (Cloud)
 * Includes RAG (Retrieval-Augmented Generation) for better accuracy.
 */
@Singleton
class LocalAIService @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    
    private val modelName = "gemma-3-27b-it" 
    private var generativeModel: GenerativeModel? = null
    private var isInitialized = false

    fun isReady(): Boolean = isInitialized

    suspend fun initialize(context: Context): Boolean {
        if (isInitialized) return true

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "Gemini API Key is missing!")
            return false
        }

        try {
            generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.4f
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 1024
                }
            )
            isInitialized = true
            Log.d(TAG, "Gemini AI initialized with model: $modelName")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Gemini AI", e)
            return false
        }
    }
    
    fun release() {
        generativeModel = null
        isInitialized = false
    }

    private suspend fun getCategorizationContext(description: String, merchant: String?): String {
        try {
            val transactions = transactionRepository.getAllTransactions().first()
            val searchTerms = description.split(" ").filter { it.isNotBlank() }
            if (searchTerms.isEmpty()) return ""
            
            val relevant = transactions.filter { tx ->
                (merchant != null && tx.description?.contains(merchant, ignoreCase = true) == true) ||
                searchTerms.any { term -> tx.description?.contains(term, ignoreCase = true) == true }
            }.take(5)

            if (relevant.isEmpty()) return ""

            val historyString = relevant.joinToString("\n") {
                "- \"${it.description}\" -> ${it.category.name}"
            }
            return "\n\nPrevious similar transactions (for reference):\n$historyString"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get RAG context", e)
            return ""
        }
    }

    suspend fun classifyTransaction(
        amount: Double,
        type: String,
        description: String?,
        merchant: String?
    ): ClassificationResult? = withContext(Dispatchers.IO) {
        if (!isReady() || description.isNullOrBlank()) return@withContext null
        
        try {
            val ragContext = getCategorizationContext(description, merchant)
            
            val prompt = """
                Classify this Indian bank transaction into ONE category.
                
                Categories: FOOD_DINING, GROCERY, FUEL, MEDICAL, UTILITIES, RENT, FASHION, ENTERTAINMENT, TRAVEL, SUBSCRIPTIONS, EMI_LOAN, CREDIT_CARD, INSURANCE, INVESTMENTS, UPI_TRANSFER, BANK_TRANSFER, INCOME, EDUCATION, OTHER
                
                Transaction:
                - Amount: ₹$amount ($type)
                - Description: $description
                ${if (merchant != null) "- Merchant: $merchant" else ""}
                $ragContext
                
                Respond ONLY with: CATEGORY|MERCHANT_NAME
                Example: FOOD_DINING|Swiggy
            """.trimIndent()
            
            val response = generativeModel?.generateContent(prompt)?.text ?: return@withContext null
            val parts = response.trim().split("|")
            val categoryName = parts.getOrNull(0)?.trim()?.uppercase() ?: "OTHER"
            val merchantName = parts.getOrNull(1)?.trim()
            
            ClassificationResult(
                category = Category.fromString(categoryName),
                merchant = merchantName,
                isDebit = type.equals("DEBIT", ignoreCase = true),
                confidence = 0.85f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed", e)
            null
        }
    }
    
    suspend fun verifySmsIntent(smsBody: String): SmsVerificationResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            return@withContext SmsVerificationResult(
                intent = SmsIntent.UNKNOWN,
                isTransaction = false,
                transactionType = null,
                amount = null,
                confidence = 0f,
                reason = "AI not initialized"
            )
        }
        
        try {
            val prompt = """
                Is this SMS a financial transaction?
                
                SMS: "$smsBody"
                
                Respond ONLY with: YES|DEBIT|AMOUNT or YES|CREDIT|AMOUNT or NO|REASON
                Example: YES|DEBIT|500 or NO|OTP message
            """.trimIndent()
            
            val response = generativeModel?.generateContent(prompt)?.text?.trim() ?: "NO|Error"
            
            if (response.startsWith("YES")) {
                val parts = response.split("|")
                SmsVerificationResult(
                    intent = SmsIntent.FINANCIAL_TRANSACTION,
                    isTransaction = true,
                    transactionType = parts.getOrNull(1)?.trim(),
                    amount = parts.getOrNull(2)?.trim()?.toDoubleOrNull(),
                    confidence = 0.9f,
                    reason = "AI detected transaction",
                    rawResponse = response
                )
            } else {
                SmsVerificationResult(
                    intent = SmsIntent.INFORMATIONAL,
                    isTransaction = false,
                    transactionType = null,
                    amount = null,
                    confidence = 0.9f,
                    reason = response.substringAfter("|").trim(),
                    rawResponse = response
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS verification failed", e)
            SmsVerificationResult(
                intent = SmsIntent.UNKNOWN,
                isTransaction = false,
                transactionType = null,
                amount = null,
                confidence = 0f,
                reason = "Error: ${e.message}"
            )
        }
    }
    
    suspend fun generateInsight(): String? = withContext(Dispatchers.IO) {
        if (!isReady()) return@withContext null
        
        try {
            val transactions = transactionRepository.getAllTransactions().first()
            val recentDebit = transactions.filter { it.isDebit }.take(10)
            
            if (recentDebit.isEmpty()) return@withContext "Start tracking expenses to get AI insights!"
            
            val total = recentDebit.sumOf { it.amount }
            val categoryBreakdown = recentDebit.groupBy { it.category }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .entries.sortedByDescending { it.value }
                .take(3)
                .joinToString(", ") { "${it.key.displayName}: ₹${it.value.toInt()}" }
            
            val prompt = """
                Generate a ONE sentence friendly spending tip for this user.
                
                Recent spending: ₹${total.toInt()} across ${recentDebit.size} transactions
                Top categories: $categoryBreakdown
                
                Keep it casual and helpful. Use emojis. Max 15 words.
            """.trimIndent()
            
            generativeModel?.generateContent(prompt)?.text?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Insight generation failed", e)
            null
        }
    }
}
