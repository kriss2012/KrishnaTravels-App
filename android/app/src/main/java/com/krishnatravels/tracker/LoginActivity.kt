package com.krishnatravels.tracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginAdminBtn = findViewById<Button>(R.id.loginAdminBtn)
        val loginStudentBtn = findViewById<Button>(R.id.loginStudentBtn)
        val progressBar = findViewById<ProgressBar>(R.id.loginProgressBar)

        loginAdminBtn.setOnClickListener {
            val user = usernameInput.text.toString().trim()
            val pass = passwordInput.text.toString().trim()

            if (user == "admin" && pass == "admin123") {
                progressBar.visibility = View.VISIBLE
                loginAdminBtn.isEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                }, 1000)
            } else if (user.isNotEmpty() && pass.isNotEmpty()) {
                // Generic Driver Login for now
                progressBar.visibility = View.VISIBLE
                loginAdminBtn.isEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, 1000)
            } else {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
            }
        }

        loginStudentBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
