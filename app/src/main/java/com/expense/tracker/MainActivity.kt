package com.expense.tracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.expense.tracker.security.AuthState
import com.expense.tracker.security.BiometricAuthManager
import com.expense.tracker.ui.navigation.AppNavigation
import com.expense.tracker.ui.screens.lock.LockScreen
import com.expense.tracker.ui.screens.onboarding.OnboardingScreen
import com.expense.tracker.ui.screens.settings.SettingsViewModel
import com.expense.tracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore for app preferences
private val android.content.Context.dataStore by preferencesDataStore(name = "app_preferences")
private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    private lateinit var biometricAuthManager: BiometricAuthManager
    
    // Auto-lock timeout in milliseconds (30 seconds)
    private val autoLockTimeoutMs = 30_000L
    private var lastBackgroundTime: Long = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        biometricAuthManager = BiometricAuthManager(this)
        
        setContent {
            // Get theme settings from SettingsViewModel
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val appLockEnabled by settingsViewModel.appLockEnabled.collectAsState()
            
            // Auth state
            val authState by biometricAuthManager.authState.collectAsState()
            
            // Onboarding state
            var hasCompletedOnboarding by remember { mutableStateOf<Boolean?>(null) }
            val coroutineScope = rememberCoroutineScope()
            
            // Load onboarding state from DataStore
            LaunchedEffect(Unit) {
                hasCompletedOnboarding = dataStore.data.map { preferences ->
                    preferences[ONBOARDING_COMPLETED] ?: false
                }.first()
            }
            
            // Handle lifecycle for auto-lock
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, appLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (appLockEnabled) {
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                lastBackgroundTime = System.currentTimeMillis()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                val timeSinceBackground = System.currentTimeMillis() - lastBackgroundTime
                                if (lastBackgroundTime > 0 && timeSinceBackground > autoLockTimeoutMs) {
                                    biometricAuthManager.lock()
                                }
                            }
                            else -> {}
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            // Auto-trigger auth on first launch if lock is enabled
            LaunchedEffect(appLockEnabled, hasCompletedOnboarding) {
                if (hasCompletedOnboarding == true && appLockEnabled && biometricAuthManager.canAuthenticate()) {
                    biometricAuthManager.authenticate(this@MainActivity) { }
                }
            }
            
            ExpenseTrackerTheme(
                themeMode = themeMode
            ) {
                // Wait for onboarding state to load
                when (hasCompletedOnboarding) {
                    null -> {
                        // Loading state - show nothing or splash
                    }
                    false -> {
                        // Show onboarding
                        OnboardingScreen(
                            onComplete = {
                                coroutineScope.launch {
                                    dataStore.edit { preferences ->
                                        preferences[ONBOARDING_COMPLETED] = true
                                    }
                                    hasCompletedOnboarding = true
                                }
                            }
                        )
                    }
                    true -> {
                        // Show lock screen or main content
                        val isLocked = appLockEnabled && authState !is AuthState.Unlocked
                        
                        AnimatedContent(
                            targetState = isLocked,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "lock_transition"
                        ) { showLock ->
                            if (showLock) {
                                LockScreen(
                                    authState = authState,
                                    onAuthenticate = {
                                        biometricAuthManager.authenticate(this@MainActivity) { }
                                    },
                                    onRetry = {
                                        biometricAuthManager.clearError()
                                        biometricAuthManager.authenticate(this@MainActivity) { }
                                    }
                                )
                            } else {
                                AppNavigation()
                            }
                        }
                    }
                }
            }
        }
    }
}
