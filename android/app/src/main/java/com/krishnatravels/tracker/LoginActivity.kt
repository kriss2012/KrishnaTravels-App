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

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginAdminBtn = findViewById<Button>(R.id.loginAdminBtn)
        val loginStudentBtn = findViewById<Button>(R.id.loginStudentBtn)
        val progressBar = findViewById<ProgressBar>(R.id.loginProgressBar)

        loginAdminBtn.setOnClickListener {
            val user = usernameInput.text.toString()
            if (user.lowercase() == "admin") {
                progressBar.visibility = View.VISIBLE
                loginAdminBtn.isEnabled = false
                loginStudentBtn.isEnabled = false
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                }, 1200)
            } else {
                Toast.makeText(this, "Admin username must be 'admin'", Toast.LENGTH_SHORT).show()
            }
        }

        loginStudentBtn.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            loginAdminBtn.isEnabled = false
            loginStudentBtn.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }, 1200)
        }
    }
}
