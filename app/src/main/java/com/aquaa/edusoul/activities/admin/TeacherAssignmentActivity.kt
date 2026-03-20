package com.aquaa.edusoul.activities.admin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.TeacherAssignmentAdapter
// import com.aquaa.edusoul.database.AppDatabase // This import can likely be removed too if not used elsewhere in this file
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.TeacherAssignmentDetails
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.viewmodels.TeacherAssignmentViewModel
import com.aquaa.edusoul.viewmodels.TeacherAssignmentViewModelFactory
import java.util.ArrayList

class TeacherAssignmentActivity : BaseActivity(), TeacherAssignmentAdapter.OnAssignmentRemoveListener {

    private val TAG = "TeacherAssignment"

    private lateinit var toolbar: Toolbar
    private lateinit var textViewAssigningFor: TextView
    private lateinit var spinnerAssignSubject: Spinner
    private lateinit var spinnerAssignBatch: Spinner
    private lateinit var buttonAssignTeacher: Button
    private lateinit var textViewAssignedStatus: TextView // New UI element
    private lateinit var imageButtonRemoveAssignment: ImageButton // New UI element
    private lateinit var recyclerViewAssignments: RecyclerView
    private lateinit var textViewNoAssignments: TextView

    private lateinit var assignmentAdapter: TeacherAssignmentAdapter
    private lateinit var subjectListForSpinner: List<Subject>
    private lateinit var batchListForSpinner: List<Batch>

    private var currentTeacherId: String = ""
    private var currentTeacherName: String = ""

