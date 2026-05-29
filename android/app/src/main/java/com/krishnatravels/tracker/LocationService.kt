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

class LocationService : Service() {

    private var locationManager: LocationManager? = null
    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIF_ID = 1
    private var driverName = ""
    private var travelId = ""
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var lastSpeed = 0.0

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLat = location.latitude
            lastLng = location.longitude
            lastSpeed = location.speed.toDouble()

            Log.d("LocationService", "GPS Update: ${lastLat}, ${lastLng}")

            // Sync with Firebase
            if (travelId.isNotEmpty()) {
                val db = com.google.firebase.database.FirebaseDatabase.getInstance()
                val updates = mapOf(
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis(),
                    "location/latitude" to lastLat,
                    "location/longitude" to lastLng,
                    "location/speed" to lastSpeed
                )
                db.getReference("travels/$travelId").updateChildren(updates)
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        driverName = intent?.getStringExtra("driverName") ?: "Driver"
        travelId = intent?.getStringExtra("travelId") ?: ""

        val notification = buildNotification("Starting GPS...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        requestLocationUpdates()
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                5f,
                locationListener
            )
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
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("travels/$travelId/isOnline").setValue(false)
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
