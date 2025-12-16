package com.expense.tracker.data.local.categorization

import com.expense.tracker.domain.model.Category
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of categorization attempt
 */
data class CategorizationResult(
    val category: Category,
    val confidence: Float,
    val source: CategorizationSource
)

enum class CategorizationSource {
    USER_OVERRIDE,      // User manually set this
    LEARNED_MERCHANT,   // Learned from user behavior
    RULE_BASED,         // Matched known merchant
    AI_CLASSIFIED,      // Gemini AI classified
    DEFAULT             // Fallback to OTHER
}

/**
 * India-specific merchant categorizer
 * Priority: User Override > Learned > Rule-based > AI > Default
 */
@Singleton
class MerchantCategorizer @Inject constructor() {
    
    // User overrides - persisted per merchant
    private val userOverrides = mutableMapOf<String, Category>()
    
    // Learned mappings from user corrections
    private val learnedMappings = mutableMapOf<String, Category>()
    
    /**
     * India-specific merchant to category mapping
     */
    private val merchantPatterns: Map<Category, List<String>> = mapOf(
        // Food & Dining
        Category.FOOD_DINING to listOf(
            "swiggy", "zomato", "eatsure", "domino", "mcdonald", "burger king",
            "kfc", "pizza hut", "starbucks", "cafe coffee day", "ccd", "chaayos",
            "haldiram", "barbeque nation", "subway", "wow momo", "faasos",
            "box8", "behrouz", "freshmenu", "oven story", "la pino", "biryani"
        ),
        
        // Grocery
        Category.GROCERY to listOf(
            "dmart", "d-mart", "big bazaar", "bigbasket", "big basket", "blinkit",
            "zepto", "instamart", "jiomart", "reliance smart", "reliance fresh",
            "more supermarket", "spar", "nature basket", "grofers", "dunzo",
            "amazon fresh", "flipkart grocery", "milkbasket"
        ),
        
        // Fuel
        Category.FUEL to listOf(
            "hpcl", "bpcl", "iocl", "indianoil", "indian oil", "hp petrol",
            "bharat petroleum", "reliance petrol", "essar", "shell", "nayara"
        ),
        
        // Medical
        Category.MEDICAL to listOf(
            "apollo pharmacy", "medplus", "netmeds", "1mg", "pharmeasy",
            "tata 1mg", "practo", "apollo hospital", "fortis", "max hospital",
            "medanta", "narayana health", "manipal hospital"
        ),
        
        // Utilities & Bills
        Category.UTILITIES to listOf(
            "bescom", "mseb", "tneb", "bses", "tata power", "adani electricity",
            "torrent power", "reliance energy", "airtel", "jio", "vodafone", "vi",
            "bsnl", "act fibernet", "hathway", "tata sky", "dish tv", "d2h",
            "mahanagar gas", "indraprastha gas", "adani gas"
        ),
        
        // Fashion & Shopping
        Category.FASHION to listOf(
            "myntra", "ajio", "flipkart", "amazon", "meesho", "nykaa",
            "tata cliq", "lifestyle", "shoppers stop", "westside", "h&m",
            "zara", "pantaloons", "max fashion", "reliance trends", "v-mart",
            "fabindia", "biba", "w for woman", "lenskart", "titan eye"
        ),
        
        // Entertainment
        Category.ENTERTAINMENT to listOf(
            "bookmyshow", "paytm movies", "pvr", "inox", "cinepolis",
            "carnival cinemas", "miraj cinemas", "netflix", "prime video",
            "hotstar", "disney", "spotify", "gaana", "wynk", "jio cinema",
            "sony liv", "zee5", "voot", "youtube premium", "gaming"
        ),
        
        // Travel & Transport
        Category.TRAVEL to listOf(
            "ola", "uber", "rapido", "meru", "irctc", "makemytrip", "goibibo",
            "cleartrip", "yatra", "easemytrip", "ixigo", "redbus", "abhibus",
            "indigo", "air india", "spicejet", "vistara", "goair", "akasa",
            "oyo", "treebo", "fabhotels", "zostel", "metro", "bmtc", "dtc"
        ),
        
        // Subscriptions
        Category.SUBSCRIPTIONS to listOf(
            "netflix", "spotify", "amazon prime", "hotstar", "zee5", "sonyliv",
            "youtube premium", "apple music", "audible", "kindle", "linkedin",
            "microsoft 365", "google one", "icloud", "dropbox", "notion"
        ),
        
        // EMI / Loan
        Category.EMI_LOAN to listOf(
            "emi", "loan", "bajaj finserv", "hdfc loan", "icici loan",
            "sbi loan", "axis loan", "home credit", "capital first",
            "tata capital", "fullerton", "mahindra finance"
        ),
        
        // Credit Card Payment
        Category.CREDIT_CARD to listOf(
            "credit card", "card payment", "hdfc card", "icici card",
            "sbi card", "axis card", "kotak card", "amex", "rbl card",
            "indusind card", "citi card", "yes bank card"
        ),
        
        // Insurance
        Category.INSURANCE to listOf(
            "lic", "life insurance", "health insurance", "car insurance",
            "icici prudential", "hdfc life", "sbi life", "max life",
            "bajaj allianz", "tata aia", "star health", "religare",
            "digit insurance", "acko", "policy bazaar"
        ),
        
        // Investments
        Category.INVESTMENTS to listOf(
            "zerodha", "groww", "upstox", "angel one", "5paisa", "kite",
            "coin", "mutual fund", "sip", "ppf", "nps", "ipo", "stock",
            "demat", "mf", "gold bond", "fi money", "jupiter", "niyo"
        ),
        
        // UPI Transfer
        Category.UPI_TRANSFER to listOf(
            "upi", "google pay", "gpay", "phonepe", "paytm", "bhim",
            "amazon pay", "whatsapp pay", "cred", "mobikwik", "freecharge"
        ),
        
        // Bank Transfer
        Category.BANK_TRANSFER to listOf(
            "neft", "rtgs", "imps", "fund transfer", "bank transfer"
        ),
        
        // Education
        Category.EDUCATION to listOf(
            "byju", "unacademy", "vedantu", "toppr", "extramarks",
            "coursera", "udemy", "skillshare", "upgrad", "scaler",
            "newton school", "masai", "coding ninjas"
        )
    )
    
