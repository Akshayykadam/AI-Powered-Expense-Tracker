package com.expense.tracker.ui.screens.settings

import com.expense.tracker.ui.theme.ThemeMode

/**
 * UI state for Settings screen
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val appLockEnabled: Boolean = true,
    val isAiEnabled: Boolean = false,         // User preference: AI on/off
    val isModelDownloaded: Boolean = false,   // Whether model is downloaded
    val transactionCount: Int = 0,
    val appVersion: String = "1.0.0",
    val debugLog: String = ""
)
