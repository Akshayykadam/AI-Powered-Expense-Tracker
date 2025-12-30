package com.expense.tracker.data.local.sms

import com.expense.tracker.domain.model.TransactionType
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Result of SMS parsing - either a valid parsed transaction or unparseable
 */
sealed class ParseResult {
    data class Success(
        val amount: Double,
        val type: TransactionType,
        val source: String,
        val description: String?,
        val merchant: String?,
        val timestamp: Long,
        val rawSmsHash: String,
        val fullBody: String // Added field
    ) : ParseResult()
    
    data class Failure(val reason: String) : ParseResult()
}

/**
 * India-focused rule-based SMS parser for extracting transaction data.
 * 100% offline - optimized for Indian bank SMS formats.
 * 
 * Filters out informational messages and only captures ACTUAL money movements.
 */
class SmsParser @Inject constructor() {
    
    companion object {
        // ==================== INFORMATIONAL MESSAGE FILTERS ====================
        // These patterns indicate non-transactional SMS that should be IGNORED
        
        private val IGNORE_PATTERNS = listOf(
            // Plan expiry/validity (Jio, Airtel, Vi, BSNL)
            Regex("""(?:has|have|will|is)\s+expired?""", RegexOption.IGNORE_CASE),
            Regex("""expir(?:es|ing|ed)\s+(?:on|today|soon|at)""", RegexOption.IGNORE_CASE),
            Regex("""valid(?:ity)?\s+(?:till|until|expires|ends)""", RegexOption.IGNORE_CASE),
            Regex("""plan\s+(?:has\s+)?expired""", RegexOption.IGNORE_CASE),
            Regex("""will\s+expire\s+on""", RegexOption.IGNORE_CASE),
            Regex("""pack\s+(?:expired|expiring)""", RegexOption.IGNORE_CASE),
            Regex("""recharge\s+before""", RegexOption.IGNORE_CASE),
            
            // Status/Confirmation messages 
            Regex("""status\s*:""", RegexOption.IGNORE_CASE),
            Regex("""(?:is|has been)\s+(?:activated|registered|linked|blocked|unblocked)""", RegexOption.IGNORE_CASE),
            Regex("""successfully\s+(?:activated|registered|linked|updated|submitted|verified)""", RegexOption.IGNORE_CASE),
            Regex("""request\s+(?:received|submitted|accepted)""", RegexOption.IGNORE_CASE),
            
            // Reminders and alerts
            Regex("""reminder\s*:""", RegexOption.IGNORE_CASE),
            Regex("""alert\s*:""", RegexOption.IGNORE_CASE),
            Regex("""(?:pay|recharge)\s+now\s+to""", RegexOption.IGNORE_CASE),
            Regex("""renew\s+(?:now|today|your)""", RegexOption.IGNORE_CASE),
            Regex("""last\s+date\s+to\s+pay""", RegexOption.IGNORE_CASE),
            Regex("""due\s+date""", RegexOption.IGNORE_CASE),
            Regex("""bill\s+generated""", RegexOption.IGNORE_CASE),
            
            // Balance inquiry
            Regex("""(?:avl|available)\s+(?:bal|balance)\s*(?:is|:)""", RegexOption.IGNORE_CASE),
            Regex("""balance\s+(?:is|:)\s*(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""your\s+(?:a/c|account|ac)\s+balance""", RegexOption.IGNORE_CASE),
            Regex("""ledger\s+balance""", RegexOption.IGNORE_CASE),
            Regex("""current\s+balance""", RegexOption.IGNORE_CASE),
            
            // OTP and security
            Regex("""OTP\s*(?:is|:)?\s*\d{4,8}""", RegexOption.IGNORE_CASE),
            Regex("""one\s*time\s*password""", RegexOption.IGNORE_CASE),
            Regex("""verification\s+code""", RegexOption.IGNORE_CASE),
            Regex("""CVV|PIN|MPIN|ATM\s+PIN""", RegexOption.IGNORE_CASE),
            Regex("""do\s+not\s+share""", RegexOption.IGNORE_CASE),
            
            // Promotional
            Regex("""win\s+(?:up\s+to|upto)""", RegexOption.IGNORE_CASE),
            Regex("""congratulations""", RegexOption.IGNORE_CASE),
            Regex("""offer\s+(?:valid|expires|available)""", RegexOption.IGNORE_CASE),
            Regex("""cashback\s+(?:offer|up\s+to)""", RegexOption.IGNORE_CASE),
            Regex("""get\s+(?:upto|up\s+to)\s+\d+%""", RegexOption.IGNORE_CASE),
            Regex("""limited\s+(?:time|period)\s+offer""", RegexOption.IGNORE_CASE),
            
            // FD/RD/Account status
            Regex("""(?:FD|RD|deposit)\s+(?:maturity|maturing|matures)""", RegexOption.IGNORE_CASE),
            Regex("""(?:FD|RD)\s+(?:booked|created)\s+successfully""", RegexOption.IGNORE_CASE),
            Regex("""acknowledgement\s*(?:no|number|:)""", RegexOption.IGNORE_CASE),
            Regex("""reference\s+(?:no|number|id)\s*:""", RegexOption.IGNORE_CASE),
            
            // Loan and EMI reminders (not the actual debit)
            Regex("""EMI\s+(?:due|reminder|of\s+Rs).*(?:due\s+on|pay\s+by)""", RegexOption.IGNORE_CASE),
            Regex("""loan\s+(?:application|approved|disbursed)""", RegexOption.IGNORE_CASE),
            
            // Card/Account services
            Regex("""card\s+(?:blocked|unblocked|dispatched|activated)""", RegexOption.IGNORE_CASE),
            Regex("""limit\s+(?:increased|decreased|changed)""", RegexOption.IGNORE_CASE),
            
            // Payment Requests and Bill Notices (Not Spends)
            Regex("""(?i)bill.*(?:due\s+on|pay\s+within)"""), 
            Regex("""(?i)disconnection\s+notice"""),
            Regex("""(?i)requested\s+money"""),
            Regex("""(?i)on\s+approving"""),
            Regex("""(?i)request\s+-\s+https"""),
        )
        
        // ==================== VALID TRANSACTION PATTERNS (India-specific) ====================
        
        private val VALID_TRANSACTION_PATTERNS = listOf(
            // Clear debit indicators
            Regex("""debited\s+(?:from|by|for|with|Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""(?:Rs\.?|INR|₹)\s*[\d,]+(?:\.\d{2})?\s+(?:has\s+been\s+)?debited""", RegexOption.IGNORE_CASE),
            Regex("""withdrawn\s+(?:Rs|INR|₹|from)""", RegexOption.IGNORE_CASE),
            Regex("""txn\s+of\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""purchase\s+of\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            
            // Clear credit indicators  
            Regex("""credited\s+(?:to|by|with|Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""(?:Rs\.?|INR|₹)\s*[\d,]+(?:\.\d{2})?\s+(?:has\s+been\s+)?credited""", RegexOption.IGNORE_CASE),
            Regex("""deposited\s+(?:Rs|INR|₹|to|in)""", RegexOption.IGNORE_CASE),
            Regex("""received\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            
            // UPI specific (India)
            Regex("""UPI(?:/P2P|/P2M)?\s+of\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""sent\s+(?:Rs|INR|₹).*(?:via\s+UPI|to\s+\w+@)""", RegexOption.IGNORE_CASE),
            Regex("""paid\s+(?:Rs|INR|₹).*(?:UPI|@)""", RegexOption.IGNORE_CASE),
            
            // NEFT/RTGS/IMPS (India)
            Regex("""(?:NEFT|RTGS|IMPS)\s+(?:of|for)\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""transfer\s+of\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            
            // Card transactions
            Regex("""card\s+(?:ending|xx)\d+\s+.*(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""spent\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            
            // Payments
            Regex("""payment\s+of\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""paid\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            
            // Recharge (actual payment, not reminder)
            Regex("""recharge(?:d)?\s+(?:of|for|with)\s+(?:Rs|INR|₹).*(?:successful|done)""", RegexOption.IGNORE_CASE),
            
            // Refunds
            Regex("""refund\s+of\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""(?:Rs|INR|₹)[\d,]+.*refund(?:ed)?""", RegexOption.IGNORE_CASE),
            
            // EMI/Loan debit (actual debit)
            Regex("""EMI\s+(?:of\s+)?(?:Rs|INR|₹)[\d,]+.*debited""", RegexOption.IGNORE_CASE),
            Regex("""auto\s*debit\s+of\s+(?:Rs|INR|₹)""", RegexOption.IGNORE_CASE),
        )
        
        // Amount patterns - captures INR amounts
        private val AMOUNT_PATTERNS = listOf(
            Regex("""Rs\.?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""INR\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""₹\s*([0-9,]+(?:\.[0-9]{1,2})?)"""),
            Regex("""Rupees?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(?:Amt|Amount|Txn)\.?\s*[:=-]?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            // SUPER PERMISSIVE: Any number > 100 with decimals (likely correct) or just currency-like formatting
            // This relies on AI to reject false positives (like OTPs)
            Regex("""(?:\s|^)([0-9]{2,7}(?:,[0-9]{3})*\.[0-9]{2})(?:\s|$)"""),
            
            // INT PERMISSIVE: Integer numbers > 2 digits (e.g. 500, 1000) - For messages like "Paid 500"
            // Strict Mode 'hasValidTransactionIndicator' will filter out valid Integers that are just OTPs
            Regex("""(?:\s|^)([0-9]{2,7}(?:,[0-9]{3})*)(?:\s|$)"""),
        )
        
        // Debit indicators
        private val DEBIT_PATTERNS = listOf(
            Regex("""debited""", RegexOption.IGNORE_CASE),
            Regex("""withdrawn""", RegexOption.IGNORE_CASE),
            Regex("""spent""", RegexOption.IGNORE_CASE),
            Regex("""paid(?!\s+to\s+you)""", RegexOption.IGNORE_CASE),  // "paid" but not "paid to you"
            Regex("""purchase""", RegexOption.IGNORE_CASE),
            Regex("""payment\s+(?:of|to)""", RegexOption.IGNORE_CASE),
            Regex("""sent\s+(?:to|Rs|INR|₹)""", RegexOption.IGNORE_CASE),
            Regex("""transferred\s+to""", RegexOption.IGNORE_CASE),
            Regex("""debit""", RegexOption.IGNORE_CASE),
            Regex("""\bdr\b""", RegexOption.IGNORE_CASE),
            Regex("""recharged?\s+(?:of|for|with)""", RegexOption.IGNORE_CASE),
            Regex("""auto\s*debit""", RegexOption.IGNORE_CASE),
            Regex("""txn\s+of""", RegexOption.IGNORE_CASE),
        )
        
        // Credit indicators
        private val CREDIT_PATTERNS = listOf(
            Regex("""credited""", RegexOption.IGNORE_CASE),
            Regex("""received\s+(?:Rs|INR|₹|from)""", RegexOption.IGNORE_CASE),
            Regex("""deposited""", RegexOption.IGNORE_CASE),
            Regex("""refund""", RegexOption.IGNORE_CASE),
            Regex("""cashback""", RegexOption.IGNORE_CASE),
            Regex("""credit(?!.*card)""", RegexOption.IGNORE_CASE),  // "credit" but not "credit card"
            Regex("""\bcr\b""", RegexOption.IGNORE_CASE),
            Regex("""added\s+to\s+(?:your|a/c|ac|account)""", RegexOption.IGNORE_CASE),
            Regex("""salary""", RegexOption.IGNORE_CASE),
            Regex("""paid\s+to\s+you""", RegexOption.IGNORE_CASE),
        )
        
        // Merchant extraction patterns (India-specific)
        private val MERCHANT_PATTERNS = listOf(
            // UPI VPA
            Regex("""(?:to|from|at)\s+([a-z0-9._-]+@[a-z]+)""", RegexOption.IGNORE_CASE),
            // Merchant name after "at" or "to"
            Regex("""(?:at|to)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+via|\s+ref|\.|\s+UPI|\s+VPA|\s*$)"""),
            // UPI ID format
            Regex("""VPA\s*:?\s*([a-z0-9._-]+@[a-z]+)""", RegexOption.IGNORE_CASE),
            Regex("""UPI[:/]([A-Za-z0-9\s]+?)[/\s]"""),
            // For/via merchant
            Regex("""(?:for|via)\s+([A-Z][A-Za-z0-9\s]+?)(?:\s+on|\s+ref|\.|$)"""),
        )
        
        // Source extraction from sender (Indian banks and wallets)
        private val SOURCE_MAPPING = mapOf(
            // Public Sector Banks
            "SBI" to "SBI", "SBIN" to "SBI",
            "PNB" to "Punjab National Bank",
            "CANARA" to "Canara Bank",
            "BOB" to "Bank of Baroda",
            "BOI" to "Bank of India",
            "UNION" to "Union Bank",
            "INDIAN" to "Indian Bank",
            "UCO" to "UCO Bank",
            "CENTRAL" to "Central Bank",
            "IDBI" to "IDBI Bank",
            
            // Private Banks
            "HDFC" to "HDFC Bank", "HDFCBK" to "HDFC Bank",
            "ICICI" to "ICICI Bank", "ICICIB" to "ICICI Bank",
            "AXIS" to "Axis Bank", "UTIB" to "Axis Bank",
            "KOTAK" to "Kotak Bank",
            "YES" to "Yes Bank",
            "INDUS" to "IndusInd Bank",
            "FEDERAL" to "Federal Bank",
            "RBL" to "RBL Bank",
            "BANDHAN" to "Bandhan Bank",
            "IDFC" to "IDFC First",
            "AU" to "AU Small Finance",
            
            // Foreign Banks
            "CITI" to "Citibank",
            "HSBC" to "HSBC",
            "DBS" to "DBS Bank",
            "SCB" to "Standard Chartered",
            "AMEX" to "American Express",
            
            // Payment Apps/Wallets
            "GPAY" to "Google Pay",
            "PAYTM" to "Paytm",
            "PHONEPE" to "PhonePe", "PHON" to "PhonePe",
            "AMAZON" to "Amazon Pay",
            "MOBIKWIK" to "MobiKwik",
            "FREECHARGE" to "Freecharge",
            "CRED" to "CRED",
            "BHIM" to "BHIM",
            "SLICE" to "Slice",
            "LAZYPAY" to "LazyPay",
            "SIMPL" to "Simpl",
            
            // Telecom
            "JIO" to "Jio",
            "AIRTEL" to "Airtel",
            "VI" to "Vi",
            "BSNL" to "BSNL",
        )
    }
    
    /**
     * Parse a raw SMS into transaction data.
     * Returns Failure if SMS is informational (no actual money movement).
     */
    fun parse(sms: RawSms, useStrictRules: Boolean = false): ParseResult {
        val body = sms.body
        
        // STEP 1: Filter out informational/non-transactional messages
        // We always filter out known non-transactions to avoid false positives
        if (isInformationalMessage(body)) {
            return ParseResult.Failure("Informational message - no transaction")
        }
        
        // STEP 2: Check for valid transaction indicators
        // In Strict Mode, we require clear indicators (debited/credited/sent/paid)
        if (useStrictRules) {
            if (!hasValidTransactionIndicator(body)) {
                // Double check if context implies transaction (e.g. "Sent Rs 500")
                if (!hasAmountWithTransactionContext(body)) {
                    return ParseResult.Failure("No clear transaction indicator")
                }
            }
        }
        
        // STEP 3: Extract amount
        val amount = extractAmount(body) 
            ?: return ParseResult.Failure("Could not extract amount")
        
        // STEP 4: Validate amount (reasonable range for India)
        if (amount < 1.0 || amount > 10_00_00_000.0) {  // 1 rupee to 10 crore
            return ParseResult.Failure("Amount out of reasonable range: $amount")
        }
        
        // STEP 5: Determine transaction type
        val type = determineTransactionType(body)
        
        // STEP 6: Extract source from sender
        val source = extractSource(sms.address)
        
        // STEP 7: Extract merchant
        val merchant = extractMerchant(body)
        
        // STEP 8: Extract description
        val description = merchant ?: body.take(80)
        
        // STEP 9: Generate hash for deduplication
        val hash = generateHash(sms.address, sms.date, amount)
        
        return ParseResult.Success(
            amount = amount,
            type = type,
            source = source,
            description = description,
            merchant = merchant,
            timestamp = sms.date,
            rawSmsHash = hash,
            fullBody = body
        )
    }
    
    private fun isInformationalMessage(body: String): Boolean {
        return IGNORE_PATTERNS.any { it.containsMatchIn(body) }
    }
    
    private fun hasValidTransactionIndicator(body: String): Boolean {
        return VALID_TRANSACTION_PATTERNS.any { it.containsMatchIn(body) }
    }
    
    private fun hasAmountWithTransactionContext(body: String): Boolean {
        val hasDebitCredit = DEBIT_PATTERNS.any { it.containsMatchIn(body) } ||
                            CREDIT_PATTERNS.any { it.containsMatchIn(body) }
        val hasAmount = extractAmount(body) != null
        return hasDebitCredit && hasAmount
    }
    
    private fun extractAmount(body: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }
    
    private fun determineTransactionType(body: String): TransactionType {
        val debitScore = DEBIT_PATTERNS.count { it.containsMatchIn(body) }
        val creditScore = CREDIT_PATTERNS.count { it.containsMatchIn(body) }
        
        return when {
            creditScore > debitScore -> TransactionType.CREDIT
            debitScore > creditScore -> TransactionType.DEBIT
            // Default to debit (most common)
            else -> TransactionType.DEBIT
        }
    }
    
    private fun extractSource(address: String): String {
        val normalized = address.uppercase()
        for ((key, value) in SOURCE_MAPPING) {
            if (normalized.contains(key)) {
                return value
            }
        }
        // Clean up common prefixes
        return address
            .removePrefix("AD-")
            .removePrefix("VM-")
            .removePrefix("VK-")
            .removePrefix("AX-")
            .removePrefix("JD-")
            .removePrefix("JM-")
            .trim()
    }
    
    private fun extractMerchant(body: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(body)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.length >= 3) {
                    return merchant
                }
            }
        }
        return null
    }
    
    private fun generateHash(sender: String, timestamp: Long, amount: Double): String {
        val data = "$sender|$timestamp|$amount"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    fun parseAll(smsList: List<RawSms>): List<ParseResult.Success> {
        return smsList.mapNotNull { sms ->
            when (val result = parse(sms)) {
                is ParseResult.Success -> result
                is ParseResult.Failure -> null
            }
        }
    }
}
