package com.easyplan.security

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.easyplan.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * BiometricHelper - Handles device biometric capability checks and preferences.
 *
 * References:
 * https://developer.android.com/training/sign-in/biometric-auth
 */
object BiometricHelper {

    private const val TAG = "BiometricHelper"
    private const val PREFS = "biometric_prefs"
    private const val KEY_ENABLED = "enabled"

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        val available = canAuth == BiometricManager.BIOMETRIC_SUCCESS
        Log.d(TAG, "isBiometricAvailable: $available (result=$canAuth)")
        return available
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        Log.i(TAG, "setEnabled: Biometrics toggled to $enabled")
    }

    fun shouldPromptForBiometrics(context: Context): Boolean {
        val hasUser = Firebase.auth.currentUser != null
        return hasUser && isEnabled(context)
    }

    fun buildPromptInfo(context: Context): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_prompt_title))
            .setSubtitle(context.getString(R.string.biometric_prompt_subtitle))
            .setDescription(context.getString(R.string.biometric_prompt_description))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

    fun createPrompt(
        activity: FragmentActivity,
        onAuthenticated: () -> Unit,
        onError: (CharSequence?) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)
        return BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "onAuthenticationSucceeded: Auth token ${result.authenticationType}")
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.w(TAG, "onAuthenticationError: $errorCode, $errString")
                onError(errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "onAuthenticationFailed: User biometrics rejected")
            }
        })
    }
}
