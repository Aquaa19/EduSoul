package com.aquaa.edusoul.activities.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.AnnouncementAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Announcement
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.AnnouncementRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.TeacherAnnouncementsManagementViewModel
import com.aquaa.edusoul.viewmodels.TeacherAnnouncementsManagementViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.Locale

class TeacherAnnouncementsManagementActivity : BaseActivity(), AnnouncementAdapter.OnAnnouncementActionsListener {

    private val TAG = "TeacherAnnouncementsManagementActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewAnnouncements: RecyclerView
    private lateinit var fabAddAnnouncement: FloatingActionButton
    private lateinit var textViewNoAnnouncements: TextView
    private lateinit var announcementAdapter: AnnouncementAdapter
    private lateinit var announcementList: ArrayList<Announcement>

    private lateinit var authManager: AuthManager
    private var currentTeacher: User? = null

    private lateinit var viewModel: TeacherAnnouncementsManagementViewModel

    // For dialog spinners
    private var batchesForTeacher: List<Batch> = emptyList()
    private var subjectsForTeacher: List<Subject> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_announcements) // Reusing existing layout

        toolbar = findViewById(R.id.toolbarTeacherAnnouncements)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage My Announcements"

        authManager = AuthManager(this)

        // Initialize ViewModel early
        val factory = TeacherAnnouncementsManagementViewModelFactory(
            AnnouncementRepository(),
            BatchRepository(),
            SubjectRepository(),
            UserRepository(),
            TeacherSubjectBatchLinkRepository()
        )
        viewModel = ViewModelProvider(this, factory)[TeacherAnnouncementsManagementViewModel::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            currentTeacher = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                if (currentTeacher == null || currentTeacher?.role != User.ROLE_TEACHER) {
                    Toast.makeText(this@TeacherAnnouncementsManagementActivity, "Access Denied: Only teachers can manage announcements.", Toast.LENGTH_LONG).show()
                    finish()
                    return@withContext
                }
                currentTeacher?.id?.let {
                    // Pass current teacher's ID as String to ViewModel.
                    // The ViewModel's 'announcementsForTeacher' LiveData will now automatically
                    // fetch and combine all relevant announcements for this teacher.
                    viewModel.setCurrentTeacherId(it)
                    // REMOVED: viewModel.setAnnouncementFilter(Announcement.AUDIENCE_ALL, null)
                    // This line is no longer needed as the ViewModel now handles fetching all relevant types.
                }
            }
        }


        recyclerViewAnnouncements = findViewById(R.id.recyclerViewTeacherAnnouncements)
        textViewNoAnnouncements = findViewById(R.id.textViewNoAnnouncements)
        fabAddAnnouncement = findViewById(R.id.fabAddAnnouncement)

        announcementList = ArrayList()
        announcementAdapter = AnnouncementAdapter(this, announcementList, this)

        recyclerViewAnnouncements.layoutManager = LinearLayoutManager(this)
        recyclerViewAnnouncements.adapter = announcementAdapter

        fabAddAnnouncement.setOnClickListener { showAddEditAnnouncementDialog(null) }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.announcementsForTeacher.observe(this) { announcements ->
            // The ViewModel now provides the fully filtered and combined list.
            // No need for client-side filtering here anymore.
            announcementAdapter.setAnnouncements(announcements)
            textViewNoAnnouncements.visibility = if (announcements.isEmpty()) View.VISIBLE else View.GONE
            recyclerViewAnnouncements.visibility = if (announcements.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.batchesForTeacher.observe(this) { batches ->
            batchesForTeacher = batches
        }

        viewModel.subjectsForTeacher.observe(this) { subjects ->
            subjectsForTeacher = subjects
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun showAddEditAnnouncementDialog(announcementToEdit: Announcement?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_announcement, null)
        builder.setView(dialogView)

        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.tilAnnouncementTitleDialog)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextAnnouncementTitleDialog)
        val tilContent = dialogView.findViewById<TextInputLayout>(R.id.tilAnnouncementContentDialog)
        val editTextContent = dialogView.findViewById<EditText>(R.id.editTextAnnouncementContentDialog)
        val spinnerAudience = dialogView.findViewById<Spinner>(R.id.spinnerAudienceDialog)
        val textViewTargetBatchLabel = dialogView.findViewById<TextView>(R.id.textViewTargetBatchLabel)
        val spinnerTargetBatch = dialogView.findViewById<Spinner>(R.id.spinnerTargetBatch)
        val textViewTargetSubjectLabel = dialogView.findViewById<TextView>(R.id.textViewTargetSubjectLabel)
        val spinnerTargetSubject = dialogView.findViewById<Spinner>(R.id.spinnerTargetSubject)


        val audienceOptions = arrayOf(
            Announcement.AUDIENCE_ALL,
            Announcement.AUDIENCE_TEACHERS,
            Announcement.AUDIENCE_BATCH,
            Announcement.AUDIENCE_SUBJECT
        )
        val audienceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ArrayList(Arrays.asList(*audienceOptions)))
        audienceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAudience.adapter = audienceAdapter

        // Setup Batch Spinner
        val batchNames = batchesForTeacher.map { it.batchName }.toMutableList()
        batchNames.add(0, "Select Batch (Optional)")
        val batchAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, batchNames)
        batchAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        spinnerTargetBatch.adapter = batchAdapter

        // Setup Subject Spinner
        val subjectNames = subjectsForTeacher.map { it.subjectName }.toMutableList()
        subjectNames.add(0, "Select Subject (Optional)")
        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjectNames)
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        spinnerTargetSubject.adapter = subjectAdapter


        builder.setTitle(if (announcementToEdit == null) "Add New Announcement" else "Edit Announcement")

        // Pre-fill fields for editing
        var selectedAudiencePosition = 0
        if (announcementToEdit != null) {
            editTextTitle.setText(announcementToEdit.title)
            editTextContent.setText(announcementToEdit.content)

            // Select audience in spinner
            selectedAudiencePosition = audienceOptions.indexOf(announcementToEdit.targetAudience).coerceAtLeast(0)

            // If target audience is BATCH or SUBJECT, pre-select the target ID
            when (announcementToEdit.targetAudience) {
                Announcement.AUDIENCE_BATCH -> {
                    val batchPosition = batchesForTeacher.indexOfFirst { it.id == announcementToEdit.batchId }.coerceAtLeast(0) + 1
                    spinnerTargetBatch.setSelection(batchPosition)
                }
                Announcement.AUDIENCE_SUBJECT -> {
                    val subjectPosition = subjectsForTeacher.indexOfFirst { it.id == announcementToEdit.subjectId }.coerceAtLeast(0) + 1
                    spinnerTargetSubject.setSelection(subjectPosition)
                }
            }
        }
        spinnerAudience.setSelection(selectedAudiencePosition)

        // Listener for Audience Spinner to show/hide Batch/Subject Spinners
        spinnerAudience.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedAudience = parent.getItemAtPosition(position).toString()
                when (selectedAudience) {
                    Announcement.AUDIENCE_BATCH -> {
                        textViewTargetBatchLabel.visibility = View.VISIBLE
                        spinnerTargetBatch.visibility = View.VISIBLE
                        textViewTargetSubjectLabel.visibility = View.GONE
                        spinnerTargetSubject.visibility = View.GONE
                    }
                    Announcement.AUDIENCE_SUBJECT -> {
                        textViewTargetBatchLabel.visibility = View.GONE
                        spinnerTargetBatch.visibility = View.GONE
                        textViewTargetSubjectLabel.visibility = View.VISIBLE
                        spinnerTargetSubject.visibility = View.VISIBLE
                    }
                    else -> {
                        textViewTargetBatchLabel.visibility = View.GONE
                        spinnerTargetBatch.visibility = View.GONE
                        textViewTargetSubjectLabel.visibility = View.GONE
                        spinnerTargetSubject.visibility = View.GONE
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        // Trigger listener initially to set correct visibility based on pre-selected item
        spinnerAudience.setSelection(selectedAudiencePosition, true)


        builder.setPositiveButton("Publish", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val content = editTextContent.text.toString().trim()
            val audience = spinnerAudience.selectedItem.toString()

            var targetBatchId: String? = null
            var targetSubjectId: String? = null

            when (audience) {
                Announcement.AUDIENCE_BATCH -> {
                    val selectedBatchName = spinnerTargetBatch.selectedItem.toString()
                    if (selectedBatchName.contains("Select Batch") || selectedBatchName.isBlank()) {
                        Toast.makeText(this, "Please select a target batch for this announcement.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    targetBatchId = batchesForTeacher.find { it.batchName == selectedBatchName }?.id
                    if (targetBatchId == null) {
                        Toast.makeText(this, "Selected batch not found. Please try again.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                Announcement.AUDIENCE_SUBJECT -> {
                    val selectedSubjectName = spinnerTargetSubject.selectedItem.toString()
                    if (selectedSubjectName.contains("Select Subject") || selectedSubjectName.isBlank()) {
                        Toast.makeText(this, "Please select a target subject for this announcement.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    targetSubjectId = subjectsForTeacher.find { it.subjectName == selectedSubjectName }?.id
                    if (targetSubjectId == null) {
                        Toast.makeText(this, "Selected subject not found. Please try again.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            }


            tilTitle.error = null
            tilContent.error = null

            if (title.isEmpty()) {
                tilTitle.error = "Title cannot be empty"
                return@setOnClickListener
            }
            if (content.isEmpty()) {
                tilContent.error = "Content cannot be empty"
                return@setOnClickListener
            }

            val announcement = Announcement(
                id = announcementToEdit?.id ?: "",
                title = title,
                content = content,
                targetAudience = audience,
                batchId = targetBatchId,
                subjectId = targetSubjectId,
                publishDate = null,
                expiryDate = null,
                authorUserId = currentTeacher?.id
            )

            if (announcementToEdit == null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                announcement.publishDate = sdf.format(Date())
                viewModel.addAnnouncement(announcement)
            } else {
                announcement.publishDate = announcementToEdit.publishDate
                announcement.authorUserId = announcementToEdit.authorUserId
                viewModel.updateAnnouncement(announcement)
            }
            alertDialog.dismiss()
        }
    }

    override fun onEditAnnouncement(announcement: Announcement, position: Int) {
        showAddEditAnnouncementDialog(announcement)
    }

    override fun onDeleteAnnouncement(announcement: Announcement, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Announcement")
            .setMessage("Are you sure you want to delete announcement: '${announcement.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteAnnouncement(announcement.id)
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