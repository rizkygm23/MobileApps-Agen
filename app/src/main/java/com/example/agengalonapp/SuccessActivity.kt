package com.example.agengalonapp

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success)
        val imageSuccess = findViewById<ImageView>(R.id.imageSuccess)
        val animation = AnimationUtils.loadAnimation(this, R.anim.anim_success_icon)
        imageSuccess.startAnimation(animation)

        val buttonBack = findViewById<Button>(R.id.buttonBackDashboard)
        buttonBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }
    }
}
