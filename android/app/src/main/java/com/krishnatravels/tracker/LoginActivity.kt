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
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.OnCompleteListener

class LoginActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        
        // Manual Firebase Init (Using config from web frontend)
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyDs0dIlUJ-3eLTOxiA5QpzLEUfZ5BOVraw")
                .setApplicationId("1:934921741539:web:6e9dd157721059d097d1d6")
                .setDatabaseUrl("https://pachora-jalgaon-tracker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .setProjectId("pachora-jalgaon-tracker")
                .build()
            FirebaseApp.initializeApp(this, options)
        }
        
        auth = FirebaseAuth.getInstance()
        
        setContentView(R.layout.activity_login)

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginAdminBtn = findViewById<Button>(R.id.loginAdminBtn)
        val loginStudentBtn = findViewById<Button>(R.id.loginStudentBtn)
        val progressBar = findViewById<ProgressBar>(R.id.loginProgressBar)

        loginAdminBtn.setOnClickListener {
            val email = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            loginAdminBtn.isEnabled = false
            
            // Firebase Auth for Drivers/Admins
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        if (email.contains("admin")) {
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        } else {
                            // Driver logged in - Go to tracking screen
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                        finish()
                    } else {
                        progressBar.visibility = View.GONE
                        loginAdminBtn.isEnabled = true
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginStudentBtn.setOnClickListener {
            // Public/Student access - no login required to view map
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
