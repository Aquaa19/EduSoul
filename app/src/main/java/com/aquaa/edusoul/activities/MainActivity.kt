package com.aquaa.edusoul.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.R
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.activities.admin.AdminDashboardActivity
import com.aquaa.edusoul.activities.manager.ManagerDashboardActivity
import com.aquaa.edusoul.activities.parent.ParentDashboardActivity
import com.aquaa.edusoul.activities.teacher.TeacherDashboardActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {

    private val TAG = "MainActivity"
    private lateinit var authManager: AuthManager
    private val SPLASH_DELAY = 1500L // Reduced for quicker permission prompt
    private val PERMISSION_REQUEST_CODE = 1001

    private val requiredPermissions = mutableListOf<String>().apply {
        // Storage permissions for older Android versions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // API 28 and below
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        // Calendar permissions
        add(Manifest.permission.READ_CALENDAR)
        add(Manifest.permission.WRITE_CALENDAR)
        // Audio recording permission
        add(Manifest.permission.RECORD_AUDIO)
        // Post Notifications permission for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Using ActivityResultLauncher for a cleaner way to handle permission results
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.d(TAG, "All necessary permissions granted.")
                continueAppFlow()
            } else {
                Log.w(TAG, "Some permissions were denied.")
                // Check if any critical permission was denied.
                // For a first-time install, you might want to explain why permissions are needed.
                val deniedPermissions = permissions.filter { !it.value }.keys
                val rationaleNeeded = deniedPermissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }

                if (rationaleNeeded) {
                    showPermissionRationaleDialog(deniedPermissions)
                } else {
                    // User checked "Don't ask again" or denied permanently.
                    // Guide them to app settings.
                    showSettingsDialog()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authManager = AuthManager(this)

        // Check if it's the first launch
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPrefs.getBoolean("is_first_launch", true)

        if (isFirstLaunch) {
            Log.d(TAG, "First launch detected. Requesting permissions.")
            requestAppPermissions()
            sharedPrefs.edit().putBoolean("is_first_launch", false).apply() // Set flag to false
        } else {
            Log.d(TAG, "Not first launch. Skipping direct permission request.")
            Handler(Looper.getMainLooper()).postDelayed({
                continueAppFlow()
            }, SPLASH_DELAY)
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Permissions to request: ${permissionsToRequest.joinToString()}")
            requestPermissionsLauncher.launch(permissionsToRequest)
        } else {
            Log.d(TAG, "All required permissions are already granted.")
            continueAppFlow()
        }
    }

    private fun showPermissionRationaleDialog(deniedPermissions: Set<String>) {
        val message = "This app requires the following permissions to function properly:\n\n" +
                deniedPermissions.joinToString("\n") { permissionName ->
                    when (permissionName) {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE -> "- Storage (to save/load files)"
                        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR -> "- Calendar (to manage schedules)"
                        Manifest.permission.RECORD_AUDIO -> "- Microphone (for audio features)"
                        Manifest.permission.POST_NOTIFICATIONS -> "- Notifications (to receive updates)"
                        else -> "- ${permissionName.substringAfterLast(".")}" // Fallback for other permissions
                    }
                } + "\n\nPlease grant these permissions."

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant") { dialog, _ ->
                requestAppPermissions() // Re-request permissions
                dialog.dismiss()
            }
            .setNegativeButton("Exit App") { dialog, _ ->
                Toast.makeText(this, "Permissions denied. Exiting app.", Toast.LENGTH_LONG).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Denied Permanently")
            .setMessage("Some essential permissions were denied permanently. Please enable them from App Settings to use this app.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                dialog.dismiss()
                finish() // Optionally finish the activity, or handle onResume if you expect them to return
            }
            .setNegativeButton("Exit App") { dialog, _ ->
                Toast.makeText(this, "App cannot function without required permissions. Exiting.", Toast.LENGTH_LONG).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun continueAppFlow() {
        Log.d(TAG, "Continuing app flow after permissions.")
        Handler(Looper.getMainLooper()).postDelayed({
            lifecycleScope.launch {
                checkLoginStatusAndRedirect()
            }
        }, SPLASH_DELAY)
    }

    private suspend fun checkLoginStatusAndRedirect() {
        if (authManager.isLoggedIn()) {
            val currentUser = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                if (currentUser != null && currentUser.role.isNotBlank()) {
                    Log.i(TAG, "User ${currentUser.username} is already logged in. Role: ${currentUser.role}")
                    redirectToDashboard(currentUser.role)
                } else {
                    Log.e(TAG, "User is logged in but user data or role is null/corrupted. Logging out.")
                    authManager.logoutUser()
                    redirectToLogin()
                }
            }
        } else {
            Log.i(TAG, "No user logged in. Redirecting to LoginActivity.")
            withContext(Dispatchers.Main) {
                redirectToLogin()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun redirectToDashboard(role: String) {
        val intent: Intent = when (role) {
            User.ROLE_OWNER, User.ROLE_ADMIN -> Intent(this, AdminDashboardActivity::class.java)
            User.ROLE_TEACHER -> Intent(this, TeacherDashboardActivity::class.java)
            User.ROLE_PARENT -> Intent(this, ParentDashboardActivity::class.java)
            User.ROLE_MANAGER -> Intent(this, ManagerDashboardActivity::class.java)
            else -> {
                Log.e(TAG, "Unknown user role: $role. Defaulting to Login screen after logout.")
                Toast.makeText(this, "Unknown user role. Please log in again.", Toast.LENGTH_LONG).show()
                authManager.logoutUser()
                Intent(this, LoginActivity::class.java)
            }
        }
        startActivity(intent)
        finish()
    }
}