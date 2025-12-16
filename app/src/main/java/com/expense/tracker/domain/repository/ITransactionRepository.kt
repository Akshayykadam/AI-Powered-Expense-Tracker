package com.expense.tracker.domain.repository

import com.expense.tracker.domain.model.Category
import com.expense.tracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transaction data.
 * Defines the contract for data operations without implementation details.
 */
interface ITransactionRepository {
    
    // ==================== CRUD Operations ====================
    
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun updateClassification(id: Long, category: Category?, merchant: String?, expenseType: String?)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteAll()
    
    // ==================== Queries ====================
    
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun existsByHash(hash: String): Boolean
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>>
    fun getTodayTransactions(): Flow<List<Transaction>>
    
    // ==================== Aggregates ====================
    
    fun getTotalInflow(startTime: Long, endTime: Long): Flow<Double>
    fun getTotalOutflow(startTime: Long, endTime: Long): Flow<Double>
    fun getNetBalance(startTime: Long, endTime: Long): Flow<Double>
    fun getCategoryTotals(startTime: Long, endTime: Long): Flow<Map<Category, Double>>
    
    // ==================== Statistics ====================
    
    fun getTransactionCount(): Flow<Int>
    fun getUnclassifiedCount(): Flow<Int>
}
