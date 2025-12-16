package com.expense.tracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expense.tracker.data.local.db.entities.TransactionEntity

/**
 * Room database for the Expense Tracker app.
 * All data is stored locally - no cloud sync.
 */
@Database(
    entities = [TransactionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        const val DATABASE_NAME = "expense_tracker_db"
    }
}
