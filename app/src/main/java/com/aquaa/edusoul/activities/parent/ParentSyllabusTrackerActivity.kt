// File: main/java/com/aquaa/edusoul/activities/parent/ParentSyllabusTrackerActivity.kt
package com.aquaa.edusoul.activities.parent

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.ParentSyllabusProgressAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.ParentSyllabusProgressDetails
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.repositories.SyllabusProgressRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ParentSyllabusViewModel
import com.aquaa.edusoul.viewmodels.ParentSyllabusViewModelFactory
import java.util.ArrayList
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParentSyllabusTrackerActivity : AppCompatActivity(), ParentSyllabusProgressAdapter.OnItemClickListener {

    private val TAG = "ParentSyllabusTrackerActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var spinnerSelectChild: Spinner
    private lateinit var textViewNoTopics: TextView

    private lateinit var recyclerViewSyllabusProgress: RecyclerView
    private lateinit var parentSyllabusProgressAdapter: ParentSyllabusProgressAdapter
    private lateinit var syllabusProgressDetailsList: MutableList<ParentSyllabusProgressDetails>

    private lateinit var authManager: AuthManager
    private lateinit var currentParent: User
    private var childrenListForSpinner: MutableList<Student> = mutableListOf()
    private val childNameToIdMap: MutableMap<String, String> = mutableMapOf() // Changed value type to String
    private var selectedChildId: String = "" // Changed type to String

    private lateinit var parentSyllabusViewModel: ParentSyllabusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_syllabus_tracker)

        toolbar = findViewById(R.id.toolbarParentSyllabusTracker)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = "Syllabus Progress Tracker"
        }

        // AppDatabase.getDatabase is not directly used for DAOs
        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            currentParent = authManager.getLoggedInUser()
                ?: run {
                    Toast.makeText(this@ParentSyllabusTrackerActivity, "Access Denied: Parent user not identified.", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }

            if (currentParent.role != User.ROLE_PARENT) {
                Toast.makeText(this@ParentSyllabusTrackerActivity, "Access Denied: Only parents can view syllabus progress.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            val studentRepository = StudentRepository()
            val subjectRepository = SubjectRepository()
            val batchRepository = BatchRepository()
            val syllabusTopicRepository = SyllabusTopicRepository()
            val syllabusProgressRepository = SyllabusProgressRepository()
            val userRepository = UserRepository()

            val factory = ParentSyllabusViewModelFactory(
                studentRepository,
                subjectRepository,
                batchRepository,
                syllabusTopicRepository,
                syllabusProgressRepository,
                userRepository
            )
            parentSyllabusViewModel = ViewModelProvider(this@ParentSyllabusTrackerActivity, factory)[ParentSyllabusViewModel::class.java]

            initializeViews()
            setupRecyclerView()
            setupChildSpinner()

            // Set the parent user ID (now String) in the ViewModel to trigger loading of children
            parentSyllabusViewModel.setParentUserId(currentParent.id) // currentParent.id is String

            parentSyllabusViewModel.childrenOfParent.observe(this@ParentSyllabusTrackerActivity) { children ->
                childrenListForSpinner.clear()
                childNameToIdMap.clear()

                if (children.isEmpty()) {
                    Log.d(TAG, "No children found for parent: ${currentParent.fullName}")
                    textViewNoTopics.text = "No children linked to your account or no syllabus topics assigned."
                    textViewNoTopics.visibility = View.VISIBLE
                    spinnerSelectChild.visibility = View.GONE
                    findViewById<TextView>(R.id.textViewSelectChildLabel).visibility = View.GONE
                } else {
                    childrenListForSpinner.addAll(children)
                    val childNames = children.map { it.fullName }
                    children.forEach { child -> childNameToIdMap[child.fullName] = child.id } // child.id is String

                    val adapter = ArrayAdapter(this@ParentSyllabusTrackerActivity, android.R.layout.simple_spinner_item, childNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerSelectChild.adapter = adapter

                    textViewNoTopics.visibility = View.GONE
                    spinnerSelectChild.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.textViewSelectChildLabel).visibility = View.VISIBLE

                    if (children.isNotEmpty() && parentSyllabusViewModel.syllabusProgressDetails.value.isNullOrEmpty()) {
                        spinnerSelectChild.setSelection(0)
                        parentSyllabusViewModel.selectChild(children[0].id) // children[0].id is String
                    }
                }
            }

            parentSyllabusViewModel.syllabusProgressDetails.observe(this@ParentSyllabusTrackerActivity) { progressDetails ->
                syllabusProgressDetailsList.clear()
                if (!progressDetails.isNullOrEmpty()) {
                    syllabusProgressDetailsList.addAll(progressDetails)
                    textViewNoTopics.visibility = View.GONE
                    recyclerViewSyllabusProgress.visibility = View.VISIBLE
                    Log.d(TAG, "Found ${progressDetails.size} syllabus topics for selected child.")
                } else {
                    textViewNoTopics.text = "No syllabus topics or progress records found for this child."
                    textViewNoTopics.visibility = View.VISIBLE
                    recyclerViewSyllabusProgress.visibility = View.GONE
                    Log.d(TAG, "No syllabus topics found for selected child.")
                }
                parentSyllabusProgressAdapter.updateData(syllabusProgressDetailsList)
            }

            parentSyllabusViewModel.errorMessage.observe(this@ParentSyllabusTrackerActivity) { message ->
                message?.let {
                    Toast.makeText(this@ParentSyllabusTrackerActivity, it, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error: $it")
                    parentSyllabusViewModel.clearErrorMessage()
                }
            }

            parentSyllabusViewModel.isLoading.observe(this@ParentSyllabusTrackerActivity) { isLoading ->
                Log.d(TAG, "Loading state: $isLoading")
                // Optionally show/hide a progress bar
            }
        }
    }

    private fun initializeViews() {
        spinnerSelectChild = findViewById(R.id.spinnerSelectChild)
        textViewNoTopics = findViewById(R.id.textViewNoTopics)
        recyclerViewSyllabusProgress = findViewById(R.id.recyclerViewSyllabusProgress)

        syllabusProgressDetailsList = ArrayList()
    }

    private fun setupRecyclerView() {
        parentSyllabusProgressAdapter = ParentSyllabusProgressAdapter(this, syllabusProgressDetailsList, this)
        recyclerViewSyllabusProgress.layoutManager = LinearLayoutManager(this)
        recyclerViewSyllabusProgress.adapter = parentSyllabusProgressAdapter
    }

    private fun setupChildSpinner() {
        spinnerSelectChild.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedChildName = parent.getItemAtPosition(position) as String
                this@ParentSyllabusTrackerActivity.selectedChildId = childNameToIdMap[selectedChildName] ?: "" // Retrieve String ID
                if (this@ParentSyllabusTrackerActivity.selectedChildId.isNotBlank()) { // Check for blank String ID
                    parentSyllabusViewModel.selectChild(this@ParentSyllabusTrackerActivity.selectedChildId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::currentParent.isInitialized && currentParent.id.isNotBlank()) { // currentParent.id is String
            parentSyllabusViewModel.setParentUserId(currentParent.id) // currentParent.id is String
            if (this.selectedChildId.isNotBlank()) { // selectedChildId is String
                parentSyllabusViewModel.selectChild(this.selectedChildId)
            } else if (childrenListForSpinner.isNotEmpty()) {
                parentSyllabusViewModel.selectChild(childrenListForSpinner[0].id) // children[0].id is String
            }
        }
    }

    override fun onSyllabusTopicCardClick(progressDetails: ParentSyllabusProgressDetails) {
        Toast.makeText(this, "Clicked on Topic: ${progressDetails.topicName} - Status: ${if (progressDetails.isCompleted) "Completed" else "Pending"}", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Syllabus topic card clicked: ${progressDetails.topicName}")
    }

    @Override
    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}