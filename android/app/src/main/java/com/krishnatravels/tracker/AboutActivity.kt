/**
 * File: AboutActivity.kt
 * Date: 2026-05-29
 * #by Kiri Team
 */
package com.krishnatravels.tracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
    }
}
