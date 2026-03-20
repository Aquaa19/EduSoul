// File: EduSoul/app/src/main/java/com/aquaa/edusoul/activities/BaseActivity.kt
package com.aquaa.edusoul.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aquaa.edusoul.utils.ThemeManager
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.auth.AuthManager
import kotlinx.coroutines.runBlocking
import android.widget.Toast // Import Toast

/**
 * BaseActivity for all activities in the EduSoul application.
 * Ensures the selected theme is applied to all activities.
 */
open class BaseActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        authManager = AuthManager(this)

        val themeKey = runBlocking {
            val currentUser = authManager.getLoggedInUser()
            when (currentUser?.role) {
                User.ROLE_ADMIN, User.ROLE_OWNER -> ThemeManager.KEY_ADMIN_THEME
                User.ROLE_TEACHER -> ThemeManager.KEY_TEACHER_THEME
                User.ROLE_PARENT, User.ROLE_MANAGER -> ThemeManager.KEY_PARENT_THEME
                else -> ThemeManager.KEY_GLOBAL_SELECTED_THEME
            }
        }

        ThemeManager.applyTheme(this, ThemeManager.loadTheme(this, themeKey))

        super.onCreate(savedInstanceState)
    }

    // Helper function to show a Toast message
    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}