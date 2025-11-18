package com.easyplan

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.easyplan.util.ThemeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * RegisterActivity - User registration screen
 *
 * This activity handles new user registration with Firebase Authentication.
 * Features include:
 * - Email/password account creation
 * - User profile setup (display name)
 * - Firestore user metadata storage
 * - Password confirmation validation
 * - Animated transitions
 *
 * @author EasyPlan Team
 * @version 1.0
 *
 * References:
 * - Firebase Authentication: https://firebase.google.com/docs/auth/android/password-auth
 * - Firestore: https://firebase.google.com/docs/firestore/quickstart
 */
class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    // Firebase Authentication instance
    private val auth by lazy { Firebase.auth }

    /**
     * Called when the activity is first created.
     * Initializes UI components and sets up registration flow.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing RegisterActivity")

        // Apply saved theme preference
        ThemeUtils.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Apply entrance animation
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        findViewById<CardView>(R.id.registerCard).startAnimation(slideUp)
        Log.d(TAG, "onCreate: UI initialized with animations")

        // Setup navigation back to login screen
        findViewById<TextView>(R.id.loginHint).setOnClickListener {
            Log.d(TAG, "Login hint clicked - returning to LoginActivity")
            finish()
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up)
            finishAfterTransition()
        }

        // Setup account creation button
        // Handles user registration with validation
        findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            Log.d(TAG, "Create account button clicked")

            // Collect user input
            val fullName = findViewById<TextInputEditText>(R.id.fullName)?.text?.toString()?.trim().orEmpty()
            val email = findViewById<TextInputEditText>(R.id.email)?.text?.toString()?.trim().orEmpty()
            val password = findViewById<TextInputEditText>(R.id.password)?.text?.toString().orEmpty()
            val confirm = findViewById<TextInputEditText>(R.id.confirmPassword)?.text?.toString().orEmpty()

            // Validate required fields
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Registration attempt with empty fields")
                Toast.makeText(this, "Please enter name, email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate password confirmation
            if (password != confirm) {
                Log.w(TAG, "Password confirmation mismatch")
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Creating account for email: $email")
            val btn = findViewById<MaterialButton>(R.id.btnCreate)
            btn.isEnabled = false // Prevent multiple submissions

            // Create Firebase Authentication account
            // Reference: https://firebase.google.com/docs/auth/android/password-auth#create_a_password-based_account
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Account created successfully for user: ${auth.currentUser?.uid}")

                        // Update user profile with display name
                        val user = auth.currentUser
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        user?.updateProfile(profileUpdates)
                        Log.d(TAG, "User profile updated with display name: $fullName")

                        // Store additional user metadata in Firestore
                        // This allows us to query users and store app-specific data
                        com.google.firebase.ktx.Firebase.firestore
                            .collection("users").document(user?.uid ?: "")
                            .set(mapOf("name" to fullName, "email" to email))
                            .addOnSuccessListener {
                                Log.d(TAG, "User metadata saved to Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to save user metadata", e)
                            }

                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()

                        // Navigate to main app
                        startActivity(Intent(this, MainActivity::class.java),
                            ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up).toBundle())
                        finish()
                    } else {
                        Log.e(TAG, "Registration failed", task.exception)
                        btn.isEnabled = true
                        Toast.makeText(this, task.exception?.localizedMessage ?: "Registration failed", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
