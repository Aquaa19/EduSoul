package com.aquaa.edusoul.activities.parent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.activities.LoginActivity
import com.aquaa.edusoul.models.User
import com.google.android.material.card.MaterialCardView
import com.aquaa.edusoul.activities.EditProfileActivity
import com.aquaa.edusoul.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aquaa.edusoul.activities.messages.ConversationListActivity
import com.aquaa.edusoul.activities.settings.ThemeSelectionActivity

class ParentDashboardActivity : BaseActivity(), View.OnClickListener {

    private val TAG = "ParentDashboardActivity"

    private lateinit var textViewWelcomeParent: TextView
    private lateinit var authManager: AuthManager
    private lateinit var progressBarParent: ProgressBar

    // Parent dashboard cards
    private lateinit var cardParentViewAttendance: MaterialCardView
    private lateinit var cardParentSyllabusProgress: MaterialCardView
    private lateinit var cardParentPerformanceOverview: MaterialCardView
    private lateinit var cardParentViewFeesStatus: MaterialCardView
    private lateinit var cardParentStudentProfile: MaterialCardView
    private lateinit var cardParentHomeworkAssignments: MaterialCardView
    private lateinit var cardParentResourceSharing: MaterialCardView
    private lateinit var cardParentMessages: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the theme before calling super.onCreate to ensure it's set early
        ThemeManager.applyTheme(this, ThemeManager.AppTheme.NEUTRAL) // Parents always use Neutral theme
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbarParentDashboard)
        setSupportActionBar(toolbar)

        progressBarParent = findViewById(R.id.progressBarParent)
        progressBarParent.visibility = View.VISIBLE

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val currentUser: User? = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                val textViewWelcomeParent = findViewById<TextView>(R.id.textViewWelcomeParent)
                if (currentUser != null) {
                    textViewWelcomeParent.text = if (currentUser.fullName != null) "Welcome, ${currentUser.fullName}!" else "Welcome, ${currentUser.username}!"
                    // No recreate() here. Theme is already applied above super.onCreate()
                } else {
                    Log.e(TAG, "No user logged in to ParentDashboardActivity.")
                    Toast.makeText(this@ParentDashboardActivity, "Error: User data not found. Logging out.", Toast.LENGTH_SHORT).show()
                    logout()
                    return@withContext
                }
                progressBarParent.visibility = View.GONE
            }
        }

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        cardParentViewAttendance = findViewById(R.id.cardParentViewAttendance)
        cardParentSyllabusProgress = findViewById(R.id.cardParentSyllabusProgress)
        cardParentPerformanceOverview = findViewById(R.id.cardParentPerformanceOverview)
        cardParentViewFeesStatus = findViewById(R.id.cardParentViewFeesStatus)
        cardParentStudentProfile = findViewById(R.id.cardParentStudentProfile)
        cardParentHomeworkAssignments = findViewById(R.id.cardParentHomeworkAssignments)
        cardParentResourceSharing = findViewById(R.id.cardParentResourceSharing)
        cardParentMessages = findViewById(R.id.cardParentMessages)
    }

    private fun setupListeners() {
        cardParentViewAttendance.setOnClickListener(this)
        cardParentSyllabusProgress.setOnClickListener(this)
        cardParentPerformanceOverview.setOnClickListener(this)
        cardParentViewFeesStatus.setOnClickListener(this)
        cardParentStudentProfile.setOnClickListener(this)
        cardParentHomeworkAssignments.setOnClickListener(this)
        cardParentResourceSharing.setOnClickListener(this)
        cardParentMessages.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val intent: Intent? = when (v.id) {
            R.id.cardParentViewAttendance -> Intent(this, ParentViewAttendanceActivity::class.java)
            R.id.cardParentSyllabusProgress -> Intent(this, ParentSyllabusTrackerActivity::class.java)
            R.id.cardParentPerformanceOverview -> Intent(this, ParentPerformanceOverviewActivity::class.java)
            R.id.cardParentViewFeesStatus -> Intent(this, ParentViewFeeStatusActivity::class.java)
            R.id.cardParentStudentProfile -> Intent(this, ParentStudentProfileActivity::class.java)
            R.id.cardParentHomeworkAssignments -> Intent(this, ParentViewHomeworkActivity::class.java)
            R.id.cardParentResourceSharing -> Intent(this, ParentViewResourcesActivity::class.java)
            R.id.cardParentMessages -> Intent(this, ConversationListActivity::class.java)
            else -> null
        }
        intent?.let { startActivity(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.parent_dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications_parent -> {
                val intent = Intent(this, ParentAnnouncementsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_edit_profile_parent -> {
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout_parent -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        Log.i(TAG, "Parent logging out.")
        authManager.logoutUser()
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}