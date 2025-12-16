package com.expense.tracker.domain.model

/**
 * AI-generated insight about spending behavior.
 * Only contains aggregated analysis, never raw SMS data.
 */
data class Insight(
    val id: Long = 0,
    val timestamp: Long,
    val period: InsightPeriod,
    val observation: String,        // Pattern identified
    val comparison: String?,        // Comparison with previous period
    val suggestion: String?,        // One actionable suggestion
    val categoryBreakdown: Map<Category, Double> = emptyMap()
)

enum class InsightPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}
