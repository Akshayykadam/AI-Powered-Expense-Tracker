package com.expense.tracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for the Expense Tracker.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class ExpenseTrackerApp : Application()
