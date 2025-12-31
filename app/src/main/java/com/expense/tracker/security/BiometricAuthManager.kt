package com.expense.tracker.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication state for the app lock
 */
sealed class AuthState {
    object Locked : AuthState()
    object Unlocked : AuthState()
    object Authenticating : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Manager for biometric/device credential authentication.
 * Uses Android's BiometricPrompt API for system-level security.
 */
class BiometricAuthManager(private val context: Context) {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Locked)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val biometricManager = BiometricManager.from(context)
    
    // Allow fingerprint, face, or device PIN/pattern
    private val allowedAuthenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    
    /**
     * Check if device has any form of authentication available
     */
    fun canAuthenticate(): Boolean {
        return when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Device has capability but nothing enrolled
                // Still allow device credential
                biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            }
            else -> false
        }
    }
    
    /**
     * Check what types of authentication are available
     */
    fun getAvailableAuthTypes(): AuthTypes {
        val hasBiometric = biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == 
            BiometricManager.BIOMETRIC_SUCCESS
        val hasDeviceCredential = biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == 
            BiometricManager.BIOMETRIC_SUCCESS
        
        return AuthTypes(
            hasBiometric = hasBiometric,
            hasDeviceCredential = hasDeviceCredential
        )
    }
    
    /**
     * Show authentication prompt
     */
    fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        if (!canAuthenticate()) {
            // No authentication available, auto-unlock
            _authState.value = AuthState.Unlocked
            onResult(true)
            return
        }
        
        _authState.value = AuthState.Authenticating
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _authState.value = AuthState.Unlocked
                onResult(true)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // User cancelled or too many attempts
                _authState.value = AuthState.Error(errString.toString())
                onResult(false)
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Single attempt failed, but user can retry
                // Don't change state - let them retry
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock TxnSense")
            .setSubtitle("Verify your identity to access your financial data")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Lock the app (called on background transition)
     */
    fun lock() {
        _authState.value = AuthState.Locked
    }
    
    /**
     * Check if currently locked
     */
    fun isLocked(): Boolean = _authState.value is AuthState.Locked
    
    /**
     * Clear error and return to locked state
     */
    fun clearError() {
        _authState.value = AuthState.Locked
    }
}

/**
 * Available authentication types on this device
 */
data class AuthTypes(
    val hasBiometric: Boolean,
    val hasDeviceCredential: Boolean
) {
    val hasAny: Boolean get() = hasBiometric || hasDeviceCredential
}
