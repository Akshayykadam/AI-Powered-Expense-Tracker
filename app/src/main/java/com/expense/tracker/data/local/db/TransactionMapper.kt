package com.expense.tracker.data.local.db

import com.expense.tracker.data.local.db.entities.TransactionEntity
import com.expense.tracker.domain.model.Category
import com.expense.tracker.domain.model.ExpenseType
import com.expense.tracker.domain.model.Transaction
import com.expense.tracker.domain.model.TransactionType

/**
 * Mappers between Room entities and domain models.
 * Keeps the data layer decoupled from domain logic.
 */
object TransactionMapper {
    
    fun entityToDomain(entity: TransactionEntity): Transaction {
        return Transaction(
            id = entity.id,
            timestamp = entity.timestamp,
            amount = entity.amount,
            type = TransactionType.fromString(entity.type),
            source = entity.source,
            merchant = entity.merchant,
            category = Category.fromString(entity.category ?: "OTHER"),
            expenseType = entity.expenseType?.let { ExpenseType.fromString(it) },
            description = entity.description,
            rawSmsHash = entity.rawSmsHash,
            fullBody = entity.fullBody
        )
    }
    
    fun domainToEntity(domain: Transaction): TransactionEntity {
        return TransactionEntity(
            id = domain.id,
            timestamp = domain.timestamp,
            amount = domain.amount,
            type = domain.type.name,
            source = domain.source,
            merchant = domain.merchant,
            category = domain.category.name,
            expenseType = domain.expenseType?.name,
            description = domain.description,
            rawSmsHash = domain.rawSmsHash,
            fullBody = domain.fullBody
        )
    }
    
    fun entitiesToDomain(entities: List<TransactionEntity>): List<Transaction> {
        return entities.map { entityToDomain(it) }
    }
}
