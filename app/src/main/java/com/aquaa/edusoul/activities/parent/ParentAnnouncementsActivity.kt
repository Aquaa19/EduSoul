// File: main/java/com/aquaa/edusoul/activities/parent/ParentAnnouncementsActivity.kt
package com.aquaa.edusoul.activities.parent

import android.os.Bundle
import android.util.Log
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
import com.aquaa.edusoul.adapters.AnnouncementAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Announcement
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.AnnouncementRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ParentAnnouncementsViewModel
import com.aquaa.edusoul.viewmodels.ParentAnnouncementsViewModelFactory
import java.util.ArrayList
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParentAnnouncementsActivity : AppCompatActivity() {

    private val TAG = "ParentAnnouncementsAct"

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewAnnouncements: RecyclerView
    private lateinit var textViewNoAnnouncements: TextView
    private lateinit var authManager: AuthManager
    private var currentParent: User? = null
    private var currentParentUserId: String = ""
    private lateinit var announcementAdapter: AnnouncementAdapter
    private lateinit var parentAnnouncementsViewModel: ParentAnnouncementsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_announcements)

        toolbar = findViewById(R.id.toolbarParentAnnouncements)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Announcements"

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            currentParent = authManager.getLoggedInUser()
            currentParentUserId = currentParent?.id ?: ""

            if (currentParent == null || currentParentUserId.isBlank() || currentParent!!.role != User.ROLE_PARENT) {
                Toast.makeText(this@ParentAnnouncementsActivity, "Access Denied: Only parents can view announcements.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            initializeViews()
            setupViewModel()
            setupRecyclerView()
        }
    }

    private fun initializeViews() {
        recyclerViewAnnouncements = findViewById(R.id.recyclerViewParentAnnouncements)
        // Fix: Corrected ID from textViewParentNoAnnouncements to textViewNoAnnouncements
        textViewNoAnnouncements = findViewById(R.id.textViewNoAnnouncements)
    }

    private fun setupViewModel() {
        val announcementRepository = AnnouncementRepository()
        val userRepository = UserRepository()
        val studentRepository = StudentRepository()
        val batchRepository = BatchRepository()

        val factory = ParentAnnouncementsViewModelFactory(
            announcementRepository,
            userRepository,
            studentRepository,
            batchRepository
        )
        parentAnnouncementsViewModel = ViewModelProvider(this, factory)[ParentAnnouncementsViewModel::class.java]

        parentAnnouncementsViewModel.loadAnnouncementsForParent(currentParentUserId)

        parentAnnouncementsViewModel.announcements.observe(this) { announcements ->
            announcementAdapter.setAnnouncements(announcements)
            textViewNoAnnouncements.visibility = if (announcements.isEmpty()) View.VISIBLE else View.GONE
        }

        parentAnnouncementsViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                parentAnnouncementsViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupRecyclerView() {
        announcementAdapter = AnnouncementAdapter(this, mutableListOf(), null)
        recyclerViewAnnouncements.layoutManager = LinearLayoutManager(this)
        recyclerViewAnnouncements.adapter = announcementAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}