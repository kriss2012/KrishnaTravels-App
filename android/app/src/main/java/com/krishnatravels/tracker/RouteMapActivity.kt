package com.krishnatravels.tracker

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RouteMapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_map)
        
        val timelineContainer = findViewById<LinearLayout>(R.id.timelineContainer)
        
        val routeStops = listOf(
            "Pachora" to true,
            "Goradkheda" to true,
            "Bildhi" to true,
            "Khedgaon" to true,
            "Hadsan" to false,
            "Nandra" to false,
            "Lasgaon" to false,
            "Samner" to false,
            "Pathri" to false,
            "Vadli" to false,
            "Wawadade" to false,
            "Ramdevwadi" to false,
            "Shirsoli" to false,
            "Jain" to false,
            "GH Raisoni" to false,
            "D Mart" to false,
            "Ichadevi" to false,
            "Jalgaon" to false
        )
        
        routeStops.forEachIndexed { index, stop ->
            val isCompleted = stop.second
            
            val stopView = TextView(this).apply {
                text = "${index + 1}. ${stop.first}"
                textSize = 18f
                setPadding(0, 24, 0, 24)
                if (isCompleted) {
                    setTextColor(Color.parseColor("#C7D86A")) // primary
                    setTypeface(null, Typeface.BOLD)
                    text = "$text (Passed)"
                } else {
                    setTextColor(Color.parseColor("#B3B3B3")) // text_secondary
                    text = "$text (Upcoming)"
                }
            }
            timelineContainer.addView(stopView)
        }
    }
}
