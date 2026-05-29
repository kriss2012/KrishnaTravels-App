package com.krishnatravels.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class LocationService : Service() {

    private var locationManager: LocationManager? = null
    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIF_ID = 1
    private var travelId = ""
    private var driverName = ""
    private var db = FirebaseDatabase.getInstance()
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastSpeed = 0.0

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLat = location.latitude
            lastLng = location.longitude
            lastSpeed = location.speed.toDouble()

            Log.d("LocationService", "GPS: ${lastLat}, ${lastLng} @ ${(lastSpeed * 3.6).toInt()} km/h")

            // Push to Firebase
            if (travelId.isNotEmpty()) {
                val locationData = mapOf(
                    "latitude" to lastLat,
                    "longitude" to lastLng,
                    "speed" to lastSpeed,
                    "timestamp" to ServerValue.TIMESTAMP
                )
                db.getReference("travels/$travelId").updateChildren(
                    mapOf(
                        "isOnline" to true,
                        "location" to locationData,
                        "lastSeen" to ServerValue.TIMESTAMP
                    )
                )
            }

            // Broadcast to UI
            val intent = Intent("LocationUpdate").apply {
                putExtra("latitude", lastLat)
                putExtra("longitude", lastLng)
                putExtra("speed", lastSpeed)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            // Update foreground notification
            updateForegroundNotification()
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            Log.w("LocationService", "Provider disabled: $provider")
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        db = FirebaseDatabase.getInstance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        travelId = intent?.getStringExtra("travelId") ?: ""
        driverName = intent?.getStringExtra("driverName") ?: "Driver"

        val notification = buildNotification("Starting GPS...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        // Set onDisconnect handler so Firebase auto-marks offline if connection drops
        if (travelId.isNotEmpty()) {
            db.getReference("travels/$travelId").onDisconnect().updateChildren(
                mapOf("isOnline" to false, "lastSeen" to ServerValue.TIMESTAMP)
            )
            db.getReference("travels/$travelId").updateChildren(
                mapOf("isOnline" to true, "driverName" to driverName)
            )
        }

        requestLocationUpdates()
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        try {
            // Use GPS for primary high-accuracy updates every 5 seconds / 5 meters
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                5f,
                locationListener
            )
            // Network as fallback
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                5f,
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission missing", e)
        }
    }

    private fun buildNotification(statusText: String): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚌 Krishna Travels — $driverName")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateForegroundNotification() {
        val speedKmh = (lastSpeed * 3.6).toInt()
        val statusText = "${String.format("%.4f", lastLat)}, ${String.format("%.4f", lastLng)} • ${speedKmh} km/h"
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener)
        if (travelId.isNotEmpty()) {
            db.getReference("travels/$travelId").updateChildren(
                mapOf("isOnline" to false, "lastSeen" to ServerValue.TIMESTAMP)
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live GPS tracking for drivers"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
