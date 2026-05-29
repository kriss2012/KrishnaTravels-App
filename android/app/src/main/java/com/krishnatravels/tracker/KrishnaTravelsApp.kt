package com.krishnatravels.tracker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class KrishnaTravelsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase manually since google-services.json is missing
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyDs0dIlUJ-3eLTOxiA5QpzLEUfZ5BOVraw")
                .setApplicationId("1:934921741539:web:6e9dd157721059d097d1d6")
                .setDatabaseUrl("https://pachora-jalgaon-tracker-default-rtdb.asia-southeast1.firebasedatabase.app")
                .setProjectId("pachora-jalgaon-tracker")
                .build()
            FirebaseApp.initializeApp(this, options)
        }
    }
}
