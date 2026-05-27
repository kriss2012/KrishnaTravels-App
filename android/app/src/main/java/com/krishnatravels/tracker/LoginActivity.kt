package com.krishnatravels.tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginAdminBtn = findViewById<Button>(R.id.loginAdminBtn)
        val loginStudentBtn = findViewById<Button>(R.id.loginStudentBtn)

        loginAdminBtn.setOnClickListener {
            val user = usernameInput.text.toString()
            if (user.lowercase() == "admin") {
                startActivity(Intent(this, AdminDashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Admin username must be 'admin'", Toast.LENGTH_SHORT).show()
            }
        }

        loginStudentBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
