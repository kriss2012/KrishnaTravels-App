package com.krishnatravels.tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class LoginActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Request all required permissions on first open
        requestAllPermissions()

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginAdminBtn)
        val guestBtn = findViewById<Button>(R.id.loginStudentBtn)
        val progressBar = findViewById<ProgressBar>(R.id.loginProgressBar)

        loginBtn.setOnClickListener {
            val user = usernameInput.text.toString().trim()
            val pass = passwordInput.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            loginBtn.isEnabled = false

            // Mock Login Logic (No Firebase)
            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                loginBtn.isEnabled = true

                if (user == "admin" && pass == "admin123") {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                } else {
                    // Firebase login attempt for drivers
                    com.google.firebase.auth.FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(user, pass)
                        .addOnSuccessListener {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            // Treat any other non-empty login as a driver if Firebase fails (for dev/demo)
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                }
            }, 1000)
        }

        guestBtn.setOnClickListener {
            // Guest access to Route Map
            startActivity(Intent(this, RouteMapActivity::class.java))
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Request background location separately after fine/coarse granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    PERMISSION_REQUEST_CODE + 1
                )
            }
        }
    }
}
