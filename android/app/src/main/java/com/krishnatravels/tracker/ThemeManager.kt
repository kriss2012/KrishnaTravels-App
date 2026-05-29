/**
 * File: ThemeManager.kt
 * Date: 2026-05-29
 * #by Kiri Team
 */
package com.krishnatravels.tracker

import android.content.Context
import android.content.SharedPreferences

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "current_theme"

    const val THEME_NATURE = 0
    const val THEME_OCEAN = 1
    const val THEME_SUNSET = 2

    fun applyTheme(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeId = prefs.getInt(KEY_THEME, THEME_NATURE)
        
        when (themeId) {
            THEME_OCEAN -> context.setTheme(R.style.AppTheme_Ocean)
            THEME_SUNSET -> context.setTheme(R.style.AppTheme_Sunset)
            else -> context.setTheme(R.style.AppTheme_Nature)
        }
    }

    fun setTheme(context: Context, themeId: Int) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, themeId).apply()
    }
}
