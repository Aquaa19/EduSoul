package com.aquaa.edusoul.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.R
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.User
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class SignupActivity : BaseActivity() {

    private val TAG = "SignupActivity"

    private lateinit var editTextFullName: TextInputEditText
    private lateinit var editTextUsername: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextConfirmPassword: TextInputEditText

    private lateinit var textInputLayoutFullName: TextInputLayout
    private lateinit var textInputLayoutUsername: TextInputLayout
    private lateinit var textInputLayoutEmail: TextInputLayout
    private lateinit var textInputLayoutPassword: TextInputLayout
    private lateinit var textInputLayoutConfirmPassword: TextInputLayout

    // Removed RadioGroup as role selection is no longer needed
    // private lateinit var radioGroupRole: RadioGroup
    private lateinit var buttonSignup: Button
    private lateinit var textViewGoToLogin: TextView
    private lateinit var progressBarSignup: ProgressBar

    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        authManager = AuthManager(this)

        // Initialize UI elements
        editTextFullName = findViewById(R.id.editTextFullNameSignup)
        editTextUsername = findViewById(R.id.editTextUsernameSignup)
        editTextEmail = findViewById(R.id.editTextEmailSignup)
        editTextPassword = findViewById(R.id.editTextPasswordSignup)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPasswordSignup)

        textInputLayoutFullName = findViewById(R.id.textInputLayoutFullNameSignup)
        textInputLayoutUsername = findViewById(R.id.textInputLayoutUsernameSignup)
        textInputLayoutEmail = findViewById(R.id.textInputLayoutEmailSignup)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPasswordSignup)
        textInputLayoutConfirmPassword = findViewById(R.id.textInputLayoutConfirmPasswordSignup)

        // Removed initialization for radioGroupRole
        // radioGroupRole = findViewById(R.id.radioGroupRole)
        buttonSignup = findViewById(R.id.buttonSignup)
        textViewGoToLogin = findViewById(R.id.textViewGoToLogin)
        progressBarSignup = findViewById(R.id.progressBarSignup)

        buttonSignup.setOnClickListener { handleSignup() }

        textViewGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun handleSignup() {
        val fullName = editTextFullName.text.toString().trim()
        val username = editTextUsername.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()

        // Clear previous errors
        textInputLayoutFullName.error = null
        textInputLayoutUsername.error = null
        textInputLayoutEmail.error = null
        textInputLayoutPassword.error = null
        textInputLayoutConfirmPassword.error = null

        // Validation
        var isValid = true
        if (TextUtils.isEmpty(fullName)) {
            textInputLayoutFullName.error = "Full Name is required."
            isValid = false
        }
        // Username is now optional, so no validation for emptiness here
        // If username is empty, it will be handled as null in AuthManager.registerUser
        if (TextUtils.isEmpty(email)) {
            textInputLayoutEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputLayoutEmail.error = "Invalid email address"
            isValid = false
        }
        if (TextUtils.isEmpty(password)) {
            textInputLayoutPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            textInputLayoutPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            textInputLayoutConfirmPassword.error = "Confirm Password is required"
            isValid = false
        } else if (password != confirmPassword) {
            textInputLayoutConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        // Role is now hardcoded to PARENT, no need for radio button check
        val role = User.ROLE_PARENT

        if (!isValid) {
            return
        }

        progressBarSignup.visibility = View.VISIBLE
        buttonSignup.isEnabled = false
        textViewGoToLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            // If username is empty, pass null to AuthManager
            val finalUsername = username.ifEmpty { null }

            try {
                val newUser = authManager.registerUser(email, password, fullName, finalUsername, role)

                withContext(Dispatchers.Main) {
                    progressBarSignup.visibility = View.GONE
                    buttonSignup.isEnabled = true
                    textViewGoToLogin.isEnabled = true

                    if (newUser != null) {
                        Toast.makeText(this@SignupActivity, "Signup Successful! Please log in.", Toast.LENGTH_LONG).show()
                        Log.i(TAG, "User ${newUser.username} signed up with role ${newUser.role}")
                        finish()
                    } else {
                        val emailExists = authManager.isEmailExists(email)
                        val usernameExists = finalUsername?.let { authManager.isUsernameExists(it) } ?: false

                        if (emailExists) {
                            textInputLayoutEmail.error = "Email already exists."
                        } else if (usernameExists) {
                            textInputLayoutUsername.error = "Username already exists."
                        } else {
                            Toast.makeText(this@SignupActivity, "Signup Failed. Please try again.", Toast.LENGTH_LONG).show()
                        }
                        Log.w(TAG, "Signup failed for email: $email, username: $finalUsername")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarSignup.visibility = View.GONE
                    buttonSignup.isEnabled = true
                    textViewGoToLogin.isEnabled = true
                    Toast.makeText(this@SignupActivity, "An unexpected error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Unexpected error during signup: ${e.message}", e)
                }
            }
        }
    }
}
