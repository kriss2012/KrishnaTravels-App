package com.krishnatravels.tracker

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RouteMapActivity : AppCompatActivity() {

    private lateinit var mapWebView: WebView
    private lateinit var timelineContainer: LinearLayout
    private lateinit var busInfoText: TextView
    private var travelsListener: ValueEventListener? = null

    private val ROUTE_STOPS = listOf(
        "Pachora" to Pair(20.6681, 75.3567),
        "Goradkheda" to Pair(20.6881, 75.3681),
        "Bildhi" to Pair(20.7081, 75.3800),
        "Khedgaon" to Pair(20.7281, 75.3920),
        "Hadsan" to Pair(20.7481, 75.4050),
        "Nandra" to Pair(20.7681, 75.4180),
        "Lasgaon" to Pair(20.7881, 75.4310),
        "Samner" to Pair(20.8081, 75.4440),
        "Pathri" to Pair(20.8281, 75.4570),
        "Vadli" to Pair(20.8481, 75.4700),
        "Wawadade" to Pair(20.8681, 75.4830),
        "Ramdevwadi" to Pair(20.8881, 75.4960),
        "Shirsoli" to Pair(20.9081, 75.5090),
        "Jain College" to Pair(20.9281, 75.5220),
        "GH Raisoni" to Pair(20.9481, 75.5350),
        "D Mart" to Pair(20.9681, 75.5480),
        "Ichadevi" to Pair(20.9881, 75.5550),
        "Jalgaon" to Pair(21.0077, 75.5626)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_map)

        mapWebView = findViewById(R.id.mapWebView)
        timelineContainer = findViewById(R.id.timelineContainer)
        busInfoText = findViewById(R.id.busInfoText)

        setupWebView()
        loadInitialMap()
        listenToLiveDrivers()
    }

    private fun setupWebView() {
        mapWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        mapWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
        }
    }

    private fun buildMapHtml(driverLat: Double = 0.0, driverLng: Double = 0.0, speedKmh: Int = 0, busName: String = ""): String {
        val stopsJs = ROUTE_STOPS.joinToString(",\n") { stop ->
            """{ name: "${stop.first}", lat: ${stop.second.first}, lng: ${stop.second.second} }"""
        }
        val hasBus = driverLat != 0.0 && driverLng != 0.0
        val busMarkerJs = if (hasBus) {
            """
            var busIcon = L.divIcon({
                html: '<div style="font-size:28px;line-height:1;">🚌</div>',
                iconAnchor: [14, 28],
                className: ''
            });
            var busMarker = L.marker([$driverLat, $driverLng], {icon: busIcon})
                .addTo(map)
                .bindPopup('<b>$busName</b><br>${speedKmh} km/h')
                .openPopup();
            map.setView([$driverLat, $driverLng], 13);
            """
        } else ""

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
  body { margin: 0; padding: 0; background: #000; }
  #map { height: 100vh; width: 100%; }
  .leaflet-container { background: #1a1a1a; }
</style>
</head>
<body>
<div id="map"></div>
<script>
  var map = L.map('map', {zoomControl: true}).setView([20.75, 75.45], 11);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap',
    maxZoom: 19
  }).addTo(map);

  // Draw route stops
  var stops = [$stopsJs];
  var latlngs = stops.map(function(s) { return [s.lat, s.lng]; });
  
  // Draw polyline
  L.polyline(latlngs, {color: '#C7D86A', weight: 4, opacity: 0.8}).addTo(map);
  
  // Add stop markers
  stops.forEach(function(stop, i) {
    var isTerminal = i === 0 || i === stops.length - 1;
    var circle = L.circleMarker([stop.lat, stop.lng], {
      radius: isTerminal ? 10 : 6,
      fillColor: isTerminal ? '#C7D86A' : '#A8B58A',
      color: '#000',
      weight: 2,
      opacity: 1,
      fillOpacity: 0.9
    }).addTo(map).bindPopup('<b>' + stop.name + '</b>');
  });
  
  $busMarkerJs
</script>
</body>
</html>
        """.trimIndent()
    }

    private fun loadInitialMap() {
        mapWebView.loadDataWithBaseURL(
            "https://unpkg.com",
            buildMapHtml(),
            "text/html",
            "UTF-8",
            null
        )
        renderTimeline(null, 0.0)
    }

    private fun listenToLiveDrivers() {
        val travelsRef = FirebaseDatabase.getInstance().getReference("travels")
        travelsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeBus: DataSnapshot? = null
                var activeLat = 0.0
                var activeLng = 0.0
                var activeSpeed = 0.0
                var busName = ""
                var onlineCount = 0

                for (travelSnap in snapshot.children) {
                    val isOnline = travelSnap.child("isOnline").getValue(Boolean::class.java) ?: false
                    if (isOnline) {
                        onlineCount++
                        val lat = travelSnap.child("location/latitude").getValue(Double::class.java) ?: 0.0
                        val lng = travelSnap.child("location/longitude").getValue(Double::class.java) ?: 0.0
                        val speed = travelSnap.child("location/speed").getValue(Double::class.java) ?: 0.0
                        if (lat != 0.0 && lng != 0.0) {
                            activeBus = travelSnap
                            activeLat = lat
                            activeLng = lng
                            activeSpeed = speed
                            busName = travelSnap.child("travelName").getValue(String::class.java)
                                ?: travelSnap.child("driverName").getValue(String::class.java) ?: "Bus"
                        }
                    }
                }

                val speedKmh = (activeSpeed * 3.6).toInt()
                if (onlineCount > 0) {
                    busInfoText.text = "🟢 $onlineCount bus(es) live now  •  $busName  •  $speedKmh km/h"
                    busInfoText.setTextColor(Color.parseColor("#10B981"))
                } else {
                    busInfoText.text = "📡 No buses online right now"
                    busInfoText.setTextColor(Color.parseColor("#64748B"))
                }

                mapWebView.loadDataWithBaseURL(
                    "https://unpkg.com",
                    buildMapHtml(activeLat, activeLng, speedKmh, busName),
                    "text/html", "UTF-8", null
                )
                renderTimeline(activeBus, activeLat)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        travelsRef.addValueEventListener(travelsListener!!)
    }

    private fun renderTimeline(activeBus: DataSnapshot?, busLat: Double) {
        timelineContainer.removeAllViews()
        for ((index, stop) in ROUTE_STOPS.withIndex()) {
            val stopLat = stop.second.first
            val isPassed = busLat > 0 && busLat >= stopLat - 0.001
            val isNearest = busLat > 0 && findNearestStopIndex(busLat) == index
            val isTerminal = index == 0 || index == ROUTE_STOPS.size - 1

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            // Dot
            val dot = android.view.View(this).apply {
                val dotSz = if (isNearest || isTerminal) 22 else 14
                val lp = LinearLayout.LayoutParams(dotSz, dotSz)
                lp.setMargins(0, 0, 20, 0)
                layoutParams = lp
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(when {
                        isNearest -> Color.parseColor("#C7D86A")
                        isPassed -> Color.parseColor("#A8B58A")
                        else -> Color.parseColor("#374151")
                    })
                    if (isNearest) setStroke(4, Color.parseColor("#E0FF7A"))
                }
            }

            val nameText = TextView(this).apply {
                text = stop.first
                textSize = if (isNearest) 16f else 13f
                setTextColor(when {
                    isNearest -> Color.parseColor("#C7D86A")
                    isPassed -> Color.parseColor("#A8B58A")
                    else -> Color.parseColor("#64748B")
                })
                if (isNearest || isTerminal) setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val statusText = TextView(this).apply {
                text = when {
                    isNearest -> "🚌 HERE"
                    isPassed -> "✓"
                    else -> ""
                }
                textSize = 12f
                setTextColor(Color.parseColor("#C7D86A"))
                setTypeface(null, Typeface.BOLD)
            }

            row.addView(dot)
            row.addView(nameText)
            row.addView(statusText)
            timelineContainer.addView(row)
        }
    }

    private fun findNearestStopIndex(busLat: Double): Int {
        var minDist = Double.MAX_VALUE
        var nearestIndex = 0
        ROUTE_STOPS.forEachIndexed { i, stop ->
            val dist = Math.abs(busLat - stop.second.first)
            if (dist < minDist) {
                minDist = dist
                nearestIndex = i
            }
        }
        return nearestIndex
    }

    override fun onDestroy() {
        super.onDestroy()
        travelsListener?.let {
            FirebaseDatabase.getInstance().getReference("travels").removeEventListener(it)
        }
        mapWebView.destroy()
    }
}
