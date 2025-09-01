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

class RegisterActivity : AppCompatActivity() {
    private val auth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        findViewById<CardView>(R.id.registerCard).startAnimation(slideUp)

        findViewById<TextView>(R.id.loginHint).setOnClickListener {
            finish()
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up)
            finishAfterTransition()
        }

        findViewById<MaterialButton>(R.id.btnCreate).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.email)?.text?.toString()?.trim().orEmpty()
            val password = findViewById<TextInputEditText>(R.id.password)?.text?.toString().orEmpty()
            val confirm = findViewById<TextInputEditText>(R.id.confirmPassword)?.text?.toString().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            findViewById<MaterialButton>(R.id.btnCreate).isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    findViewById<MaterialButton>(R.id.btnCreate).isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java),
                            ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up).toBundle())
                        finish()
                    } else {
                        Toast.makeText(this, task.exception?.localizedMessage ?: "Registration failed", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
