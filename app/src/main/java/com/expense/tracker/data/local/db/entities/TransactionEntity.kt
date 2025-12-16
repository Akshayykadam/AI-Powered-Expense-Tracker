package com.expense.tracker.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for storing transactions.
 * The rawSmsHash is indexed for efficient deduplication checks.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["rawSmsHash"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val timestamp: Long,
    val amount: Double,
    val type: String,            // DEBIT / CREDIT
    val source: String,          // Bank / Wallet name
    val merchant: String?,
    val category: String?,       // Category enum name
    val expenseType: String?,    // NEED / WANT / TRANSFER
    val description: String?,
    val rawSmsHash: String,       // SHA-256 hash for deduplication
    val fullBody: String? = null
)
