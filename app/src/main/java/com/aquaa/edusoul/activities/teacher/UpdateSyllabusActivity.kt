// File: main/java/com/aquaa/edusoul/activities/teacher/UpdateSyllabusActivity.kt
package com.aquaa.edusoul.activities.teacher

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.SyllabusProgressAdapter
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.SyllabusTopic
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.repositories.SyllabusProgressRepository
import com.aquaa.edusoul.viewmodels.SyllabusTopicViewModel
import com.aquaa.edusoul.viewmodels.SyllabusTopicViewModelFactory
import com.aquaa.edusoul.auth.AuthManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import androidx.lifecycle.asLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext

class UpdateSyllabusActivity : BaseActivity() {

    private val TAG = "UpdateSyllabusActivity"

    private lateinit var spinnerBatch: Spinner
    private lateinit var spinnerSubject: Spinner
    private lateinit var recyclerViewSyllabus: RecyclerView
    private lateinit var buttonSaveChanges: Button
    private lateinit var textViewNoTopics: TextView

    private lateinit var syllabusTopicViewModel: SyllabusTopicViewModel
    private lateinit var syllabusProgressAdapter: SyllabusProgressAdapter

    // Changed IDs from Long to String
    private var currentTeacherId: String = ""
    private var selectedBatchId: String = ""
    private var selectedSubjectId: String = ""

    private var initialSubjectId: String? = null // For pre-selection from intent
    private var initialBatchId: String? = null // For pre-selection from intent

    // Constants for Intent Extras
    companion object {
        const val EXTRA_SUBJECT_ID = "SUBJECT_ID"
        const val EXTRA_BATCH_ID = "BATCH_ID"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_syllabus)

        val toolbar = findViewById<Toolbar>(R.id.toolbarUpdateSyllabus)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get pre-selection data from intent
        initialSubjectId = intent.getStringExtra(EXTRA_SUBJECT_ID)
        initialBatchId = intent.getStringExtra(EXTRA_BATCH_ID)


        val applicationContext = application
        // REMOVED: val appDatabase = AppDatabase.getDatabase(applicationContext) // This line caused the crash

        val syllabusTopicRepository = SyllabusTopicRepository()
        val syllabusProgressRepository = SyllabusProgressRepository()
        val batchRepository = BatchRepository()
        val subjectRepository = SubjectRepository()
        val userRepository = UserRepository()

        val factory = SyllabusTopicViewModelFactory(syllabusTopicRepository, syllabusProgressRepository)
        syllabusTopicViewModel = ViewModelProvider(this, factory)[SyllabusTopicViewModel::class.java]

        val authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            val currentTeacher = authManager.getLoggedInUser()
            if (currentTeacher == null || currentTeacher.id.isBlank()) { // Check for blank String ID
                Toast.makeText(this@UpdateSyllabusActivity, "Error: Could not identify teacher.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            currentTeacherId = currentTeacher.id

            spinnerBatch = findViewById(R.id.spinnerSyllabusBatch)
            spinnerSubject = findViewById(R.id.spinnerSyllabusSubject)
            recyclerViewSyllabus = findViewById(R.id.recyclerViewSyllabusProgress)
            buttonSaveChanges = findViewById(R.id.buttonSaveChanges)
            textViewNoTopics = findViewById(R.id.textViewNoSyllabusTopics)

            syllabusProgressAdapter = SyllabusProgressAdapter(this@UpdateSyllabusActivity, mutableListOf())
            recyclerViewSyllabus.layoutManager = LinearLayoutManager(this@UpdateSyllabusActivity)
            recyclerViewSyllabus.adapter = syllabusProgressAdapter

            setupSpinnersAndObservers(batchRepository, subjectRepository)
            setupEventListeners()
        }
    }

    private fun setupSpinnersAndObservers(batchRepository: BatchRepository, subjectRepository: SubjectRepository) {
        // Use currentTeacherId directly as String
        batchRepository.getBatchesForTeacher(currentTeacherId).asLiveData().observe(this) { batches ->
            val displayList = if (batches.isNotEmpty()) {
                batches
            } else {
                // Use empty string for ID in dummy object
                listOf(Batch("", "No Batches Available", null, null, null, null))
            }
            val batchAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayList)
            batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerBatch.adapter = batchAdapter

            // Attempt to pre-select batch if provided via intent
            if (initialBatchId != null && initialBatchId!!.isNotBlank()) {
                val positionToSelect = displayList.indexOfFirst { it.id == initialBatchId }
                if (positionToSelect != -1) {
                    spinnerBatch.setSelection(positionToSelect)
                    selectedBatchId = displayList[positionToSelect].id // Set selectedBatchId immediately
                }
            } else if (displayList.isNotEmpty() && displayList[0].id.isNotBlank()) {
                // If no pre-selection, default to first valid batch if available
                spinnerBatch.setSelection(0)
                selectedBatchId = displayList[0].id
            }
            // Manually trigger subject loading based on selected batch
            if (selectedBatchId.isNotBlank()) {
                loadSubjectsForSelectedBatch(selectedBatchId, subjectRepository)
            } else {
                clearSubjectSpinner()
                clearSyllabusList()
            }
        }

