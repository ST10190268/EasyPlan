package com.easyplan

import android.app.ActivityOptions
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.easyplan.util.ThemeUtils

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applySavedTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Add entrance animation
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        findViewById<CardView>(R.id.registerCard).startAnimation(slideUp)

        findViewById<TextView>(R.id.loginHint).setOnClickListener {
            finish() // Go back to login
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.slide_up)
            finishAfterTransition()
        }
    }
}

