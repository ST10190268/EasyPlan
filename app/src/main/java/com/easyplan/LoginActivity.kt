package com.easyplan

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.easyplan.security.BiometricHelper
import com.easyplan.util.ThemeUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * LoginActivity - Entry point for user authentication
 *
 * This activity handles user login functionality using Firebase Authentication.
 * Features include:
 * - Email/password authentication
 * - Google Sign-In (SSO) for quick access
 * - Password reset functionality
 * - Navigation to registration screen
 * - Animated transitions for better UX
 *
 * @author EasyPlan Team
 * @version 2.0 - Added Google Sign-In SSO
 *
 * References:
 * - Firebase Authentication: https://firebase.google.com/docs/auth/android/start
 * - Google Sign-In: https://developers.google.com/identity/sign-in/android/start
 * - Material Design Components: https://material.io/develop/android
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    // Firebase Authentication instance - initialized lazily for performance
    private val auth by lazy { Firebase.auth }

    // Google Sign-In client
    private lateinit var googleSignInClient: GoogleSignInClient

    // Activity result launcher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Google Sign-In result received with code: ${result.resultCode}")
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google Sign-In successful, account: ${account.email}")
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed", e)
            val reason = e.localizedMessage ?: ""
            Toast.makeText(
                this,
                getString(R.string.error_google_signin_detail, reason),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Called when the activity is first created.
     * Sets up the UI, applies theme, and initializes click listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing LoginActivity")

        // Apply user's saved theme preference (Light/Dark/System)
        ThemeUtils.applySavedTheme(this)

        // Enable edge-to-edge display for modern Android UI
        enableEdgeToEdge()

        // Check if user is already logged in - if so, skip login UI setup
        // onStart() will handle biometric prompt or direct navigation
        if (auth.currentUser != null) {
            Log.d(TAG, "onCreate: User already logged in (${auth.currentUser?.uid}), skipping UI setup")
            setContentView(R.layout.activity_login) // Still need to set content view for onStart()
            return
        }

        setContentView(R.layout.activity_login)

        // Configure Google Sign-In
        // Reference: https://developers.google.com/identity/sign-in/android/start-integrating
        // Web Client ID is stored in strings.xml for better maintainability and security
        val webClientId = getString(R.string.google_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        Log.d(TAG, "onCreate: Google Sign-In client configured with Web Client ID from resources")

        // Apply slide-up animation to login card for smooth entrance
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        findViewById<CardView>(R.id.loginCard).startAnimation(slideUp)
        Log.d(TAG, "onCreate: UI initialized with animations")

        // Setup Google Sign-In button (if exists in layout)
        findViewById<MaterialButton>(R.id.btnGoogleSignIn)?.setOnClickListener {
            Log.d(TAG, "Google Sign-In button clicked")
            signInWithGoogle()
        }

        // Setup navigation to registration screen
        findViewById<TextView>(R.id.registerLink).setOnClickListener {
            Log.d(TAG, "Register link clicked - navigating to RegisterActivity")
            val intent = Intent(this, RegisterActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_up, R.anim.fade_in)
            startActivity(intent, options.toBundle())
        }

        // Setup forgot password functionality
        // Allows users to reset their password via email
        findViewById<TextView>(R.id.forgotPassword)?.setOnClickListener {
            Log.d(TAG, "Forgot password clicked")
            val email = findViewById<TextInputEditText>(R.id.email)?.text?.toString()?.trim().orEmpty()

            if (email.isEmpty()) {
                Log.w(TAG, "Forgot password: Email field is empty")
                Toast.makeText(this, getString(R.string.error_email_required_first), Toast.LENGTH_SHORT).show()
            } else {
                Log.d(TAG, "Sending password reset email to: $email")
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Log.i(TAG, "Password reset email sent successfully to: $email")
                        Toast.makeText(this, getString(R.string.success_password_reset), Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to send password reset email", exception)
                        Toast.makeText(
                            this,
                            exception.localizedMessage ?: getString(R.string.error_password_reset_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        // Setup login button click listener
        // Handles email/password authentication via Firebase
        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            Log.d(TAG, "Login button clicked")
            val email = findViewById<TextInputEditText>(R.id.email)?.text?.toString()?.trim().orEmpty()
            val password = findViewById<TextInputEditText>(R.id.password)?.text?.toString().orEmpty()

            // Validate input fields
            if (email.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Login attempt with empty credentials")
                Toast.makeText(this, getString(R.string.error_login_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Attempting login for email: $email")
            // Disable button to prevent multiple clicks during authentication
            findViewById<MaterialButton>(R.id.btnLogin).isEnabled = false

            // Authenticate user with Firebase
            // Reference: https://firebase.google.com/docs/auth/android/password-auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    // Re-enable button after authentication attempt
                    findViewById<MaterialButton>(R.id.btnLogin).isEnabled = true

                    if (task.isSuccessful) {
                        Log.i(TAG, "Login successful for user: ${auth.currentUser?.uid}")
                        val name = auth.currentUser?.displayName
                        if (!name.isNullOrEmpty()) {
                            Toast.makeText(this, getString(R.string.success_login_user, name), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, getString(R.string.success_login), Toast.LENGTH_SHORT).show()
                        }

                        // Auto-enable biometrics if available and not already enabled
                        if (BiometricHelper.isBiometricAvailable(this) && !BiometricHelper.isEnabled(this)) {
                            BiometricHelper.setEnabled(this, true)
                            Log.i(TAG, "Auto-enabled biometrics for user: ${auth.currentUser?.uid}")
                        }

                        navigateToMain()
                    } else {
                        Log.e(TAG, "Login failed", task.exception)
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage ?: getString(R.string.error_login_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        maybeUnlockWithBiometrics()
    }

    /**
     * Initiates Google Sign-In flow
     * Launches the Google Sign-In intent
     */
    private fun signInWithGoogle() {
        Log.d(TAG, "signInWithGoogle: Launching Google Sign-In intent")
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    /**
     * Authenticates with Firebase using Google credentials
     *
     * @param account The Google account to authenticate with
     *
     * Reference: https://firebase.google.com/docs/auth/android/google-signin
     */
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle: Authenticating with Firebase")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "firebaseAuthWithGoogle: Sign-In successful")
                    val user = auth.currentUser
                    val name = user?.displayName ?: ""
                    Toast.makeText(this, getString(R.string.success_login_user, name), Toast.LENGTH_SHORT).show()

                    // Auto-enable biometrics if available and not already enabled
                    if (BiometricHelper.isBiometricAvailable(this) && !BiometricHelper.isEnabled(this)) {
                        BiometricHelper.setEnabled(this, true)
                        Log.i(TAG, "Auto-enabled biometrics for user: ${auth.currentUser?.uid}")
                    }

                    navigateToMain()
                } else {
                    Log.e(TAG, "firebaseAuthWithGoogle: Sign-In failed", task.exception)
                    val reason = task.exception?.localizedMessage ?: ""
                    Toast.makeText(
                        this,
                        getString(R.string.error_google_signin_detail, reason),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up)
        startActivity(intent, options.toBundle())
        finish()
    }

    private fun maybeUnlockWithBiometrics() {
        val currentUser = auth.currentUser ?: return
        if (!BiometricHelper.shouldPromptForBiometrics(this)) {
            Log.d(TAG, "maybeUnlockWithBiometrics: Biometrics disabled, continuing as ${currentUser.uid}")
            navigateToMain()
            return
        }

        if (!BiometricHelper.isBiometricAvailable(this)) {
            Log.w(TAG, "maybeUnlockWithBiometrics: Hardware unavailable, disabling preference")
            BiometricHelper.setEnabled(this, false)
            Toast.makeText(this, getString(R.string.biometric_not_available), Toast.LENGTH_LONG).show()
            navigateToMain()
            return
        }

        val prompt = BiometricHelper.createPrompt(this, {
            Log.d(TAG, "maybeUnlockWithBiometrics: Authenticated for ${currentUser.uid}")
            navigateToMain()
        }) { error ->
            Log.e(TAG, "maybeUnlockWithBiometrics: Failed ${error ?: "unknown error"}")
            Toast.makeText(
                this,
                error ?: getString(R.string.biometric_enrollment_required),
                Toast.LENGTH_LONG
            ).show()
        }
        prompt.authenticate(BiometricHelper.buildPromptInfo(this))
    }
}
