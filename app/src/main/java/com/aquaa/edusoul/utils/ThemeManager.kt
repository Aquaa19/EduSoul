package com.aquaa.edusoul.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.aquaa.edusoul.R

object ThemeManager {

    private const val PREFS_NAME = "EduSoulThemePrefs"
    const val KEY_GLOBAL_SELECTED_THEME = "selected_theme" // Retained for backward compatibility if needed

    // New keys for role-specific themes
    const val KEY_ADMIN_THEME = "admin_dashboard_theme"
    const val KEY_TEACHER_THEME = "teacher_dashboard_theme"
    const val KEY_PARENT_THEME = "parent_dashboard_theme"


    enum class AppTheme(val themeResId: Int, val themeName: String) {
        NEUTRAL(R.style.Theme_EduSoul, "System Default"), // Points to Theme.EduSoul for system default behavior
        BLUE(R.style.Theme_EduSoul_Blue, "Blue"),
        PURPLE(R.style.Theme_EduSoul_Purple, "Purple"),
        // Renamed GREEN to DEEP_CYAN and updated its resource ID
        DEEP_CYAN(R.style.Theme_EduSoul_DeepCyan, "Deep Cyan"),
        ORANGE(R.style.Theme_EduSoul_Orange, "Orange"),
        RED(R.style.Theme_EduSoul_Red, "Red"),
        TEAL(R.style.Theme_EduSoul_Teal, "Teal"),
        INDIGO(R.style.Theme_EduSoul_Indigo, "Indigo"),
        DEEP_ORANGE(R.style.Theme_EduSoul_DeepOrange, "Deep Orange");
    }

    // Helper to get the theme resource ID based on the theme name string
    fun getThemeResIdByName(themeName: String?): Int {
        return when (themeName) {
            AppTheme.NEUTRAL.themeName -> AppTheme.NEUTRAL.themeResId
            AppTheme.BLUE.themeName -> AppTheme.BLUE.themeResId
            AppTheme.PURPLE.themeName -> AppTheme.PURPLE.themeResId
            AppTheme.DEEP_CYAN.themeName -> AppTheme.DEEP_CYAN.themeResId // Updated for new theme
            AppTheme.ORANGE.themeName -> AppTheme.ORANGE.themeResId
            AppTheme.RED.themeName -> AppTheme.RED.themeResId
            AppTheme.TEAL.themeName -> AppTheme.TEAL.themeResId
            AppTheme.INDIGO.themeName -> AppTheme.INDIGO.themeResId
            AppTheme.DEEP_ORANGE.themeName -> AppTheme.DEEP_ORANGE.themeResId
            else -> AppTheme.NEUTRAL.themeResId // Default to Neutral if unknown or null
        }
    }

    val availableThemes: List<AppTheme> = listOf(
        AppTheme.NEUTRAL,
        AppTheme.BLUE,
        AppTheme.PURPLE,
        AppTheme.DEEP_CYAN, // Updated to new theme
        AppTheme.ORANGE,
        AppTheme.RED,
        AppTheme.TEAL,
        AppTheme.INDIGO,
        AppTheme.DEEP_ORANGE
    )

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Modified: Now accepts a themeKey to save specific themes
    fun saveTheme(context: Context, theme: AppTheme, themeKey: String) {
        getPreferences(context).edit().putString(themeKey, theme.themeName).apply()
    }

    // Modified: Now accepts a themeKey to load specific themes with role-based defaults
    fun loadTheme(context: Context, themeKey: String): AppTheme {
        val defaultThemeForRole = when (themeKey) {
            KEY_ADMIN_THEME -> AppTheme.NEUTRAL // Admins default to neutral (now green-ish) if no custom theme set
            KEY_TEACHER_THEME -> AppTheme.DEEP_CYAN // Teachers default to new Deep Cyan if no custom theme set
            KEY_PARENT_THEME -> AppTheme.NEUTRAL // Parents always default to neutral (now green-ish)
            else -> AppTheme.NEUTRAL // Fallback default
        }
        val themeName = getPreferences(context).getString(themeKey, defaultThemeForRole.themeName)
        return availableThemes.find { it.themeName == themeName } ?: defaultThemeForRole
    }

    // Optional: If you still need a global theme loading method for non-role-specific screens
    fun loadGlobalTheme(context: Context): AppTheme {
        val themeName = getPreferences(context).getString(KEY_GLOBAL_SELECTED_THEME, AppTheme.NEUTRAL.themeName)
        return availableThemes.find { it.themeName == themeName } ?: AppTheme.NEUTRAL
    }

    fun applyTheme(context: Context, theme: AppTheme) {
        when (theme) {
            AppTheme.NEUTRAL -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                context.setTheme(R.style.Theme_EduSoul) // Apply the base theme
            }
            else -> {
                // Allow custom themes to also follow the system night mode setting.
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                context.setTheme(theme.themeResId)
            }
        }
    }
}