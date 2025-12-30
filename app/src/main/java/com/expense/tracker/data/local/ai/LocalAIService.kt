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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
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
    val id: String,
    val isTransaction: Boolean,
    val amount: Double?,
    val merchant: String?,
    val category: String?,
    val confidence: Float,
    val transactionType: String? = null
)

@Serializable
data class SmsBatchRequest(
    val messages: List<SmsMessageRequest>
)

@Serializable
data class SmsMessageRequest(
    val id: String,
    val text: String
)

@Serializable
data class SmsMessageResponse(
    val id: String,
    val isExpense: Boolean,
    val amount: Double?,
    val merchant: String?,
    val category: String?,
    val confidence: Float
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
                    temperature = 0.1f // Minimal creativity for deterministic JSON
                    topK = 32
                    topP = 1f
                    maxOutputTokens = 2048 // Increased for batch processing
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
    
    private val jsonHelper = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        encodeDefaults = true
    }

    suspend fun processSmsBatch(messages: List<SmsMessageRequest>): List<SmsVerificationResult> = withContext(Dispatchers.IO) {
        if (!isReady() || messages.isEmpty()) return@withContext emptyList()
        
        try {
            val inputJson = jsonHelper.encodeToString(messages)
            
            val prompt = """
                You are a deterministic financial data processor. 
                Convert the following SMS messages into a machine-readable JSON array of transaction details.
                
                INPUT FORMAT (JSON Array):
                [{"id": "...", "text": "..."}]
                
                RULES:
                1. Return VALID JSON ONLY. No explanation, no markdown, no comments.
                2. Set isExpense=true ONLY for completed debits, payments, or spends.
                3. Set isExpense=false for reminders, OTPs, balance inquiries, credit card due alerts, or service status (e.g. "Recharge Successful").
                4. EXTRACT 'amount' CAREFULLY: Remove currency symbols (Rs, INR, ₹) and commas. Example: "Rs. 1,200.50" -> 1200.50.
                5. EXTRACT 'merchant': For UPI/Bank transfers, look for the receiver name (e.g. "Paid to RAJESH"). If brand name exists (e.g. "Swiggy", "Uber"), use it.
                6. Predict the 'category' from: FOOD_DINING, GROCERY, FUEL, MEDICAL, UTILITIES, RENT, FASHION, ENTERTAINMENT, TRAVEL, SUBSCRIPTIONS, EMI_LOAN, CREDIT_CARD, INSURANCE, INVESTMENTS, UPI_TRANSFER, BANK_TRANSFER, INCOME, EDUCATION, OTHER.
                7. Input array length MUST match output array length perfectly. IDs must match.

                EXAMPLES:
                Input: {"id":"1", "text":"Rs. 5,000 debited for Rent to LANDLORD via UPI"}
                Output: {"id":"1", "isExpense":true, "amount":5000.0, "merchant":"LANDLORD", "category":"RENT", "confidence":0.9}

                Input: {"id":"2", "text":"Your bill of Rs 499 is due on 15th"}
                Output: {"id":"2", "isExpense":false, "amount":null, "merchant":null, "category":null, "confidence":1.0}

                Input: {"id":"3", "text":"Paid INR 120.00 at STARBUCKS"}
                Output: {"id":"3", "isExpense":true, "amount":120.0, "merchant":"STARBUCKS", "category":"FOOD_DINING", "confidence":0.95}
                
                INPUT DATA:
                $inputJson
                
                OUTPUT FORMAT (JSON Array):
                [{"id": "string", "isExpense": boolean, "amount": number|null, "merchant": "string|null", "category": "string|null", "confidence": float}]
            """.trimIndent()
            
            val responseText = generativeModel?.generateContent(prompt)?.text?.trim() 
                ?.replace("```json", "")?.replace("```", "") ?: "[]"
            
            Log.d(TAG, "Batch Response: $responseText")
            
            val batchResponse = jsonHelper.decodeFromString<List<SmsMessageResponse>>(responseText)
            
            batchResponse.map { resp ->
                SmsVerificationResult(
                    id = resp.id,
                    isTransaction = resp.isExpense,
                    amount = resp.amount,
                    merchant = resp.merchant,
                    category = resp.category,
                    confidence = resp.confidence,
                    transactionType = if (resp.isExpense) "DEBIT" else null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Batch processing failed", e)
            emptyList()
        }
    }

    // Deprecated in favor of processSmsBatch
    suspend fun verifySmsIntent(smsBody: String): SmsVerificationResult = withContext(Dispatchers.IO) {
        val dummyId = "single_verify"
        val result = processSmsBatch(listOf(SmsMessageRequest(dummyId, smsBody))).firstOrNull()
        
        result ?: SmsVerificationResult(
            id = dummyId,
            isTransaction = false,
            amount = null,
            merchant = null,
            category = null,
            confidence = 0f
        )
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
            
                val topMerchants = recentDebit.filter { !it.merchant.isNullOrBlank() }
                    .groupBy { it.merchant!! }
                    .mapValues { it.value.sumOf { t -> t.amount } }
                    .toList().sortedByDescending { it.second }.take(3)
                    .joinToString(", ") { "${it.first}: ₹${it.second.toInt()}" }

                val paymentModes = recentDebit.groupBy { 
                    val text = (it.fullBody ?: it.description ?: "").uppercase()
                    if (text.contains("UPI") || text.contains("VPA")) "UPI" else "Card/Other"
                }.mapValues { it.value.size }
                
                val prompt = """
                    Generate a ONE sentence friendly, specific spending observation for this user.
                    
                    Recent spending: ₹${total.toInt()} across ${recentDebit.size} transactions
                    Top Categories: $categoryBreakdown
                    Top Merchants: $topMerchants
                    Payment Mix: $paymentModes
                    
                    Rules:
                    1. Mention a specific Merchant or Category if spending is high.
                    2. Be witty, GenZ style (use slang like 'cooked', 'slayed', 'real ones').
                    3. Use emojis. Max 15 words.
                """.trimIndent()
            
            generativeModel?.generateContent(prompt)?.text?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Insight generation failed", e)
            null
        }
    }
}
