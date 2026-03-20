package com.aquaa.edusoul.activities.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.ExamAdapter
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.viewmodels.ManageExamsViewModel
import com.aquaa.edusoul.viewmodels.ManageExamsViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.ArrayList
import com.aquaa.edusoul.utils.AiAssistantConstants
import android.os.Handler
import android.os.Looper
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.auth.AuthManager // Import AuthManager
import com.aquaa.edusoul.models.User // Import User model
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext
import com.aquaa.edusoul.repositories.UserRepository // Import UserRepository

class ManageExamsActivity : BaseActivity(), ExamAdapter.OnExamActionsListener {

    private val TAG = "ManageExamsActivity"

    private lateinit var recyclerViewExams: RecyclerView
    private lateinit var fabAddExam: FloatingActionButton
    private lateinit var textViewNoExams: TextView
    private lateinit var examAdapter: ExamAdapter

    private var subjectListLiveData: List<Subject> = emptyList()
    private var batchListLiveData: List<Batch> = emptyList()
    private var teacherListLiveData: List<User> = emptyList() // To hold fetched teachers

    private lateinit var manageExamsViewModel: ManageExamsViewModel
    private lateinit var subjectRepository: SubjectRepository
    private lateinit var batchRepository: BatchRepository
    private lateinit var userRepository: UserRepository // Declare UserRepository
    private lateinit var authManager: AuthManager // Declare AuthManager
    private var currentAdminUser: User? = null // Renamed for clarity, it's the logged-in admin

