package com.expense.tracker.di

import com.expense.tracker.data.repository.TransactionRepository
import com.expense.tracker.domain.repository.ITransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-wide dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepository: TransactionRepository
    ): ITransactionRepository
}
