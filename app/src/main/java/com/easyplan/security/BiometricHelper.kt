package com.easyplan.security

import android.content.Context
import android.os.Build
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
    private const val KEY_ENABLED_USER = "enabled_user_id"

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        val canAuth = biometricManager.canAuthenticate(authenticators)
        val available = canAuth == BiometricManager.BIOMETRIC_SUCCESS
        Log.d(TAG, "isBiometricAvailable: $available (result=$canAuth)")
        return available
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = isEnabledForCurrentUser(context)

    fun setEnabled(context: Context, enabled: Boolean) {
        val editor = prefs(context).edit().putBoolean(KEY_ENABLED, enabled)
        if (enabled) {
            val uid = Firebase.auth.currentUser?.uid
            if (!uid.isNullOrEmpty()) {
                editor.putString(KEY_ENABLED_USER, uid)
            }
        } else {
            editor.remove(KEY_ENABLED_USER)
        }
        editor.apply()
        Log.i(TAG, "setEnabled: Biometrics toggled to $enabled")
    }

    private fun isEnabledForCurrentUser(context: Context): Boolean {
        val currentUid = Firebase.auth.currentUser?.uid
        val savedUid = prefs(context).getString(KEY_ENABLED_USER, null)
        val enabled = prefs(context).getBoolean(KEY_ENABLED, false)
        return enabled && !savedUid.isNullOrEmpty() && savedUid == currentUid
    }

    fun shouldPromptForBiometrics(context: Context): Boolean {
        val hasUser = Firebase.auth.currentUser != null
        val enabledForUser = isEnabledForCurrentUser(context)
        Log.d(TAG, "shouldPromptForBiometrics: hasUser=$hasUser enabledForUser=$enabledForUser")
        return hasUser && enabledForUser
    }

    fun buildPromptInfo(context: Context): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_prompt_title))
            .setSubtitle(context.getString(R.string.biometric_prompt_subtitle))
            .setDescription(context.getString(R.string.biometric_prompt_description))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            builder.setNegativeButtonText(context.getString(R.string.cancel))
        }

        return builder.build()
    }

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