    private lateinit var teacherAssignmentViewModel: TeacherAssignmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_assignment)

        toolbar = findViewById(R.id.toolbarTeacherAssignment)
        setSupportActionBar(toolbar)

        if (intent.extras != null) {
            currentTeacherId = intent.getStringExtra("TEACHER_ID") ?: ""
            currentTeacherName = intent.getStringExtra("TEACHER_NAME") ?: ""
        }

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Assign: $currentTeacherName"
        }

        if (currentTeacherId.isBlank()) {
            Toast.makeText(this, "Error: Teacher ID not found.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Teacher ID not passed to TeacherAssignmentActivity.")
            finish()
            return
        }

        // Initialize Repositories
        val subjectRepository = SubjectRepository()
        val batchRepository = BatchRepository()
        val teacherSubjectBatchLinkRepository = TeacherSubjectBatchLinkRepository()

        // Setup ViewModel
        val factory = TeacherAssignmentViewModelFactory(subjectRepository, batchRepository, teacherSubjectBatchLinkRepository)
        teacherAssignmentViewModel = ViewModelProvider(this, factory)[TeacherAssignmentViewModel::class.java]
        teacherAssignmentViewModel.setCurrentTeacherId(currentTeacherId)

        // Find UI elements
        textViewAssigningFor = findViewById(R.id.textViewAssigningFor)
        spinnerAssignSubject = findViewById(R.id.spinnerAssignSubject)
        spinnerAssignBatch = findViewById(R.id.spinnerAssignBatch)
        buttonAssignTeacher = findViewById(R.id.buttonAssignTeacher)
        textViewAssignedStatus = findViewById(R.id.textViewAssignedStatus) // Initialize new TextView
        imageButtonRemoveAssignment = findViewById(R.id.imageButtonRemoveAssignment) // Initialize new ImageButton
        recyclerViewAssignments = findViewById(R.id.recyclerViewTeacherAssignments)
        textViewNoAssignments = findViewById(R.id.textViewNoAssignments)

        textViewAssigningFor.text = "Assigning Subjects/Batches for: $currentTeacherName"

        setupRecyclerView()
        setupSpinnersAndObservers()

        // Set Listeners
        buttonAssignTeacher.setOnClickListener { assignNewSubjectAndBatch() }
        imageButtonRemoveAssignment.setOnClickListener { removeCurrentAssignment() } // Set listener for new button
    }

    private fun setupRecyclerView() {
        assignmentAdapter = TeacherAssignmentAdapter(this, mutableListOf(), this)
        recyclerViewAssignments.layoutManager = LinearLayoutManager(this)
        recyclerViewAssignments.adapter = assignmentAdapter
    }

    private fun setupSpinnersAndObservers() {
        teacherAssignmentViewModel.allSubjects.observe(this) { subjects ->
            subjectListForSpinner = subjects
            val subjectAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(subjectListForSpinner))
            subjectAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAssignSubject.adapter = subjectAdapterSpinner
            // Trigger status check when subjects load or selection changes
            spinnerAssignSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    checkAssignmentStatusForSelectedItems()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    checkAssignmentStatusForSelectedItems()
                }
            }
        }

        teacherAssignmentViewModel.allBatches.observe(this) { batches ->
            batchListForSpinner = batches
            val batchAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(batchListForSpinner))
            batchAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAssignBatch.adapter = batchAdapterSpinner
            // Trigger status check when batches load or selection changes
            spinnerAssignBatch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    checkAssignmentStatusForSelectedItems()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    checkAssignmentStatusForSelectedItems()
                }
            }
        }

        teacherAssignmentViewModel.teacherAssignments.observe(this) { assignments ->
            assignmentAdapter.setAssignments(assignments)
            textViewNoAssignments.visibility = if (assignments.isEmpty()) View.VISIBLE else View.GONE
            // Re-check assignment status whenever the list of assignments updates
            checkAssignmentStatusForSelectedItems()
        }

        teacherAssignmentViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                teacherAssignmentViewModel.clearErrorMessage()
            }
        }

        // Observe the new LiveData for current selection assignment status
        teacherAssignmentViewModel.isCurrentSelectionAssigned.observe(this) { isAssigned ->
            updateAssignmentButtonState(isAssigned)
        }
    }

    private fun checkAssignmentStatusForSelectedItems() {
        val selectedSubject = spinnerAssignSubject.selectedItem as? Subject
        val selectedBatch = spinnerAssignBatch.selectedItem as? Batch
        val teacherId = teacherAssignmentViewModel.currentTeacherId.value ?: ""

        if (selectedSubject != null && selectedBatch != null && teacherId.isNotBlank()) {
            teacherAssignmentViewModel.checkCurrentSelectionAssignment(teacherId, selectedSubject.id, selectedBatch.id)
        } else {
            // If selection is incomplete, default to not assigned state
            updateAssignmentButtonState(false)
        }
    }

    private fun updateAssignmentButtonState(isAssigned: Boolean) {
        if (isAssigned) {
            buttonAssignTeacher.visibility = View.GONE
            textViewAssignedStatus.visibility = View.VISIBLE
            imageButtonRemoveAssignment.visibility = View.VISIBLE
        } else {
            buttonAssignTeacher.visibility = View.VISIBLE
            textViewAssignedStatus.visibility = View.GONE
            imageButtonRemoveAssignment.visibility = View.GONE
        }
    }

    private fun assignNewSubjectAndBatch() {
        val selectedSubject = spinnerAssignSubject.selectedItem as? Subject
        val selectedBatch = spinnerAssignBatch.selectedItem as? Batch

        if (selectedSubject == null || selectedBatch == null) {
            Toast.makeText(this, "Please select both a subject and a batch.", Toast.LENGTH_SHORT).show()
            return
        }

        // Pass String IDs to ViewModel
        teacherAssignmentViewModel.assignTeacherToSubjectAndBatch(currentTeacherId, selectedSubject.id, selectedBatch.id)
    }

    private fun removeCurrentAssignment() {
        val selectedSubject = spinnerAssignSubject.selectedItem as? Subject
        val selectedBatch = spinnerAssignBatch.selectedItem as? Batch
        val teacherId = teacherAssignmentViewModel.currentTeacherId.value ?: ""

        if (selectedSubject == null || selectedBatch == null || teacherId.isBlank()) {
            Toast.makeText(this, "No assignment selected to remove.", Toast.LENGTH_SHORT).show()
            return
        }

        // Construct the composite ID to remove the specific assignment
        val assignmentIdToRemove = "${teacherId}_${selectedSubject.id}_${selectedBatch.id}"

        AlertDialog.Builder(this)
            .setTitle("Remove Current Assignment")
            .setMessage("Are you sure you want to remove the assignment of '${currentTeacherName}' to '${selectedSubject.subjectName}' in '${selectedBatch.batchName}'?")
            .setPositiveButton("Remove") { _, _ ->
                teacherAssignmentViewModel.removeTeacherAssignment(assignmentIdToRemove)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRemoveAssignment(assignment: TeacherAssignmentDetails, position: Int) {
        // This is for removing from the list below, keep its original logic
        AlertDialog.Builder(this)
            .setTitle("Remove Assignment")
            .setMessage("Are you sure you want to remove this assignment?\n(${assignment.subjectName} - ${assignment.batchName})")
            .setPositiveButton("Remove") { _, _ ->
                teacherAssignmentViewModel.removeTeacherAssignment(assignment.assignmentId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}