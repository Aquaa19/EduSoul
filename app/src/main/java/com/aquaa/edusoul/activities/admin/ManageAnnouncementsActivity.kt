// File: main/java/com/aquaa/edusoul/activities/admin/ManageAnnouncementsActivity.kt
package com.aquaa.edusoul.activities.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
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
import com.aquaa.edusoul.adapters.AnnouncementAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Announcement
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.AnnouncementRepository
import com.aquaa.edusoul.viewmodels.ManageAnnouncementsViewModel
import com.aquaa.edusoul.viewmodels.ManageAnnouncementsViewModelFactory
import com.aquaa.edusoul.utils.AiAssistantConstants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import java.util.Arrays
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext

/*
 * ManageAnnouncementsActivity: Allows an admin to manage announcements.
 * Migrated to Kotlin and MVVM.
 */
class ManageAnnouncementsActivity : BaseActivity(), AnnouncementAdapter.OnAnnouncementActionsListener {

    private val TAG = "ManageAnnouncements"

    private lateinit var recyclerViewAnnouncements: RecyclerView
    private lateinit var fabAddAnnouncement: FloatingActionButton
    private lateinit var textViewNoAnnouncements: TextView
    private lateinit var announcementAdapter: AnnouncementAdapter
    private lateinit var announcementList: java.util.ArrayList<Announcement>
    private lateinit var authManager: AuthManager
    private var currentAdmin: User? = null

