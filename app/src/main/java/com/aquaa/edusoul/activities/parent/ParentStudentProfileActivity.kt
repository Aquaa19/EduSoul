// File: main/java/com/aquaa/edusoul/activities/parent/ParentStudentProfileActivity.kt
package com.aquaa.edusoul.activities.parent

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.R
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ParentStudentProfileViewModel
import com.aquaa.edusoul.viewmodels.ParentStudentProfileViewModelFactory
import java.util.ArrayList
import java.util.HashMap
// REMOVED: import java.util.List // Not needed
// REMOVED: import java.util.Map // Not needed
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * ParentStudentProfileActivity: Allows a parent to view their child(ren)'s profile details.
 * Migrated to Kotlin and MVVM.
 */
class ParentStudentProfileActivity : AppCompatActivity() {

    private val TAG = "ParentStudentProfileActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var spinnerSelectChild: Spinner
    private lateinit var textViewNoChildren: TextView

    // UI elements for student profile details
    private lateinit var textViewStudentName: TextView
    private lateinit var textViewAdmissionNo: TextView
    private lateinit var textViewDateOfBirth: TextView
    private lateinit var textViewGender: TextView
    private lateinit var textViewGradeClass: TextView
    private lateinit var textViewAdmissionDate: TextView
    private lateinit var textViewSchoolName: TextView
    private lateinit var textViewAddress: TextView
    private lateinit var textViewNotes: TextView
    private lateinit var imageViewProfile: ImageView

    private lateinit var authManager: AuthManager
    private var currentParent: User? = null

    private lateinit var childrenList: java.util.ArrayList<Student>
    private lateinit var childNameToIdMap: java.util.HashMap<String, String> // Changed value type from Long to String

    private lateinit var parentStudentProfileViewModel: ParentStudentProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_student_profile)

        toolbar = findViewById(R.id.toolbarParentStudentProfile)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = "Student Profile"
        }

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            currentParent = authManager.getLoggedInUser()

            if (currentParent == null || currentParent!!.role != User.ROLE_PARENT) {
                Toast.makeText(this@ParentStudentProfileActivity, "Access Denied: Only parents can view student profiles.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            // Initialize views and setup ViewModel/RecyclerView AFTER currentParent is loaded and validated
            initializeViews()
            setupViewModel()
            setupChildSpinner() // Call this here to ensure children data is ready for the spinner
        }
    }

    private fun initializeViews() {
        spinnerSelectChild = findViewById(R.id.spinnerSelectChild)
        textViewNoChildren = findViewById(R.id.textViewNoChildren)

        textViewStudentName = findViewById(R.id.textViewStudentName)
        textViewAdmissionNo = findViewById(R.id.textViewAdmissionNo)
        textViewDateOfBirth = findViewById(R.id.textViewDateOfBirth)
        textViewGender = findViewById(R.id.textViewGender)
        textViewGradeClass = findViewById(R.id.textViewGradeClass)
        textViewAdmissionDate = findViewById(R.id.textViewAdmissionDate)
        textViewSchoolName = findViewById(R.id.textViewSchoolName)
        textViewAddress = findViewById(R.id.textViewAddress)
        textViewNotes = findViewById(R.id.textViewNotes)
        imageViewProfile = findViewById(R.id.imageViewProfile)

        // Hide profile details container initially
        findViewById<View>(R.id.profile_details_container).visibility = View.GONE

        childrenList = java.util.ArrayList()
        childNameToIdMap = java.util.HashMap() // Initialized with String as value type
    }

    private fun setupViewModel() {
        // AppDatabase.getDatabase is not used for DAOs directly
        val studentRepository = StudentRepository()
        val userRepository = UserRepository()

        val factory = ParentStudentProfileViewModelFactory(studentRepository, userRepository)
        parentStudentProfileViewModel = ViewModelProvider(this, factory)[ParentStudentProfileViewModel::class.java]

        currentParent?.id?.let { parentId ->
            parentStudentProfileViewModel.setParentUserId(parentId) // parentId is String
        }

        // Observe children of parent for the spinner
        parentStudentProfileViewModel.childrenOfParent.observe(this) { children ->
            childrenList.clear()
            childrenList.addAll(children)
            childNameToIdMap.clear()
            val childNames = ArrayList<String>()

            if (childrenList.isNotEmpty()) {
                for (child in childrenList) {
                    val name = child.fullName
                    childNames.add(name)
                    childNameToIdMap[name] = child.id // child.id is String
                }
                textViewNoChildren.visibility = View.GONE
                spinnerSelectChild.visibility = View.VISIBLE
                findViewById<View>(R.id.profile_details_container).visibility = View.VISIBLE

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, childNames)
                adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
                spinnerSelectChild.adapter = adapter

                // Initially display the first child's profile if available
                if (childrenList.isNotEmpty() && parentStudentProfileViewModel.selectedStudentProfile.value == null) {
                    spinnerSelectChild.setSelection(0)
                    parentStudentProfileViewModel.selectChild(childrenList[0].id) // childrenList[0].id is String
                }

            } else {
                Log.d(TAG, "No children found for parent: ${currentParent?.fullName}")
                textViewNoChildren.text = "No children linked to your account."
                textViewNoChildren.visibility = View.VISIBLE
                spinnerSelectChild.visibility = View.GONE
                findViewById<View>(R.id.profile_details_container).visibility = View.GONE
            }
        }

        // Observe selected student profile from ViewModel
        parentStudentProfileViewModel.selectedStudentProfile.observe(this) { student ->
            if (student != null) {
                displayStudentProfile(student)
                findViewById<View>(R.id.profile_details_container).visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Student profile not found.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "displayStudentProfile: Selected student not found in ViewModel.")
                findViewById<View>(R.id.profile_details_container).visibility = View.GONE
            }
        }

        // Observe error messages from ViewModel (if any actions were added to ViewModel later)
        parentStudentProfileViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                parentStudentProfileViewModel.clearErrorMessage()
            }
        }
    }


    private fun displayStudentProfile(student: Student) {
        textViewStudentName.text = "Full Name: ${student.fullName}"
        textViewAdmissionNo.text = "Admission No: ${student.admissionNumber ?: "N/A"}"
        textViewDateOfBirth.text = "DOB: ${student.dateOfBirth ?: "N/A"}"
        textViewGender.text = "Gender: ${student.gender ?: "N/A"}"
        textViewGradeClass.text = "Grade/Class: ${student.gradeOrClass ?: "N/A"}"
        textViewAdmissionDate.text = "Admission Date: ${student.admissionDate ?: "N/A"}"
        textViewSchoolName.text = "School: ${student.schoolName ?: "N/A"}"
        textViewAddress.text = "Address: ${student.address ?: "N/A"}"
        textViewNotes.text = "Notes: ${student.notes ?: "N/A"}"

        val profileImagePath = student.profileImagePath
        if (profileImagePath != null && profileImagePath.isNotEmpty()) {
            imageViewProfile.setImageResource(R.drawable.ic_person_placeholder) // Placeholder, should load actual image
        } else {
            imageViewProfile.setImageResource(R.drawable.ic_person_placeholder)
        }
        Log.d(TAG, "Displayed profile for: ${student.fullName}")
    }

    private fun setupChildSpinner() {
        spinnerSelectChild.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedChildName = parent.getItemAtPosition(position) as String
                val selectedChildId = childNameToIdMap[selectedChildName] // This is now String?
                if (selectedChildId != null) {
                    parentStudentProfileViewModel.selectChild(selectedChildId) // selectedChildId is String
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
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