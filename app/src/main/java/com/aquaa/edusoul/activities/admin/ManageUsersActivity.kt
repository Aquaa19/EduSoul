package com.aquaa.edusoul.activities.admin

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.UserAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.UserViewModel
import com.aquaa.edusoul.viewmodels.UserViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.ArrayList
import java.util.regex.Pattern
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.activities.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageUsersActivity : BaseActivity(), UserAdapter.OnUserActionsListener {

    private val TAG = "ManageUsersActivity"

    private lateinit var recyclerViewUsers: RecyclerView
    private lateinit var fabAddUser: FloatingActionButton
    private lateinit var textViewNoUsers: TextView
    private lateinit var userAdapter: UserAdapter
    private lateinit var varUserList: MutableList<User>
    private lateinit var userViewModel: UserViewModel
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageUsers)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        val userRepository = UserRepository()
        val factory = UserViewModelFactory(userRepository, authManager)
        userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)

        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
        fabAddUser = findViewById(R.id.fabAddUser)
        textViewNoUsers = findViewById(R.id.textViewNoUsers)

        varUserList = ArrayList()
        userAdapter = UserAdapter(this, varUserList as ArrayList<User>, this)

        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        recyclerViewUsers.adapter = userAdapter

        fabAddUser.setOnClickListener { _ -> showAddEditUserDialog(null) }

        userViewModel.manageableUsers.observe(this) { users ->
            Log.d(TAG, "Manageable Users LiveData updated. Size: ${users.size}")
            userAdapter.setUsers(users)
            updateEmptyState()
        }

        userViewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading state: $isLoading")
        }

        userViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error/Success: $it")
                userViewModel.clearErrorMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.loadManageableUsers()
    }

    private fun updateEmptyState() {
        if (userAdapter.itemCount == 0) {
            textViewNoUsers.visibility = View.VISIBLE
            recyclerViewUsers.visibility = View.GONE
            Log.i(TAG, "No users found in database.")
        } else {
            textViewNoUsers.visibility = View.GONE
            recyclerViewUsers.visibility = View.VISIBLE
            Log.i(TAG, "Displaying ${userAdapter.itemCount} users.")
        }
    }

    private fun showAddEditUserDialog(userToEdit: User?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_user_admin, null)
        builder.setView(dialogView)

        val tilFullName = dialogView.findViewById<TextInputLayout>(R.id.tilFullNameUserDialog)
        val editTextFullName = dialogView.findViewById<TextInputEditText>(R.id.editTextFullNameUserDialog)
        val tilUsername = dialogView.findViewById<TextInputLayout>(R.id.tilUsernameUserDialog)
        val editTextUsername = dialogView.findViewById<TextInputEditText>(R.id.editTextUsernameUserDialog)
        val tilEmail = dialogView.findViewById<TextInputLayout>(R.id.tilEmailUserDialog)
        val editTextEmail = dialogView.findViewById<TextInputEditText>(R.id.editTextEmailUserDialog)
        val tilPhone = dialogView.findViewById<TextInputLayout>(R.id.tilPhoneUserDialog)
        val editTextPhone = dialogView.findViewById<TextInputEditText>(R.id.editTextPhoneUserDialog)
        val tilPassword = dialogView.findViewById<TextInputLayout>(R.id.tilPasswordUserDialog)
        val editTextPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextPasswordUserDialog)
        val radioGroupRole = dialogView.findViewById<RadioGroup>(R.id.radioGroupRoleUserDialog)
        val radioButtonTeacher = dialogView.findViewById<RadioButton>(R.id.radioButtonTeacherUserDialog)
        val radioButtonParent = dialogView.findViewById<RadioButton>(R.id.radioButtonParentUserDialog)
        val radioButtonManager = dialogView.findViewById<RadioButton>(R.id.radioButtonManagerUserDialog)

        val isOwner = userToEdit?.role == User.ROLE_OWNER

        builder.setTitle(if (userToEdit == null) "Add New User" else if (isOwner) "View Owner Details (Not Editable)" else "Edit User")

        if (userToEdit != null) {
            editTextFullName.setText(userToEdit.fullName)
            editTextUsername.setText(userToEdit.username)
            editTextEmail.setText(userToEdit.email)
            editTextPhone.setText(userToEdit.phoneNumber)

            when (userToEdit.role) {
                User.ROLE_TEACHER -> radioButtonTeacher.isChecked = true
                User.ROLE_PARENT -> radioButtonParent.isChecked = true
                "manager" -> radioButtonManager.isChecked = true
                User.ROLE_OWNER -> {
                    radioButtonTeacher.isEnabled = false
                    radioButtonParent.isEnabled = false
                    radioButtonManager.isEnabled = false
                }
            }

            if (isOwner) {
                editTextFullName.isEnabled = false
                editTextUsername.isEnabled = false
                editTextEmail.isEnabled = false
                editTextPhone.isEnabled = false
                radioGroupRole.isEnabled = false
                for (i in 0 until radioGroupRole.childCount) {
                    radioGroupRole.getChildAt(i).isEnabled = false
                }
                tilPassword.visibility = View.GONE
            } else {
                // For editing existing users, username is not editable and password field is hidden
                editTextUsername.isEnabled = false
                tilPassword.visibility = View.GONE
            }
        } else {
            // For adding new users, username is editable and password field is visible
            editTextUsername.isEnabled = true
            tilPassword.visibility = View.VISIBLE
        }

        builder.setPositiveButton(if (userToEdit == null) "Add" else if (isOwner) "Close" else "Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { _ ->
            if (isOwner) {
                alertDialog.dismiss()
                return@setOnClickListener
            }

            val fullName = editTextFullName.text.toString().trim()
            val username = editTextUsername.text.toString().trim() // Username is now optional
            val email = editTextEmail.text.toString().trim() // Email is now compulsory
            val phone = editTextPhone.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            val role = when (radioGroupRole.checkedRadioButtonId) {
                R.id.radioButtonTeacherUserDialog -> User.ROLE_TEACHER
                R.id.radioButtonParentUserDialog -> User.ROLE_PARENT
                R.id.radioButtonManagerUserDialog -> "manager"
                else -> ""
            }

            tilFullName.error = null
            tilUsername.error = null
            tilEmail.error = null
            tilPassword.error = null
            var isValid = true

            if (fullName.isEmpty()) {
                tilFullName.error = "Full name cannot be empty"
                isValid = false
            }
            // Email is now compulsory
            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Invalid email address"
                isValid = false
            }
            // Username is now optional, so no validation for emptiness
            // No change to password validation for new users
            if (userToEdit == null && password.isEmpty()) {
                tilPassword.error = "Password cannot be empty"
                isValid = false
            }
            if (role.isEmpty()) {
                Toast.makeText(this, "Please select a role.", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            lifecycleScope.launch(Dispatchers.IO) {
                if (userToEdit == null) { // Adding new user
                    // For new users, if username is empty, we can generate one or leave it null
                    val finalUsername = username.ifEmpty { null } // Set to null if empty

                    val newUser = authManager.registerUser(
                        email,
                        password,
                        fullName,
                        finalUsername ?: email, // Use email as username if username is empty
                        role
                    )
                    withContext(Dispatchers.Main) {
                        if (newUser != null) {
                            Toast.makeText(this@ManageUsersActivity, "User added successfully", Toast.LENGTH_SHORT).show()
                            alertDialog.dismiss()
                        } else {
                            // Check for email existence if registration failed
                            val emailExists = authManager.isEmailExists(email)
                            if (emailExists) {
                                tilEmail.error = "Email already exists."
                            } else {
                                Toast.makeText(this@ManageUsersActivity, "Failed to add user (Unknown error)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else { // Editing existing user
                    val updatedUser = User(
                        userToEdit.id,
                        userToEdit.username, // Keep existing username
                        userToEdit.passwordHash,
                        fullName,
                        email, // Update email
                        if (phone.isEmpty()) null else phone,
                        role,
                        userToEdit.profileImagePath,
                        userToEdit.registrationDate
                    )
                    userViewModel.updateUser(updatedUser)
                    withContext(Dispatchers.Main) {
                        alertDialog.dismiss()
                    }
                }
            }
        }
    }

    override fun onEditUser(user: User, position: Int) {
        showAddEditUserDialog(user)
    }

    override fun onDeleteUser(user: User, position: Int) {
        if (user.role == User.ROLE_OWNER) {
            AlertDialog.Builder(this)
                .setTitle("Cannot Delete Owner")
                .setMessage("The primary owner account '${user.fullName}' cannot be deleted for system integrity reasons.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete user '${user.fullName}'?")
            .setPositiveButton("Delete") { _, _ ->
                userViewModel.deleteUser(user.id)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_delete)
            .show()
    }

    override fun onAssignTeacher(user: User, position: Int) {
        Log.d(TAG, "onAssignTeacher: Clicked for teacher: " + user.fullName)
        val intent = Intent(this, TeacherAssignmentActivity::class.java)
        intent.putExtra("TEACHER_ID", user.id)
        intent.putExtra("TEACHER_NAME", user.fullName)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}