    private var shouldAutoSaveOnPrefill = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_exams)

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageExams)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this) // Initialize AuthManager

        // Fetch current user in a coroutine
        lifecycleScope.launch(Dispatchers.Main) {
            currentAdminUser = authManager.getLoggedInUser()
            if (currentAdminUser == null) {
                Toast.makeText(this@ManageExamsActivity, "Error: User not identified. Please log in again.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }
            // Now that currentAdminUser is loaded, proceed with ViewModel and UI setup that depends on it
            setupViewModelAndUI()
        }
    }

    private fun setupViewModelAndUI() {
        val examRepository = ExamRepository()
        subjectRepository = SubjectRepository()
        batchRepository = BatchRepository()
        userRepository = UserRepository() // Initialize UserRepository
        val factory = ManageExamsViewModelFactory(examRepository, subjectRepository, batchRepository, userRepository) // Pass userRepository
        manageExamsViewModel = ViewModelProvider(this, factory)[ManageExamsViewModel::class.java]

        recyclerViewExams = findViewById(R.id.recyclerViewExams)
        fabAddExam = findViewById(R.id.fabAddExam)
        textViewNoExams = findViewById(R.id.textViewNoExams)

        examAdapter = ExamAdapter(this, mutableListOf(), this,
            getSubjectNameById = { subjectId ->
                subjectId?.let { sId ->
                    runBlocking { subjectRepository.getSubjectById(sId)?.subjectName }
                } ?: "N/A"
            },
            getBatchNameById = { batchId ->
                batchId?.let { bId ->
                    runBlocking { batchRepository.getBatchById(bId)?.batchName }
                } ?: "N/A"
            }
        )

        recyclerViewExams.layoutManager = LinearLayoutManager(this)
        recyclerViewExams.adapter = examAdapter

        fabAddExam.setOnClickListener { showAddEditExamDialog(null) }

        setupSpinnersAndObservers()

        if (intent.getBooleanExtra(AiAssistantConstants.EXTRA_PREFILL_DATA, false)) {
            shouldAutoSaveOnPrefill = true

            val examName = intent.getStringExtra(AiAssistantConstants.EXTRA_EXAM_NAME)
            val subjectName = intent.getStringExtra(AiAssistantConstants.EXTRA_EXAM_SUBJECT)
            val batchName = intent.getStringExtra(AiAssistantConstants.EXTRA_EXAM_BATCH)
            val examDate = intent.getStringExtra(AiAssistantConstants.EXTRA_EXAM_DATE)
            val maxMarks = intent.getIntExtra(AiAssistantConstants.EXTRA_EXAM_MAX_MARKS, 0)

            val prefillBundle = Bundle().apply {
                putString(AiAssistantConstants.EXTRA_EXAM_NAME, examName)
                putString(AiAssistantConstants.EXTRA_EXAM_SUBJECT, subjectName)
                putString(AiAssistantConstants.EXTRA_EXAM_BATCH, batchName)
                putString(AiAssistantConstants.EXTRA_EXAM_DATE, examDate)
                putInt(AiAssistantConstants.EXTRA_EXAM_MAX_MARKS, maxMarks)
            }
            // This will be handled by checkAndShowPrefillDialog once data is loaded
        }
    }

    private fun setupSpinnersAndObservers() {
        manageExamsViewModel.allSubjects.observe(this) { subjects ->
            subjectListLiveData = subjects
            // Trigger check only when all necessary data for prefill dialog might be ready
            checkAndShowPrefillDialog()
        }

        manageExamsViewModel.allBatches.observe(this) { batches ->
            batchListLiveData = batches
            // Trigger check only when all necessary data for prefill dialog might be ready
            checkAndShowPrefillDialog()
        }

        manageExamsViewModel.allTeachers.observe(this) { teachers ->
            teacherListLiveData = teachers
            // Trigger check only when all necessary data for prefill dialog might be ready
            checkAndShowPrefillDialog()
        }

        manageExamsViewModel.allExams.observe(this) { examsFromDb ->
            examAdapter.setExams(examsFromDb)
            textViewNoExams.visibility = if (examsFromDb.isEmpty()) View.VISIBLE else View.GONE
        }

        manageExamsViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                manageExamsViewModel.clearErrorMessage()
            }
        }
    }

    private fun checkAndShowPrefillDialog() {
        // Only proceed if prefill is intended and all necessary spinner data is loaded
        // Now include teacherListLiveData check
        if (shouldAutoSaveOnPrefill && subjectListLiveData.isNotEmpty() && batchListLiveData.isNotEmpty() && teacherListLiveData.isNotEmpty()) {
            val prefillBundle = intent.extras
            // Add a flag to prevent the dialog from showing multiple times if observers trigger
            if (prefillBundle != null && !prefillBundle.getBoolean("dialogShown", false)) {
                prefillBundle.putBoolean("dialogShown", true)
                showAddEditExamDialog(null, prefillBundle)
            }
        }
    }

    private fun showAddEditExamDialog(examToEdit: Exam?, prefillData: Bundle? = null) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_exam, null)
        builder.setView(dialogView)

        val tilExamName = dialogView.findViewById<TextInputLayout>(R.id.tilExamNameDialog)
        val editTextExamName = dialogView.findViewById<EditText>(R.id.editTextExamNameDialog)
        val spinnerSubjectDialog = dialogView.findViewById<Spinner>(R.id.spinnerExamSubjectDialog)
        val spinnerBatchDialog = dialogView.findViewById<Spinner>(R.id.spinnerExamBatchDialog)
        val spinnerTeacherDialog = dialogView.findViewById<Spinner>(R.id.spinnerExamTeacherDialog) // Find the new teacher spinner
        val editTextExamDate = dialogView.findViewById<TextInputEditText>(R.id.editTextExamDateDialog)
        val tilMaxMarks = dialogView.findViewById<TextInputLayout>(R.id.tilMaxMarksDialog)
        val editTextMaxMarks = dialogView.findViewById<EditText>(R.id.editTextMaxMarksDialog)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextExamDescriptionDialog)

        val subjectAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(subjectListLiveData))
        subjectAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSubjectDialog.adapter = subjectAdapterSpinner

        val batchAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(batchListLiveData))
        batchAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBatchDialog.adapter = batchAdapterSpinner

        // New: Teacher Spinner Adapter
        val teacherAdapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(teacherListLiveData))
        teacherAdapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTeacherDialog.adapter = teacherAdapterSpinner


        editTextExamDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, dayOfMonth ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.set(year, month, dayOfMonth)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    editTextExamDate.setText(sdf.format(selectedCalendar.time))
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        builder.setTitle(if (examToEdit == null) "Add New Exam" else "Edit Exam")

        var prefilledSubjectPosition = 0
        var prefilledBatchPosition = 0
        var prefilledTeacherPosition = 0 // For prefilling teacher
        var prefilledAllRequired = false

        if (examToEdit != null) {
            editTextExamName.setText(examToEdit.examName)
            editTextExamDate.setText(examToEdit.examDate)
            editTextMaxMarks.setText(examToEdit.maxMarks.toString())
            editTextDescription.setText(examToEdit.description)

            // Corrected: Compare String IDs directly
            prefilledSubjectPosition = subjectListLiveData.indexOfFirst { it.id == examToEdit.subjectId }.coerceAtLeast(0)
            if (prefilledSubjectPosition != -1) spinnerSubjectDialog.setSelection(prefilledSubjectPosition)

            // Corrected: Compare String IDs directly
            prefilledBatchPosition = batchListLiveData.indexOfFirst { it.id == examToEdit.batchId }.coerceAtLeast(0)
            if (prefilledBatchPosition != -1) spinnerBatchDialog.setSelection(prefilledBatchPosition)

            // New: Prefill Teacher for Edit Mode
            prefilledTeacherPosition = teacherListLiveData.indexOfFirst { it.id == examToEdit.teacherId }.coerceAtLeast(0)
            spinnerTeacherDialog.setSelection(prefilledTeacherPosition)

        } else if (prefillData != null) {
            val prefillExamName = prefillData.getString(AiAssistantConstants.EXTRA_EXAM_NAME)
            val prefillSubjectName = prefillData.getString(AiAssistantConstants.EXTRA_EXAM_SUBJECT)
            val prefillBatchName = prefillData.getString(AiAssistantConstants.EXTRA_EXAM_BATCH)
            val prefillExamDate = prefillData.getString(AiAssistantConstants.EXTRA_EXAM_DATE)
            val prefillMaxMarks = prefillData.getInt(AiAssistantConstants.EXTRA_EXAM_MAX_MARKS, 0)
            // No prefill for teacher from AI Assistant yet, but could be added later if needed.

            editTextExamName.setText(prefillExamName)
            editTextExamDate.setText(prefillExamDate)
            editTextMaxMarks.setText(prefillMaxMarks.toString())

            prefilledSubjectPosition = subjectListLiveData.indexOfFirst { it.subjectName.equals(prefillSubjectName, ignoreCase = true) }
            if (prefilledSubjectPosition != -1) spinnerSubjectDialog.setSelection(prefilledSubjectPosition)

            if (!prefillBatchName.isNullOrBlank()) {
                prefilledBatchPosition = batchListLiveData.indexOfFirst { it.batchName.equals(prefillBatchName, ignoreCase = true) }
                if (prefilledBatchPosition != -1) spinnerBatchDialog.setSelection(prefilledBatchPosition)
            }
            // If prefill is from AI, default to no teacher selected or first teacher if applicable.
            // For now, let it default to 0 (first item in spinner).

            prefilledAllRequired = !prefillExamName.isNullOrBlank() && prefilledSubjectPosition != -1 &&
                    !prefillExamDate.isNullOrBlank() && prefillMaxMarks > 0
        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        if (shouldAutoSaveOnPrefill && examToEdit == null && prefilledAllRequired) {
            val selectedSubjectItem = if (prefilledSubjectPosition != -1) subjectListLiveData[prefilledSubjectPosition] else null
            val selectedBatchItem = if (prefilledBatchPosition != -1 && !prefillData?.getString(AiAssistantConstants.EXTRA_EXAM_BATCH).isNullOrBlank()) batchListLiveData[prefilledBatchPosition] else null
            // No auto-prefill for teacher from AI Assistant yet.
            // For now, if auto-saving, ensure a teacher is implicitly selected (e.g., the first one).
            val selectedTeacherItemForAutoSave = teacherListLiveData.firstOrNull() // Take the first teacher for auto-save if available

            Handler(Looper.getMainLooper()).postDelayed({
                // Ensure the dialog is still showing and content is valid before clicking
                // Now also check if a teacher is available for auto-save
                if (alertDialog.isShowing && selectedSubjectItem != null &&
                    (prefillData?.getString(AiAssistantConstants.EXTRA_EXAM_BATCH).isNullOrBlank() || selectedBatchItem != null) &&
                    selectedTeacherItemForAutoSave != null) { // Ensure a teacher is selected for auto-save
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    shouldAutoSaveOnPrefill = false
                } else {
                    Toast.makeText(this, "AI: Could not auto-save. Missing or unrecognized data (including teacher). Please confirm manually.", Toast.LENGTH_LONG).show()
                    shouldAutoSaveOnPrefill = false
                }
            }, 300)
        }


        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val examName = editTextExamName.text.toString().trim()
            val maxMarksStr = editTextMaxMarks.text.toString().trim()
            val examDate = editTextExamDate.text.toString().trim()
            val description = editTextDescription.text.toString().trim()

            // New: Get selected teacher
            val selectedTeacher = spinnerTeacherDialog.selectedItem as? User
            val assignedTeacherId = selectedTeacher?.id // Get the ID from the selected User

            if (examName.isEmpty() || maxMarksStr.isEmpty() || examDate.isEmpty() || spinnerSubjectDialog.selectedItem == null || assignedTeacherId.isNullOrBlank()) {
                Toast.makeText(this, "Please fill all required (*) fields, and select a teacher.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedSubject = spinnerSubjectDialog.selectedItem as Subject
            val selectedBatch = spinnerBatchDialog.selectedItem as? Batch
            val maxMarks = maxMarksStr.toInt()

            val exam = Exam(
                id = examToEdit?.id ?: "",
                examName = examName,
                subjectId = selectedSubject.id,
                batchId = selectedBatch?.id,
                examDate = examDate,
                maxMarks = maxMarks,
                description = if (description.isEmpty()) null else description,
                teacherId = assignedTeacherId // Now correctly uses the selected teacher's ID
            )

            if (examToEdit == null) {
                manageExamsViewModel.addExam(exam, assignedTeacherId) // Pass assignedTeacherId
            } else {
                manageExamsViewModel.updateExam(exam, assignedTeacherId) // Pass assignedTeacherId
            }
            alertDialog.dismiss()
        }
    }

    override fun onEditExam(exam: Exam, position: Int) {
        shouldAutoSaveOnPrefill = false // Disable auto-save when manually editing
        showAddEditExamDialog(exam)
    }

    override fun onDeleteExam(exam: Exam, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Exam")
            .setMessage("Are you sure you want to delete '${exam.examName}'? This will also delete all associated results.")
            .setPositiveButton("Delete") { _, _ ->
                manageExamsViewModel.deleteExam(exam.id) // Corrected: exam.id is String
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