    private lateinit var manageAnnouncementsViewModel: ManageAnnouncementsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_announcements)

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageAnnouncements)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            currentAdmin = authManager.getLoggedInUser()
        }

        recyclerViewAnnouncements = findViewById(R.id.recyclerViewAnnouncements)
        fabAddAnnouncement = findViewById(R.id.fabAddAnnouncement)
        textViewNoAnnouncements = findViewById(R.id.textViewNoAnnouncements)

        announcementList = java.util.ArrayList<Announcement>()
        announcementAdapter = AnnouncementAdapter(this, announcementList, this)

        recyclerViewAnnouncements.layoutManager = LinearLayoutManager(this)
        recyclerViewAnnouncements.adapter = announcementAdapter

        fabAddAnnouncement.setOnClickListener { showAddEditAnnouncementDialog(null, null) }

        // REMOVED: val appDatabase = AppDatabase.getDatabase(applicationContext) // This line caused the crash
        val announcementRepository = AnnouncementRepository()
        val factory = ManageAnnouncementsViewModelFactory(announcementRepository)
        manageAnnouncementsViewModel = ViewModelProvider(this, factory)[ManageAnnouncementsViewModel::class.java]

        setupObservers()

        if (intent.getBooleanExtra(AiAssistantConstants.EXTRA_PREFILL_DATA, false)) {
            val title = intent.getStringExtra(AiAssistantConstants.EXTRA_ANNOUNCEMENT_TITLE)
            val content = intent.getStringExtra(AiAssistantConstants.EXTRA_ANNOUNCEMENT_CONTENT)
            val audience = intent.getStringExtra(AiAssistantConstants.EXTRA_ANNOUNCEMENT_AUDIENCE)

            val prefillBundle = Bundle().apply {
                putString(AiAssistantConstants.EXTRA_ANNOUNCEMENT_TITLE, title)
                putString(AiAssistantConstants.EXTRA_ANNOUNCEMENT_CONTENT, content)
                putString(AiAssistantConstants.EXTRA_ANNOUNCEMENT_AUDIENCE, audience)
            }
            showAddEditAnnouncementDialog(null, prefillBundle)
        }
    }

    private fun setupObservers() {
        manageAnnouncementsViewModel.allAnnouncements.observe(this) { announcementsFromDb ->
            announcementAdapter.setAnnouncements(announcementsFromDb)

            if (announcementAdapter.itemCount == 0) {
                textViewNoAnnouncements.visibility = View.VISIBLE
                recyclerViewAnnouncements.visibility = View.GONE
            } else {
                textViewNoAnnouncements.visibility = View.GONE
                recyclerViewAnnouncements.visibility = View.VISIBLE
            }
        }

        manageAnnouncementsViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                manageAnnouncementsViewModel.clearErrorMessage()
            }
        }
    }

    private fun showAddEditAnnouncementDialog(announcementToEdit: Announcement?, prefillData: Bundle? = null) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_announcement, null)
        builder.setView(dialogView)

        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.tilAnnouncementTitleDialog)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextAnnouncementTitleDialog)
        val tilContent = dialogView.findViewById<TextInputLayout>(R.id.tilAnnouncementContentDialog)
        val editTextContent = dialogView.findViewById<EditText>(R.id.editTextAnnouncementContentDialog)
        val spinnerAudience = dialogView.findViewById<Spinner>(R.id.spinnerAudienceDialog)

        val audienceOptions = arrayOf(
            Announcement.AUDIENCE_ALL,
            Announcement.AUDIENCE_PARENTS, // Corrected constant
            Announcement.AUDIENCE_TEACHERS // Corrected constant
        )
        val audienceList = java.util.ArrayList(Arrays.asList(*audienceOptions))
        val audienceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            audienceList
        )
        audienceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAudience.adapter = audienceAdapter

        // Hide batch and subject selectors for admin announcements
        dialogView.findViewById<TextView>(R.id.textViewTargetBatchLabel).visibility = View.GONE
        dialogView.findViewById<Spinner>(R.id.spinnerTargetBatch).visibility = View.GONE
        dialogView.findViewById<TextView>(R.id.textViewTargetSubjectLabel).visibility = View.GONE
        dialogView.findViewById<Spinner>(R.id.spinnerTargetSubject).visibility = View.GONE


        builder.setTitle(if (announcementToEdit == null) "Add New Announcement" else "Edit Announcement")

        if (announcementToEdit != null) {
            editTextTitle.setText(announcementToEdit.title)
            editTextContent.setText(announcementToEdit.content)
            val audience = announcementToEdit.targetAudience
            when (audience) {
                Announcement.AUDIENCE_PARENTS -> spinnerAudience.setSelection(audienceList.indexOf(Announcement.AUDIENCE_PARENTS))
                Announcement.AUDIENCE_TEACHERS -> spinnerAudience.setSelection(audienceList.indexOf(Announcement.AUDIENCE_TEACHERS))
                else -> spinnerAudience.setSelection(audienceList.indexOf(Announcement.AUDIENCE_ALL))
            }
        } else if (prefillData != null) {
            val prefillTitle = prefillData.getString(AiAssistantConstants.EXTRA_ANNOUNCEMENT_TITLE)
            val prefillContent = prefillData.getString(AiAssistantConstants.EXTRA_ANNOUNCEMENT_CONTENT)
            val prefillAudience = prefillData.getString(AiAssistantConstants.EXTRA_ANNOUNCEMENT_AUDIENCE)

            editTextTitle.setText(prefillTitle ?: "")
            editTextContent.setText(prefillContent ?: "")

            val audiencePosition = audienceList.indexOfFirst { it.equals(prefillAudience, ignoreCase = true) }
            if (audiencePosition != -1) spinnerAudience.setSelection(audiencePosition)
        }

        builder.setPositiveButton("Publish", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val content = editTextContent.text.toString().trim()
            val audience = spinnerAudience.selectedItem.toString()

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

            // If Announcement is a data class, use copy()
            // Otherwise, ensure publishDate and authorUserId are 'var' in Announcement class
            // or create a new Announcement object and assign values.

            if (announcementToEdit == null) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val newAnnouncement = Announcement(
                    id = "", // Generate a new ID or leave it to your backend/database
                    title = title,
                    content = content,
                    targetAudience = audience,
                    batchId = null,
                    subjectId = null,
                    publishDate = sdf.format(Date()),
                    expiryDate = null,
                    authorUserId = currentAdmin?.id
                )
                manageAnnouncementsViewModel.addAnnouncement(newAnnouncement)
            } else {
                // Option 1: If Announcement is a data class
                // val updatedAnnouncement = announcementToEdit.copy(
                // title = title,
                // content = content,
                // targetAudience = audience
                // // authorUserId and publishDate are typically not changed on edit,
                // // but if they are, include them in the copy()
                // )

                // Option 2: If Announcement properties are 'var'
                // announcementToEdit.title = title
                // announcementToEdit.content = content
                // announcementToEdit.targetAudience = audience
                // val updatedAnnouncement = announcementToEdit

                // Option 3: Create a new object with existing and new values (less ideal for updates)
                val updatedAnnouncement = Announcement(
                    id = announcementToEdit.id, // Keep existing ID
                    title = title,
                    content = content,
                    targetAudience = audience,
                    batchId = announcementToEdit.batchId,
                    subjectId = announcementToEdit.subjectId,
                    publishDate = announcementToEdit.publishDate, // Keep original publish date
                    expiryDate = announcementToEdit.expiryDate,
                    authorUserId = announcementToEdit.authorUserId // Keep original author
                )
                manageAnnouncementsViewModel.updateAnnouncement(updatedAnnouncement)
            }
            alertDialog.dismiss()
        }
    }

    override fun onEditAnnouncement(announcement: Announcement, position: Int) {
        showAddEditAnnouncementDialog(announcement, null)
    }

    override fun onDeleteAnnouncement(announcement: Announcement, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Announcement")
            .setMessage("Are you sure you want to delete announcement: '${announcement.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                manageAnnouncementsViewModel.deleteAnnouncement(announcement.id)
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