    /**
     * Categorize a transaction based on merchant/description
     */
    fun categorize(merchant: String?, description: String?): CategorizationResult {
        val searchText = "${merchant.orEmpty()} ${description.orEmpty()}".lowercase()
        
        // Check user overrides first
        merchant?.let { m ->
            userOverrides[m.lowercase()]?.let { category ->
                return CategorizationResult(category, 1.0f, CategorizationSource.USER_OVERRIDE)
            }
        }
        
        // Check learned mappings
        merchant?.let { m ->
            learnedMappings[m.lowercase()]?.let { category ->
                return CategorizationResult(category, 0.95f, CategorizationSource.LEARNED_MERCHANT)
            }
        }
        
        // Rule-based matching - DISABLED as per user request for AI-only
        // for ((category, patterns) in merchantPatterns) {
        //     for (pattern in patterns) {
        //         if (searchText.contains(pattern)) {
        //             return CategorizationResult(category, 0.85f, CategorizationSource.RULE_BASED)
        //         }
        //     }
        // }
        
        // No match - needs AI or default
        return CategorizationResult(Category.OTHER, 0.0f, CategorizationSource.DEFAULT)
    }
    
    /**
     * Learn from user correction
     */
    fun learnFromUserCorrection(merchant: String, category: Category) {
        userOverrides[merchant.lowercase()] = category
    }
    
    /**
     * Store AI-learned mapping
     */
    fun storeAiMapping(merchant: String, category: Category) {
        if (!userOverrides.containsKey(merchant.lowercase())) {
            learnedMappings[merchant.lowercase()] = category
        }
    }
    
    /**
     * Check if categorization needs AI help
     */
    fun needsAiCategorization(result: CategorizationResult): Boolean {
        return result.source == CategorizationSource.DEFAULT && result.confidence < 0.7f
    }
}