        spinnerBatch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedBatch = parent.getItemAtPosition(position) as Batch
                selectedBatchId = selectedBatch.id
                if (selectedBatchId.isNotBlank()) {
                    loadSubjectsForSelectedBatch(selectedBatchId, subjectRepository)
                } else {
                    clearSubjectSpinner()
                    clearSyllabusList()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                clearSubjectSpinner()
                clearSyllabusList()
            }
        }

        spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSubject = parent.getItemAtPosition(position) as Subject
                selectedSubjectId = selectedSubject.id

                if (selectedBatchId.isNotBlank() && selectedSubjectId.isNotBlank()) {
                    syllabusTopicViewModel.selectSubjectAndBatch(selectedSubjectId, selectedBatchId)
                } else {
                    clearSyllabusList()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                clearSyllabusList()
            }
        }

        syllabusTopicViewModel.syllabusTopicsWithStatus.observe(this) { topicsWithStatus ->
            syllabusProgressAdapter.setTopicStatusList(topicsWithStatus.toMutableList())

            if (syllabusProgressAdapter.itemCount == 0) {
                textViewNoTopics.text = "No syllabus topics defined for this subject."
                textViewNoTopics.visibility = View.VISIBLE
                recyclerViewSyllabus.visibility = View.GONE
                buttonSaveChanges.visibility = View.GONE
            } else {
                textViewNoTopics.visibility = View.GONE
                recyclerViewSyllabus.visibility = View.VISIBLE
                buttonSaveChanges.visibility = View.VISIBLE
            }
        }
    }

    private fun loadSubjectsForSelectedBatch(batchId: String, subjectRepository: SubjectRepository) {
        subjectRepository.getSubjectsForTeacherAndBatch(currentTeacherId, batchId).asLiveData().observe(this) { subjects ->
            val subjectDisplayList = if (subjects.isNotEmpty()) {
                subjects
            } else {
                listOf(Subject("", "No Assigned Subjects", null, null))
            }
            val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjectDisplayList)
            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSubject.adapter = subjectAdapter

            // Attempt to pre-select subject if provided via intent
            if (initialSubjectId != null && initialSubjectId!!.isNotBlank()) {
                val positionToSelect = subjectDisplayList.indexOfFirst { it.id == initialSubjectId }
                if (positionToSelect != -1) {
                    spinnerSubject.setSelection(positionToSelect)
                    selectedSubjectId = subjectDisplayList[positionToSelect].id // Set selectedSubjectId immediately
                }
                // Clear initialSubjectId after attempting to use it to prevent re-selection on subsequent changes
                initialSubjectId = null
            } else if (subjectDisplayList.isNotEmpty() && subjectDisplayList[0].id.isNotBlank()) {
                // If no pre-selection, default to first valid subject if available
                spinnerSubject.setSelection(0)
                selectedSubjectId = subjectDisplayList[0].id
            }

            // Manually trigger topic loading based on selected subject/batch
            if (selectedBatchId.isNotBlank() && selectedSubjectId.isNotBlank()) {
                syllabusTopicViewModel.selectSubjectAndBatch(selectedSubjectId, selectedBatchId)
            } else {
                clearSyllabusList()
            }
        }
    }


    private fun setupEventListeners() {
        buttonSaveChanges.setOnClickListener { saveProgress() }
    }

    private fun clearSubjectSpinner() {
        // Use empty string for ID in dummy object
        val emptySubjectList = listOf(Subject("", "No Subjects Available", null, null))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, emptySubjectList)
        spinnerSubject.adapter = adapter
    }

    private fun clearSyllabusList() {
        syllabusProgressAdapter.setTopicStatusList(mutableListOf())
        textViewNoTopics.text = "Select a batch and subject to view syllabus topics."
        textViewNoTopics.visibility = View.VISIBLE
        recyclerViewSyllabus.visibility = View.GONE
        buttonSaveChanges.visibility = View.GONE
    }

    private fun saveProgress() {
        // Check if IDs are blank instead of <= 0
        if (selectedBatchId.isBlank() || selectedSubjectId.isBlank()) {
            Toast.makeText(this, "Please select a valid batch and subject.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentStatusList = syllabusProgressAdapter.getTopicStatusList()
        var changesMade = false

        for (status in currentStatusList) {
            val (topic, isCompleted) = status

            // Pass String IDs to ViewModel
            syllabusTopicViewModel.updateSyllabusProgress(
                topicId = topic.id, // topic.id is String
                batchId = selectedBatchId, // selectedBatchId is String
                isCompleted = isCompleted,
                updatedByTeacherId = currentTeacherId // currentTeacherId is String
            )
            changesMade = true
        }

        if (changesMade) {
            Toast.makeText(this, "Syllabus progress updates dispatched.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}