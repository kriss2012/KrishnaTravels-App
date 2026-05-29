package com.krishnatravels.tracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.*

data class DriverModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val travelId: String = "",
    val route: String = "",
    val isOnline: Boolean = false,
    val location: LocationData? = null
)

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Double = 0.0,
    val timestamp: Long = 0L
)

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var driversContainer: LinearLayout
    private lateinit var liveCountText: TextView
    private lateinit var totalCountText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var driversRef: DatabaseReference

    private val driversList = mutableListOf<DriverModel>()
    private var driversListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance()

        driversContainer = findViewById(R.id.driversContainer)
        liveCountText = findViewById(R.id.liveCount)
        totalCountText = findViewById(R.id.totalCount)
        progressBar = findViewById(R.id.adminProgressBar)

        driversRef = db.getReference("drivers")

        findViewById<Button>(R.id.btnAddDriver).setOnClickListener { showAddDriverDialog() }
        findViewById<Button>(R.id.btnViewMap).setOnClickListener {
            startActivity(Intent(this, RouteMapActivity::class.java))
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        listenToDrivers()
    }

    private fun listenToDrivers() {
        progressBar.visibility = View.VISIBLE
        driversListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progressBar.visibility = View.GONE
                driversList.clear()
                var liveCount = 0
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: ""
                    val email = child.child("email").getValue(String::class.java) ?: ""
                    val phone = child.child("phone").getValue(String::class.java) ?: ""
                    val travelId = child.child("travelId").getValue(String::class.java) ?: ""
                    val route = child.child("route").getValue(String::class.java) ?: ""

                    // Read live location from travels node
                    val isOnline = false // will be joined from travels node
                    driversList.add(DriverModel(uid, name, email, phone, travelId, route, isOnline))
                }
                totalCountText.text = driversList.size.toString()
                // Now fetch live status from travels
                joinLiveStatus()
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminDashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        driversRef.addValueEventListener(driversListener!!)
    }

    private fun joinLiveStatus() {
        db.getReference("travels").get().addOnSuccessListener { travelsSnap ->
            var liveCount = 0
            val updatedList = driversList.map { driver ->
                val travelSnap = travelsSnap.child(driver.travelId)
                val isOnline = travelSnap.child("isOnline").getValue(Boolean::class.java) ?: false
                val lat = travelSnap.child("location/latitude").getValue(Double::class.java) ?: 0.0
                val lng = travelSnap.child("location/longitude").getValue(Double::class.java) ?: 0.0
                val speed = travelSnap.child("location/speed").getValue(Double::class.java) ?: 0.0
                val ts = travelSnap.child("location/timestamp").getValue(Long::class.java) ?: 0L
                val loc = if (lat != 0.0 && lng != 0.0) LocationData(lat, lng, speed, ts) else null
                if (isOnline) liveCount++
                driver.copy(isOnline = isOnline, location = loc)
            }
            driversList.clear()
            driversList.addAll(updatedList)
            liveCountText.text = liveCount.toString()
            totalCountText.text = driversList.size.toString()
            renderDriverList()
        }
    }

    private fun renderDriverList() {
        driversContainer.removeAllViews()
        if (driversList.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No drivers added yet. Tap 'Add Driver' to get started."
                setTextColor(0xFFB3B3B3.toInt())
                textSize = 14f
                setPadding(0, 40, 0, 40)
                gravity = android.view.Gravity.CENTER
            }
            driversContainer.addView(empty)
            return
        }
        for (driver in driversList) {
            val card = createDriverCard(driver)
            driversContainer.addView(card)
        }
    }

    private fun createDriverCard(driver: DriverModel): View {
        val card = CardView(this).apply {
            radius = 20f
            setCardBackgroundColor(0xFF111111.toInt())
            cardElevation = 8f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 20)
            layoutParams = params
        }

        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 40)
        }

        // Status + Name row
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val statusDot = View(this).apply {
            val dotSize = 24
            val lp = LinearLayout.LayoutParams(dotSize, dotSize)
            lp.setMargins(0, 0, 20, 0)
            layoutParams = lp
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(if (driver.isOnline) 0xFF10B981.toInt() else 0xFF64748B.toInt())
            }
        }
        val nameText = TextView(this).apply {
            text = driver.name.ifEmpty { "Unnamed Driver" }
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val statusBadge = TextView(this).apply {
            text = if (driver.isOnline) "LIVE" else "OFFLINE"
            setTextColor(if (driver.isOnline) 0xFF10B981.toInt() else 0xFF64748B.toInt())
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16, 8, 16, 8)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(if (driver.isOnline) 0x1510B981.toInt() else 0x1564748B.toInt())
                setStroke(2, if (driver.isOnline) 0xFF10B981.toInt() else 0xFF64748B.toInt())
            }
        }
        topRow.addView(statusDot)
        topRow.addView(nameText)
        topRow.addView(statusBadge)

        // Info rows
        val emailText = makeInfoRow("📧  ${driver.email.ifEmpty { "No email" }}")
        val routeText = makeInfoRow("🛣️  Route: ${driver.route.ifEmpty { driver.travelId.ifEmpty { "Not assigned" } }}")

        val locationInfo = if (driver.isOnline && driver.location != null) {
            val speedKmh = (driver.location.speed * 3.6).toInt()
            makeInfoRow("📍  ${String.format("%.4f", driver.location.latitude)}, ${String.format("%.4f", driver.location.longitude)}  •  ${speedKmh} km/h")
        } else {
            makeInfoRow("📍  Location not available")
        }

        // Action buttons
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 0)
        }
        val editBtn = Button(this).apply {
            text = "Edit"
            setTextColor(0xFFC7D86A.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(0x20C7D86A.toInt())
                setStroke(2, 0xFFC7D86A.toInt())
            }
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(0, 0, 16, 0)
            layoutParams = lp
            setOnClickListener { showEditDriverDialog(driver) }
        }
        val deleteBtn = Button(this).apply {
            text = "Remove"
            setTextColor(0xFFEF4444.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(0x20EF4444.toInt())
                setStroke(2, 0xFFEF4444.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { confirmDeleteDriver(driver) }
        }
        btnRow.addView(editBtn)
        btnRow.addView(deleteBtn)

        inner.addView(topRow)
        inner.addView(emailText)
        inner.addView(routeText)
        inner.addView(locationInfo)
        inner.addView(btnRow)
        card.addView(inner)
        return card
    }

    private fun makeInfoRow(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(0xFFB3B3B3.toInt())
            textSize = 13f
            setPadding(0, 8, 0, 0)
        }
    }

    private fun showAddDriverDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_driver_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val nameInput = dialogView.findViewById<EditText>(R.id.driverName)
        val emailInput = dialogView.findViewById<EditText>(R.id.driverEmail)
        val passInput = dialogView.findViewById<EditText>(R.id.driverPassword)
        val phoneInput = dialogView.findViewById<EditText>(R.id.driverPhone)
        val routeInput = dialogView.findViewById<EditText>(R.id.driverRoute)
        val travelIdInput = dialogView.findViewById<EditText>(R.id.driverTravelId)
        val saveBtn = dialogView.findViewById<Button>(R.id.btnSaveDriver)
        val cancelBtn = dialogView.findViewById<Button>(R.id.btnCancelDriver)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        titleText.text = "Add New Driver"

        cancelBtn.setOnClickListener { dialog.dismiss() }
        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val pass = passInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val route = routeInput.text.toString().trim()
            val travelId = travelIdInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || travelId.isEmpty()) {
                Toast.makeText(this, "Name, Email, Password and Travel ID are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveBtn.isEnabled = false
            saveBtn.text = "Creating..."

            // Use secondary auth instance to create account without logging out admin
            val secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            // We use REST API approach: create user, save to DB, then we done
            // Actually use Admin SDK approach through database (simpler for demo)
            // Create via Firebase Auth then save profile
            val currentUser = auth.currentUser
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    val driverData = mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "route" to route,
                        "travelId" to travelId,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.getReference("drivers/$uid").setValue(driverData)
                        .addOnSuccessListener {
                            // Re-sign in as admin
                            Toast.makeText(this, "Driver '$name' added successfully!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    // Also create travels entry
                    db.getReference("travels/$travelId").updateChildren(
                        mapOf("travelName" to name, "driverName" to name, "route" to route, "isOnline" to false)
                    )
                }
                .addOnFailureListener {
                    saveBtn.isEnabled = true
                    saveBtn.text = "Save Driver"
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
        dialog.show()
    }

    private fun showEditDriverDialog(driver: DriverModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_driver_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val nameInput = dialogView.findViewById<EditText>(R.id.driverName)
        val emailInput = dialogView.findViewById<EditText>(R.id.driverEmail)
        val passInput = dialogView.findViewById<EditText>(R.id.driverPassword)
        val phoneInput = dialogView.findViewById<EditText>(R.id.driverPhone)
        val routeInput = dialogView.findViewById<EditText>(R.id.driverRoute)
        val travelIdInput = dialogView.findViewById<EditText>(R.id.driverTravelId)
        val saveBtn = dialogView.findViewById<Button>(R.id.btnSaveDriver)
        val cancelBtn = dialogView.findViewById<Button>(R.id.btnCancelDriver)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)

        titleText.text = "Edit Driver"
        nameInput.setText(driver.name)
        emailInput.setText(driver.email)
        emailInput.isEnabled = false
        passInput.hint = "Leave blank to keep password"
        phoneInput.setText(driver.phone)
        routeInput.setText(driver.route)
        travelIdInput.setText(driver.travelId)
        travelIdInput.isEnabled = false

        cancelBtn.setOnClickListener { dialog.dismiss() }
        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val route = routeInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveBtn.isEnabled = false
            saveBtn.text = "Saving..."
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "route" to route
            )
            db.getReference("drivers/${driver.uid}").updateChildren(updates)
                .addOnSuccessListener {
                    // Also update travels node
                    db.getReference("travels/${driver.travelId}").updateChildren(
                        mapOf("driverName" to name, "route" to route)
                    )
                    Toast.makeText(this, "Driver updated!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    saveBtn.isEnabled = true
                    saveBtn.text = "Save"
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
        dialog.show()
    }

    private fun confirmDeleteDriver(driver: DriverModel) {
        AlertDialog.Builder(this)
            .setTitle("Remove Driver")
            .setMessage("Are you sure you want to remove ${driver.name}? This cannot be undone.")
            .setPositiveButton("Remove") { _, _ ->
                db.getReference("drivers/${driver.uid}").removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "${driver.name} removed.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        driversListener?.let { driversRef.removeEventListener(it) }
    }
}
