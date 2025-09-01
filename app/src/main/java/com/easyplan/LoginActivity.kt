package com.easyplan

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private val auth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        findViewById<CardView>(R.id.loginCard).startAnimation(slideUp)

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_up, R.anim.fade_in)
            startActivity(intent, options.toBundle())
        }

        // Forgot password flow
        findViewById<TextView>(R.id.forgotPassword)?.setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.email)?.text?.toString()?.trim().orEmpty()
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email first", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password reset email sent", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, it.localizedMessage ?: "Failed to send reset email", Toast.LENGTH_LONG).show()
                    }
            }
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.email)?.text?.toString()?.trim().orEmpty()
            val password = findViewById<TextInputEditText>(R.id.password)?.text?.toString().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            findViewById<MaterialButton>(R.id.btnLogin).isEnabled = false
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    findViewById<MaterialButton>(R.id.btnLogin).isEnabled = true
                    if (task.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java)
                        val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up)
                        startActivity(intent, options.toBundle())
                        finish()
                    } else {
                        Toast.makeText(this, task.exception?.localizedMessage ?: "Login failed", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
