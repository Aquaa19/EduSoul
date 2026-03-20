// File: main/java/com/aquaa/edusoul/activities/EditProfileActivity.kt
package com.aquaa.edusoul.activities

import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.R
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.UserViewModel
import com.aquaa.edusoul.viewmodels.UserViewModelFactory
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : BaseActivity() {

    private val TAG = "EditProfileActivity"

    private lateinit var tilFullNameProfile: TextInputLayout
    private lateinit var editTextFullNameProfile: TextInputEditText
    private lateinit var tilUsernameProfile: TextInputLayout
    private lateinit var editTextUsernameProfile: TextInputEditText
    private lateinit var tilEmailProfile: TextInputLayout
    private lateinit var editTextEmailProfile: TextInputEditText
    private lateinit var tilPhoneProfile: TextInputLayout
    private lateinit var editTextPhoneProfile: TextInputEditText
    private lateinit var buttonUpdateProfile: Button

    private lateinit var tilCurrentPassword: TextInputLayout
    private lateinit var editTextCurrentPassword: TextInputEditText
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var editTextNewPassword: TextInputEditText
    private lateinit var tilConfirmNewPassword: TextInputLayout
    private lateinit var editTextConfirmNewPassword: TextInputEditText
    private lateinit var buttonChangePassword: Button

    private lateinit var authManager: AuthManager
    private var currentUser: User? = null

    private lateinit var userViewModel: UserViewModel
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val toolbar = findViewById<Toolbar>(R.id.toolbarEditProfile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        // The line below was causing the crash because AppDatabase is now a dummy
        // and its DAO methods throw NotImplementedError when called by Room's internal mechanisms.
        // val userDao = AppDatabase.getDatabase(applicationContext).userDao()

        // UserRepository is already correctly instantiated to use Firestore.
        userRepository = UserRepository()
        val factory = UserViewModelFactory(userRepository, authManager)
        userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)

        userViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_LONG).show()
                userViewModel.clearErrorMessage()
            }
        }

        initializeViews()
        setupListeners()
        loadUserProfile()
    }

    private fun initializeViews() {
        tilFullNameProfile = findViewById(R.id.tilFullNameProfile)
        editTextFullNameProfile = findViewById(R.id.editTextFullNameProfile)
        tilUsernameProfile = findViewById(R.id.tilUsernameProfile)
        editTextUsernameProfile = findViewById(R.id.editTextUsernameProfile)
        tilEmailProfile = findViewById(R.id.tilEmailProfile)
        editTextEmailProfile = findViewById(R.id.editTextEmailProfile)
        tilPhoneProfile = findViewById(R.id.tilPhoneProfile)
        editTextPhoneProfile = findViewById(R.id.editTextPhoneProfile)
        buttonUpdateProfile = findViewById(R.id.buttonUpdateProfile)

        tilCurrentPassword = findViewById(R.id.tilCurrentPassword)
        editTextCurrentPassword = findViewById(R.id.editTextCurrentPassword)
        tilNewPassword = findViewById(R.id.tilNewPassword)
        editTextNewPassword = findViewById(R.id.editTextNewPassword)
        tilConfirmNewPassword = findViewById(R.id.tilConfirmNewPassword)
        editTextConfirmNewPassword = findViewById(R.id.editTextConfirmNewPassword)
        buttonChangePassword = findViewById(R.id.buttonChangePassword)
    }

    private fun setupListeners() {
        buttonUpdateProfile.setOnClickListener { updateProfileInfo() }
        buttonChangePassword.setOnClickListener { changePassword() }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch(Dispatchers.IO) {
            val loggedInUser = authManager.getLoggedInUser()
            if (loggedInUser != null) {
                currentUser = loggedInUser
                withContext(Dispatchers.Main) {
                    currentUser?.let { user ->
                        editTextFullNameProfile.setText(user.fullName)
                        editTextUsernameProfile.setText(user.username)
                        editTextEmailProfile.setText(user.email)
                        editTextPhoneProfile.setText(user.phoneNumber)

                        if (user.role == User.ROLE_OWNER) {
                            editTextFullNameProfile.isEnabled = false
                            editTextEmailProfile.isEnabled = false
                            editTextPhoneProfile.isEnabled = false
                            buttonUpdateProfile.isEnabled = false
                            buttonUpdateProfile.text = "Owner Profile Not Editable"

                            editTextCurrentPassword.isEnabled = false
                            editTextNewPassword.isEnabled = false
                            editTextConfirmNewPassword.isEnabled = false
                            buttonChangePassword.isEnabled = false
                            buttonChangePassword.text = "Owner Password Not Changeable"
                            Snackbar.make(findViewById(android.R.id.content), "Owner account details are not editable.", Snackbar.LENGTH_LONG).show()
                        }
                    } ?: run {
                        Snackbar.make(findViewById(android.R.id.content), "Error loading profile data.", Snackbar.LENGTH_LONG).show()
                        finish()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Snackbar.make(findViewById(android.R.id.content), "User not logged in.", Snackbar.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun updateProfileInfo() {
        val user = currentUser ?: return

        val fullName = editTextFullNameProfile.text.toString().trim()
        val email = editTextEmailProfile.text.toString().trim()
        val phone = editTextPhoneProfile.text.toString().trim()

        tilFullNameProfile.error = null
        tilEmailProfile.error = null

        var isValid = true
        if (fullName.isEmpty()) {
            tilFullNameProfile.error = "Full name cannot be empty"
            isValid = false
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmailProfile.error = "Invalid email address"
            isValid = false
        }

        if (!isValid) return

        lifecycleScope.launch(Dispatchers.IO) {
            userViewModel.updateUserProfile(
                user.id,
                fullName,
                if (email.isEmpty()) null else email,
                if (phone.isEmpty()) null else phone,
                user.profileImagePath
            )
        }
    }

    private fun changePassword() {
        val user = currentUser ?: return
        val userEmail = user.email
        if (userEmail.isNullOrBlank()) {
            Toast.makeText(this, "Cannot change password without a valid email address.", Toast.LENGTH_LONG).show()
            return
        }

        val currentPassword = editTextCurrentPassword.text.toString().trim()
        val newPassword = editTextNewPassword.text.toString().trim()
        val confirmNewPassword = editTextConfirmNewPassword.text.toString().trim()

        tilCurrentPassword.error = null
        tilNewPassword.error = null
        tilConfirmNewPassword.error = null

        var isValid = true
        if (currentPassword.isEmpty()) {
            tilCurrentPassword.error = "Current password is required"
            isValid = false
        }
        if (newPassword.isEmpty()) {
            tilNewPassword.error = "New password is required"
            isValid = false
        } else if (newPassword.length < 6) {
            tilNewPassword.error = "Password must be at least 6 characters"
            isValid = false
        }
        if (confirmNewPassword.isEmpty()) {
            tilConfirmNewPassword.error = "Confirm new password is required"
            isValid = false
        } else if (newPassword != confirmNewPassword) {
            tilConfirmNewPassword.error = "New passwords do not match"
            isValid = false
        }

        if (!isValid) return

        lifecycleScope.launch(Dispatchers.IO) {
            val firebaseUser = authManager.loginUser(userEmail, currentPassword)
            val isCurrentPasswordCorrect = firebaseUser != null

            withContext(Dispatchers.Main) {
                if (!isCurrentPasswordCorrect) {
                    tilCurrentPassword.error = "Incorrect current password."
                    return@withContext
                }

                userViewModel.changePassword(newPassword)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}