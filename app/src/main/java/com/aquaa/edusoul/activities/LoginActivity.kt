package com.aquaa.edusoul.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.R
import com.aquaa.edusoul.auth.AuthManager
import com.google.android.material.snackbar.Snackbar
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.activities.admin.AdminDashboardActivity
import com.aquaa.edusoul.activities.manager.ManagerDashboardActivity
import com.aquaa.edusoul.activities.parent.ParentDashboardActivity
import com.aquaa.edusoul.activities.teacher.TeacherDashboardActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : BaseActivity() {

    private val TAG = "LoginActivity"

    private lateinit var editTextIdentifier: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var textInputLayoutIdentifier: TextInputLayout
    private lateinit var textInputLayoutPassword: TextInputLayout
    private lateinit var buttonLogin: Button
    private lateinit var textViewGoToSignup: TextView
    private lateinit var progressBarLogin: ProgressBar

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authManager = AuthManager(this)

        // Check if user is already logged in and redirect
        lifecycleScope.launch(Dispatchers.IO) {
            if (authManager.isLoggedIn()) {
                val currentUser = authManager.getLoggedInUser()
                withContext(Dispatchers.Main) {
                    if (currentUser != null && currentUser.role.isNotBlank()) {
                        Log.i(TAG, "User ${currentUser.username} is already logged in. Role: ${currentUser.role}")
                        redirectToDashboard(currentUser.role)
                        finish()
                    } else {
                        // **CHANGE 1: Removed logoutUser() call.**
                        // The user is authenticated but their profile data is missing.
                        // We keep them logged in but show an error.
                        Log.e(TAG, "User is logged in to Firebase Auth but profile data is missing/corrupted.")
                        Snackbar.make(findViewById(android.R.id.content), "Session error, please log in again to refresh.", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        editTextIdentifier = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        textInputLayoutIdentifier = findViewById(R.id.textInputLayoutUsername)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        textViewGoToSignup = findViewById(R.id.textViewGoToSignup)
        progressBarLogin = findViewById(R.id.progressBarLogin)

        textInputLayoutIdentifier.hint = "Email or Username"

        buttonLogin.setOnClickListener { handleLogin() }

        textViewGoToSignup.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleLogin() {
        val identifier = editTextIdentifier.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        textInputLayoutIdentifier.error = null
        textInputLayoutPassword.error = null

        if (TextUtils.isEmpty(identifier)) {
            textInputLayoutIdentifier.error = "Email or Username is required"
            return
        }
        if (TextUtils.isEmpty(password)) {
            textInputLayoutPassword.error = "Password is required"
            return
        }

        progressBarLogin.visibility = View.VISIBLE
        buttonLogin.isEnabled = false
        textViewGoToSignup.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val emailToLogin: String? = if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                identifier
            } else {
                val normalizedIdentifier = identifier.lowercase(java.util.Locale.ROOT)
                authManager.getUserByUsername(normalizedIdentifier)?.email
            }

            if (emailToLogin.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    progressBarLogin.visibility = View.GONE
                    buttonLogin.isEnabled = true
                    textViewGoToSignup.isEnabled = true
                    textInputLayoutIdentifier.error = "No account found with this username or invalid email."
                    Log.w(TAG, "Login failed: Identifier '$identifier' not found or invalid.")
                }
                return@launch
            }

            try {
                val firebaseUser = authManager.loginUser(emailToLogin, password)
                if (firebaseUser != null) {
                    val userProfile = authManager.getLoggedInUser()
                    withContext(Dispatchers.Main) {
                        if (userProfile != null) {
                            val welcomeName = userProfile.fullName ?: userProfile.username
                            Snackbar.make(findViewById(android.R.id.content), "Login Successful! Welcome $welcomeName", Snackbar.LENGTH_SHORT).show()
                            Log.i(TAG, "User ${userProfile.username} logged in with role ${userProfile.role}")
                            redirectToDashboard(userProfile.role)
                            finish()
                        } else {
                            // **CHANGE 2: Removed logoutUser() call.**
                            // This is the main fix. The user is logged in, but we can't find their profile.
                            // We now show an error but keep them logged in.
                            progressBarLogin.visibility = View.GONE
                            buttonLogin.isEnabled = true
                            textViewGoToSignup.isEnabled = true
                            Snackbar.make(findViewById(android.R.id.content), "Login successful, but couldn't fetch user profile.", Snackbar.LENGTH_LONG).show()
                            Log.e(TAG, "Login successful, but Firestore profile for user ${firebaseUser.uid} not found.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBarLogin.visibility = View.GONE
                        buttonLogin.isEnabled = true
                        textViewGoToSignup.isEnabled = true
                        Snackbar.make(findViewById(android.R.id.content), "Login Failed. Invalid email or password.", Snackbar.LENGTH_LONG).show()
                        textInputLayoutPassword.error = "Invalid email or password."
                        Log.w(TAG, "Login failed for identifier: $identifier")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarLogin.visibility = View.GONE
                    buttonLogin.isEnabled = true
                    textViewGoToSignup.isEnabled = true
                    Snackbar.make(findViewById(android.R.id.content), "An unexpected error occurred: ${e.message}", Snackbar.LENGTH_LONG).show()
                    Log.e(TAG, "Unexpected error during login: ${e.message}", e)
                }
            }
        }
    }

    private fun redirectToDashboard(role: String) {
        val intent: Intent = when {
            role.isBlank() -> {
                Log.e(TAG, "User role is null or blank, cannot redirect to dashboard.")
                Snackbar.make(findViewById(android.R.id.content), "Error: User role not found. Cannot open dashboard.", Snackbar.LENGTH_LONG).show()
                return
            }
            role == User.ROLE_OWNER || role == User.ROLE_ADMIN -> Intent(this, AdminDashboardActivity::class.java)
            role == User.ROLE_TEACHER -> Intent(this, TeacherDashboardActivity::class.java)
            role == User.ROLE_PARENT -> Intent(this, ParentDashboardActivity::class.java)
            role == User.ROLE_MANAGER -> Intent(this, ManagerDashboardActivity::class.java)
            else -> {
                Log.e(TAG, "Unknown user role: $role. Cannot redirect.")
                Snackbar.make(findViewById(android.R.id.content), "Unknown user role. Please contact support.", Snackbar.LENGTH_LONG).show()
                return
            }
        }
        startActivity(intent)
        finish()
    }
}