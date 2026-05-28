package com.krishnatravels.tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        findViewById<Button>(R.id.btnViewMap).setOnClickListener {
            startActivity(Intent(this, RouteMapActivity::class.java))
        }

        findViewById<Button>(R.id.btnSendNotification).setOnClickListener {
            Toast.makeText(this, "Notification broadcast sent to all students!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnAboutApp).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
