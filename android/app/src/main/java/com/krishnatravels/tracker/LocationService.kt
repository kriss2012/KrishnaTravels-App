package com.krishnatravels.tracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Bundle
import androidx.core.app.NotificationCompat
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LocationService : Service() {

    private var locationManager: LocationManager? = null
    private val CHANNEL_ID = "LocationServiceChannel"
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d("LocationService", "Location updated: ${location.latitude}, ${location.longitude}")
            
            // 1. Send local broadcast for UI
            val intent = Intent("LocationUpdate")
            intent.putExtra("latitude", location.latitude)
            intent.putExtra("longitude", location.longitude)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            // 2. Update Firebase if driver is logged in
            val user = auth.currentUser
            if (user != null && !user.email.isNullOrEmpty() && !user.email!!.contains("admin")) {
                val locationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "speed" to location.speed,
                    "timestamp" to System.currentTimeMillis()
                )
                
                // We need the travelId. For now, we'll try to find it under drivers/uid/travelId
                database.getReference("drivers").child(user.uid).child("travelId").get()
                    .addOnSuccessListener { snapshot ->
                        val travelId = snapshot.value as? String
                        if (travelId != null) {
                            val updates = mapOf(
                                "travels/$travelId/location" to locationData,
                                "travels/$travelId/isOnline" to true,
                                "travels/$travelId/lastSeen" to System.currentTimeMillis()
                            )
                            database.reference.updateChildren(updates)
                        }
                    }
            }
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Krishna Travels")
            .setContentText("Tracking live location...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
        requestLocationUpdates()
        
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        try {
            // Update every 5 seconds (5000 ms) and 0 meters
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                0f,
                locationListener
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                0f,
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Permission missing for location updates", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
