package com.easyplan

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.easyplan.util.ThemeUtils
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Add entrance animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        findViewById<CardView>(R.id.loginCard).startAnimation(slideUp)

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_up, R.anim.fade_in)
            startActivity(intent, options.toBundle())
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            // TODO: Validate and proceed to MainActivity after authentication
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up)
            startActivity(intent, options.toBundle())
            finish()
        }
    }
}

