package com.expense.tracker.di

import com.expense.tracker.data.repository.TransactionRepository
import com.expense.tracker.domain.repository.ITransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
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

    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            return androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("user_preferences") }
            )
        }
    }
}

// Extension for context to get file
fun android.content.Context.preferencesDataStoreFile(name: String): java.io.File = 
    java.io.File(this.filesDir, "datastore/$name.preferences_pb")
