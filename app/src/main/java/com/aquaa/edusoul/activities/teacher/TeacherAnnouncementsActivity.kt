package com.aquaa.edusoul.activities.teacher

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class TeacherAnnouncementsActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewAnnouncements: RecyclerView
    private lateinit var textViewNoAnnouncements: TextView
    private lateinit var announcementAdapter: AnnouncementAdapter
    private lateinit var announcementList: ArrayList<Announcement> // Use ArrayList explicitly

    private lateinit var authManager: AuthManager
    private var currentUser: User? = null

    // We'll reuse ManageAnnouncementsViewModel as it can fetch announcements
    private lateinit var announcementsViewModel: ManageAnnouncementsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_announcements)

        toolbar = findViewById(R.id.toolbarTeacherAnnouncements)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        // Get current user to filter announcements if needed (e.g., for specific roles)
        CoroutineScope(Dispatchers.IO).launch {
            currentUser = authManager.getLoggedInUser()
            if (currentUser == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TeacherAnnouncementsActivity, "Error: User not identified. Please log in again.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        // Removed: val appDatabase = AppDatabase.getDatabase(applicationContext)
        val announcementRepository = AnnouncementRepository()
        val factory = ManageAnnouncementsViewModelFactory(announcementRepository)
        announcementsViewModel = ViewModelProvider(this, factory)[ManageAnnouncementsViewModel::class.java]

        recyclerViewAnnouncements = findViewById(R.id.recyclerViewTeacherAnnouncements)
        // FIX: Corrected the ID to match the XML layout
        textViewNoAnnouncements = findViewById(R.id.textViewNoAnnouncements)

        announcementList = ArrayList()
        // Pass null for listener as teachers can only view, not edit/delete from this screen
        announcementAdapter = AnnouncementAdapter(this, announcementList, null)

        recyclerViewAnnouncements.layoutManager = LinearLayoutManager(this)
        recyclerViewAnnouncements.adapter = announcementAdapter

        setupObservers()
    }

    private fun setupObservers() {
        // Observe all announcements and filter for relevant ones for the teacher
        announcementsViewModel.allAnnouncements.observe(this) { allAnnouncements ->
            val filteredAnnouncements = allAnnouncements.filter { announcement ->
                // Filter for announcements targeted to ALL or TEACHERS role
                announcement.targetAudience == Announcement.AUDIENCE_ALL ||
                        announcement.targetAudience == Announcement.AUDIENCE_TEACHERS
            }
            announcementAdapter.setAnnouncements(filteredAnnouncements)

            if (filteredAnnouncements.isEmpty()) {
                textViewNoAnnouncements.visibility = View.VISIBLE
                recyclerViewAnnouncements.visibility = View.GONE
            } else {
                textViewNoAnnouncements.visibility = View.GONE
                recyclerViewAnnouncements.visibility = View.VISIBLE
            }
        }

        announcementsViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                announcementsViewModel.clearErrorMessage()
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