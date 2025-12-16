package com.expense.tracker.data.local.db

import androidx.room.*
import com.expense.tracker.data.local.db.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transactions.
 * All aggregate queries are computed at runtime - no precomputed values.
 */
@Dao
interface TransactionDao {
    
    // ==================== INSERT ====================
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>
    
    // ==================== UPDATE ====================
    
    @Update
    suspend fun update(transaction: TransactionEntity)
    
    @Query("UPDATE transactions SET category = :category, merchant = :merchant, expenseType = :expenseType WHERE id = :id")
    suspend fun updateClassification(id: Long, category: String?, merchant: String?, expenseType: String?)
    
    // ==================== DELETE ====================
    
    @Delete
    suspend fun delete(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
    
    // ==================== QUERIES ====================
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE rawSmsHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): TransactionEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE rawSmsHash = :hash)")
    suspend fun existsByHash(hash: String): Boolean
    
    // ==================== TIME-BASED QUERIES ====================
    
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayTransactions(startOfDay: Long): Flow<List<TransactionEntity>>
    
    // ==================== AGGREGATE QUERIES (Computed) ====================
    
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'CREDIT' AND timestamp >= :startTime AND timestamp < :endTime")
    fun getTotalInflow(startTime: Long, endTime: Long): Flow<Double>
    
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :startTime AND timestamp < :endTime")
    fun getTotalOutflow(startTime: Long, endTime: Long): Flow<Double>
    
    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0.0) FROM transactions WHERE timestamp >= :startTime AND timestamp < :endTime")
    fun getNetBalance(startTime: Long, endTime: Long): Flow<Double>
    
    // ==================== CATEGORY AGGREGATES ====================
    
    @Query("""
        SELECT category, COALESCE(SUM(amount), 0.0) as total 
        FROM transactions 
        WHERE type = 'DEBIT' AND timestamp >= :startTime AND timestamp < :endTime 
        GROUP BY category
    """)
    fun getCategoryTotals(startTime: Long, endTime: Long): Flow<List<CategoryTotal>>
    
    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM transactions WHERE category IS NULL OR category = 'OTHER'")
    fun getUnclassifiedCount(): Flow<Int>
}

/**
 * Helper data class for category aggregate queries
 */
data class CategoryTotal(
    val category: String?,
    val total: Double
)
