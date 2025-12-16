package com.expense.tracker.data.local.sms

/**
 * Data class representing a raw SMS message
 */
data class RawSms(
    val id: Long,
    val address: String,   // Sender
    val body: String,      // Message content
    val date: Long         // Timestamp
)
