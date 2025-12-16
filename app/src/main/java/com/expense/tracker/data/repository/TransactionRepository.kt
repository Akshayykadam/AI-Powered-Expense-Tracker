package com.expense.tracker.data.repository

import com.expense.tracker.data.local.db.TransactionDao
import com.expense.tracker.data.local.db.TransactionMapper
import com.expense.tracker.domain.model.Category
import com.expense.tracker.domain.model.Transaction
import com.expense.tracker.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of transaction repository.
 * All data stays local - no cloud sync.
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) : ITransactionRepository {
    
    // ==================== CRUD Operations ====================
    
    override suspend fun insertTransaction(transaction: Transaction): Long {
        val entity = TransactionMapper.domainToEntity(transaction)
        return transactionDao.insert(entity)
    }
    
    override suspend fun insertTransactions(transactions: List<Transaction>): List<Long> {
        val entities = transactions.map { TransactionMapper.domainToEntity(it) }
        return transactionDao.insertAll(entities)
    }
    
    override suspend fun updateTransaction(transaction: Transaction) {
        val entity = TransactionMapper.domainToEntity(transaction)
        transactionDao.update(entity)
    }
    
    override suspend fun updateClassification(
        id: Long, 
        category: Category?, 
        merchant: String?, 
        expenseType: String?
    ) {
        transactionDao.updateClassification(id, category?.name, merchant, expenseType)
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) {
        val entity = TransactionMapper.domainToEntity(transaction)
        transactionDao.delete(entity)
    }
    
    override suspend fun deleteAll() {
        transactionDao.deleteAll()
    }
    
    // ==================== Queries ====================
    
    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            TransactionMapper.entitiesToDomain(entities)
        }
    }
    
    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getById(id)?.let { TransactionMapper.entityToDomain(it) }
    }
    
    override suspend fun existsByHash(hash: String): Boolean {
        return transactionDao.existsByHash(hash)
    }
    
    override fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startTime, endTime).map { entities ->
            TransactionMapper.entitiesToDomain(entities)
        }
    }
    
    override fun getTodayTransactions(): Flow<List<Transaction>> {
        val startOfDay = getStartOfDay()
        return transactionDao.getTodayTransactions(startOfDay).map { entities ->
            TransactionMapper.entitiesToDomain(entities)
        }
    }
    
    // ==================== Aggregates ====================
    
    override fun getTotalInflow(startTime: Long, endTime: Long): Flow<Double> {
        return transactionDao.getTotalInflow(startTime, endTime)
    }
    
    override fun getTotalOutflow(startTime: Long, endTime: Long): Flow<Double> {
        return transactionDao.getTotalOutflow(startTime, endTime)
    }
    
    override fun getNetBalance(startTime: Long, endTime: Long): Flow<Double> {
        return transactionDao.getNetBalance(startTime, endTime)
    }
    
    override fun getCategoryTotals(startTime: Long, endTime: Long): Flow<Map<Category, Double>> {
        return transactionDao.getCategoryTotals(startTime, endTime).map { totals ->
            totals.associate { categoryTotal ->
                Category.fromString(categoryTotal.category ?: "OTHER") to categoryTotal.total
            }
        }
    }
    
    // ==================== Statistics ====================
    
    override fun getTransactionCount(): Flow<Int> {
        return transactionDao.getTransactionCount()
    }
    
    override fun getUnclassifiedCount(): Flow<Int> {
        return transactionDao.getUnclassifiedCount()
    }
    
    // ==================== Helpers ====================
    
    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
