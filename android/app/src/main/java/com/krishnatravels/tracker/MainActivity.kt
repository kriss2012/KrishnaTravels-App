package com.krishnatravels.tracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var locationText: TextView
    private lateinit var startTrackingButton: Button
    private lateinit var viewRouteButton: Button
    private lateinit var statusBadge: TextView
    private lateinit var driverNameText: TextView
    private lateinit var routeText: TextView
    private lateinit var speedText: TextView
    private lateinit var coordsText: TextView
    private lateinit var logoutButton: Button

    private var isTracking = false
    private var driverName = ""
    private var travelId = ""
    private var driverRoute = ""

    private val LOCATION_NOTIF_CHANNEL = "driver_location_channel"
    private val NOTIF_ID_LOCATION = 2001
    private val PERM_REQUEST = 300

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lng = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val speed = intent?.getDoubleExtra("speed", 0.0) ?: 0.0

            val speedKmh = (speed * 3.6).toInt()
            coordsText.text = "📍 ${String.format("%.5f", lat)}, ${String.format("%.5f", lng)}"
            speedText.text = "${speedKmh} km/h"
            locationText.text = "Streaming every 5s · Last: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"

            // Show notification when app is in background
            if (!isAppInForeground()) {
                showLocationNotification(lat, lng, speedKmh)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationText = findViewById(R.id.locationText)
        startTrackingButton = findViewById(R.id.startTrackingButton)
        viewRouteButton = findViewById(R.id.viewRouteButton)
        statusBadge = findViewById(R.id.statusBadge)
        driverNameText = findViewById(R.id.driverNameText)
        routeText = findViewById(R.id.driverRouteText)
        speedText = findViewById(R.id.speedText)
        coordsText = findViewById(R.id.coordsText)
        logoutButton = findViewById(R.id.logoutButton)

        createNotificationChannel()
        loadDriverInfo()

        startTrackingButton.setOnClickListener {
            if (!isTracking) {
                checkAndStartTracking()
            } else {
                stopLocationService()
            }
        }

        viewRouteButton.setOnClickListener {
            startActivity(Intent(this, RouteMapActivity::class.java))
        }

        logoutButton.setOnClickListener {
            stopLocationService()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadDriverInfo() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("drivers/$uid").get()
                .addOnSuccessListener { snap ->
                    driverName = snap.child("name").getValue(String::class.java) ?: "Driver"
                    travelId = snap.child("travelId").getValue(String::class.java) ?: ""
                    driverRoute = snap.child("route").getValue(String::class.java) ?: "Pachora ↔ Jalgaon"
                    driverNameText.text = driverName
                    routeText.text = "Route: $driverRoute"
                }
        } else {
            // Fallback for mock login when no Firebase user is present
            driverName = "Guest Driver"
            travelId = "bus_001" // Default for testing
            driverRoute = "Pachora ↔ Jalgaon"
            driverNameText.text = driverName
            routeText.text = "Route: $driverRoute (Mock Mode)"
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(locationReceiver, IntentFilter("LocationUpdate"))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }

    private fun checkAndStartTracking() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERM_REQUEST)
        } else {
            startLocationService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Ask for background location separately
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERM_REQUEST + 1)
                } else {
                    startLocationService()
                }
            } else {
                Toast.makeText(this, "Location permission required for tracking", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == PERM_REQUEST + 1) {
            startLocationService()
        }
    }

    private fun startLocationService() {
        if (travelId.isEmpty()) {
            Toast.makeText(this, "No Travel ID assigned. Contact admin.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(this, LocationService::class.java)
        intent.putExtra("travelId", travelId)
        intent.putExtra("driverName", driverName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isTracking = true
        startTrackingButton.text = "⏹  Go Offline"
        startTrackingButton.setBackgroundResource(R.drawable.bg_button_stop)
        statusBadge.text = "● LIVE"
        statusBadge.setTextColor(0xFF10B981.toInt())
        locationText.text = "Starting GPS stream..."
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        isTracking = false
        startTrackingButton.text = "▶  Go Online"
        startTrackingButton.setBackgroundResource(R.drawable.bg_button_primary)
        statusBadge.text = "● OFFLINE"
        statusBadge.setTextColor(0xFF64748B.toInt())
        locationText.text = "Tracking stopped"
        speedText.text = "— km/h"
        coordsText.text = "No location"
        cancelLocationNotification()
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (process in appProcesses) {
            if (process.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && process.processName == packageName) {
                return true
            }
        }
        return false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LOCATION_NOTIF_CHANNEL,
                "Driver Location Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live location when app is in background"
                setShowBadge(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showLocationNotification(lat: Double, lng: Double, speedKmh: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, LOCATION_NOTIF_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("📍 Live Location Active — $driverName")
            .setContentText("${String.format("%.4f", lat)}, ${String.format("%.4f", lng)} • ${speedKmh} km/h")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Route: $driverRoute\nLat: ${String.format("%.5f", lat)}, Lng: ${String.format("%.5f", lng)}\nSpeed: ${speedKmh} km/h"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIF_ID_LOCATION, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    private fun cancelLocationNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIF_ID_LOCATION)
    }
}
