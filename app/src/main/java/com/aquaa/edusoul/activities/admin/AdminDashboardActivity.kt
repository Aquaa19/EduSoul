// File: src/main/java/com/aquaa/edusoul/activities/admin/AdminDashboardActivity.kt
package com.aquaa.edusoul.activities.admin

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.activities.EditProfileActivity
import com.aquaa.edusoul.activities.GlobalSearchActivity
import com.aquaa.edusoul.activities.LoginActivity
import com.aquaa.edusoul.activities.messages.ConversationListActivity
import com.aquaa.edusoul.activities.settings.ThemeSelectionActivity
import com.aquaa.edusoul.activities.teacher.ResourceManagementActivity
import com.aquaa.edusoul.adapters.AdminDashboardCardsAdapter
import com.aquaa.edusoul.adapters.DashboardCardItem
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.dialogs.AiAssistantDialogFragment
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.* // Import all repositories
import com.aquaa.edusoul.utils.AiAssistantManager
import com.aquaa.edusoul.utils.ThemeManager
import com.aquaa.edusoul.viewmodels.AdminDashboardViewModel
import com.aquaa.edusoul.viewmodels.AdminDashboardViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull // Changed to firstOrNull for safer single-value retrieval from Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AdminDashboardActivity : BaseActivity(), AdminDashboardCardsAdapter.OnItemClickListener, AiAssistantDialogFragment.AiAssistantListener {

    private val TAG = "AdminDashboardActivity"
    private lateinit var authManager: AuthManager

    private lateinit var recyclerViewAdminDashboard: RecyclerView
    private lateinit var adminDashboardAdapter: AdminDashboardCardsAdapter
    private val dashboardItems = mutableListOf<DashboardCardItem>()
    private lateinit var progressBarAdmin: ProgressBar

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var customizeDashboardLauncher: ActivityResultLauncher<Intent>
    private lateinit var itemTouchHelper: ItemTouchHelper

    private lateinit var textViewTotalStudents: TextView
    private lateinit var textViewTeachersOnline: TextView
    private lateinit var textViewUpcomingClasses: TextView
    private lateinit var textViewPendingFees: TextView

    // Repositories injected via ViewModel, but also directly used for AI Assistant callbacks
    private lateinit var studentRepository: StudentRepository
    private lateinit var userRepository: UserRepository
    private lateinit var classSessionRepository: ClassSessionRepository
    private lateinit var feePaymentRepository: FeePaymentRepository
    private lateinit var subjectRepository: SubjectRepository // Added for AI Assistant
    private lateinit var batchRepository: BatchRepository // Added for AI Assistant


    private lateinit var viewModel: AdminDashboardViewModel


    private lateinit var fabAiAssistant: FloatingActionButton
    private lateinit var aiAssistantManager: AiAssistantManager
    private var aiAssistantDialog: AiAssistantDialogFragment? = null

    private var dX = 0f
    private var dY = 0f
    private var lastAction: Int = 0

    companion object {
        const val PREFS_NAME = "AdminDashboardPrefs"
        const val KEY_CARD_ORDER = "card_order"
        const val KEY_CARD_VISIBILITY_PREFIX = "card_visibility_"

        val DEFAULT_DASHBOARD_CARDS = listOf(
            DashboardCardItem(R.id.cardManageUsers, "Manage Users", R.drawable.ic_manage_users, ManageUsersActivity::class.java),
            DashboardCardItem(R.id.cardManageStudents, "Manage Students", R.drawable.ic_student, ManageStudentsActivity::class.java),
            DashboardCardItem(R.id.cardManageSubjects, "Manage Subjects", R.drawable.ic_subjects, ManageSubjectsActivity::class.java),
            DashboardCardItem(R.id.cardManageBatches, "Manage Batches", R.drawable.ic_batches, ManageBatchesActivity::class.java),
            DashboardCardItem(R.id.cardManageSchedule, "Daily Schedule", R.drawable.ic_schedule, ManageClassScheduleActivity::class.java),
            DashboardCardItem(R.id.cardSetWeeklyTimetable, "Weekly Timetable", R.drawable.ic_add_event, SetWeeklyTimetableActivity::class.java),
            DashboardCardItem(R.id.cardManageExams, "Exam Management", R.drawable.ic_exam_management, ManageExamsActivity::class.java),
            DashboardCardItem(R.id.cardManageFeeStructures, "Manage Fee Structures", R.drawable.ic_fee_item, ManageFeeStructuresActivity::class.java),
            DashboardCardItem(R.id.cardManageAnnouncements, "Broadcast", R.drawable.ic_announcements, ManageAnnouncementsActivity::class.java),
            DashboardCardItem(R.id.cardStatusReports, "Status Reports", R.drawable.ic_analytics, StatusReportsDashboardActivity::class.java),
            // NEW CARD ADDED HERE
            DashboardCardItem(R.id.cardLearningResources, "Learning Resources", R.drawable.ic_resource, ResourceManagementActivity::class.java),
            // Removed the "Backup & Restore" card as requested.
            // DashboardCardItem(R.id.cardSystemConfig, "Backup & Restore", R.drawable.ic_menu_save, SystemConfigActivity::class.java),
            DashboardCardItem(R.id.cardAdminMessages, "Messages", R.drawable.ic_message, ConversationListActivity::class.java)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply the theme before calling super.onCreate to ensure it's set early
        ThemeManager.applyTheme(this, ThemeManager.loadTheme(this, ThemeManager.KEY_ADMIN_THEME))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAdminDashboard)
        setSupportActionBar(toolbar)

        progressBarAdmin = findViewById(R.id.progressBarAdmin)
        progressBarAdmin.visibility = View.VISIBLE

        authManager = AuthManager(this)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        recyclerViewAdminDashboard = findViewById(R.id.recyclerViewAdminDashboard)
        recyclerViewAdminDashboard.layoutManager = GridLayoutManager(this, 2)

        textViewTotalStudents = findViewById(R.id.textViewTotalStudents)
        textViewTeachersOnline = findViewById(R.id.textViewTeachersOnline)
        textViewUpcomingClasses = findViewById(R.id.textViewUpcomingClasses)
        textViewPendingFees = findViewById(R.id.textViewPendingFees)

        // Initialize all repositories directly needed for AI Assistant callbacks
        studentRepository = StudentRepository()
        userRepository = UserRepository()
        classSessionRepository = ClassSessionRepository()
        feePaymentRepository = FeePaymentRepository()
        subjectRepository = SubjectRepository() // Initialize
        batchRepository = BatchRepository() // Initialize


        val factory = AdminDashboardViewModelFactory(studentRepository, userRepository, classSessionRepository, feePaymentRepository)
        viewModel = ViewModelProvider(this, factory).get(AdminDashboardViewModel::class.java)


        fabAiAssistant = findViewById(R.id.fabAiAssistant)
        aiAssistantManager = AiAssistantManager()

        adminDashboardAdapter = AdminDashboardCardsAdapter(this, dashboardItems, this)
        recyclerViewAdminDashboard.adapter = adminDashboardAdapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(dashboardItems, fromPosition, toPosition)
                adminDashboardAdapter.notifyItemMoved(fromPosition, toPosition)
                saveCardOrder()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = true
            override fun isItemViewSwipeEnabled(): Boolean = false

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
            }
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerViewAdminDashboard)


        customizeDashboardLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                loadAndApplyCardPreferences()
            }
        }

        fabAiAssistant.setOnTouchListener { view, event ->
            val viewWidth = view.width
            val viewHeight = view.height
            val parent = view.parent as View

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    lastAction = MotionEvent.ACTION_DOWN
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY

                    val clampedX = newX.coerceIn(0f, (parent.width - viewWidth).toFloat())
                    val clampedY = newY.coerceIn(0f, (parent.height - viewHeight).toFloat())

                    view.x = clampedX
                    view.y = clampedY
                    lastAction = MotionEvent.ACTION_MOVE
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        toggleAiAssistantDialog()
                    }
                    view.performClick()
                    true
                }
                else -> false
            }
        }


        lifecycleScope.launch(Dispatchers.IO) {
            val currentUser: User? = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                val textViewWelcomeAdmin = findViewById<TextView>(R.id.textViewWelcomeAdmin)
                if (currentUser != null) {
                    textViewWelcomeAdmin.text = if (currentUser.fullName != null) "Welcome, ${currentUser.fullName}!" else "Welcome, ${currentUser.username}!"
                    // No recreate() here. Theme is already applied above super.onCreate()
                    loadAndApplyCardPreferences()
                    observeQuickStats()
                } else {
                    Log.e(TAG, "No user logged in to AdminDashboardActivity.")
                    val intent = Intent(this@AdminDashboardActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    Toast.makeText(this@AdminDashboardActivity, "Please log in to continue.", Toast.LENGTH_LONG).show()
                }
                progressBarAdmin.visibility = View.GONE
            }
        }
    }

    private fun loadAndApplyCardPreferences() {
        val savedOrderJson = sharedPreferences.getString(KEY_CARD_ORDER, null)
        val savedOrderIds: List<Int>? = if (savedOrderJson != null) {
            Gson().fromJson(savedOrderJson, object : TypeToken<List<Int>>() {}.type)
        } else {
            null
        }

        val orderedCards = mutableListOf<DashboardCardItem>()
        val defaultCardsMap = DEFAULT_DASHBOARD_CARDS.associateBy { it.id }

        if (savedOrderIds != null) {
            for (id in savedOrderIds) {
                defaultCardsMap[id]?.let {
                    val isVisible = sharedPreferences.getBoolean(KEY_CARD_VISIBILITY_PREFIX + id, true)
                    orderedCards.add(it.copy(isVisible = isVisible))
                }
            }
            val existingIds = orderedCards.map { it.id }.toSet()
            DEFAULT_DASHBOARD_CARDS.forEach {
                if (it.id !in existingIds) {
                    val isVisible = sharedPreferences.getBoolean(KEY_CARD_VISIBILITY_PREFIX + it.id, true)
                    orderedCards.add(it.copy(isVisible = isVisible))
                }
            }
        } else {
            DEFAULT_DASHBOARD_CARDS.forEach {
                val isVisible = sharedPreferences.getBoolean(KEY_CARD_VISIBILITY_PREFIX + it.id, true)
                orderedCards.add(it.copy(isVisible = isVisible))
            }
        }
        dashboardItems.clear()
        dashboardItems.addAll(orderedCards)
        adminDashboardAdapter.updateList(orderedCards)
    }

    private fun saveCardOrder() {
        val currentOrderIds = adminDashboardAdapter.getCurrentOrder().map { it.id }
        val json = Gson().toJson(currentOrderIds)
        sharedPreferences.edit().putString(KEY_CARD_ORDER, json).apply()
        Log.d(TAG, "Saved card order: $currentOrderIds")
    }

    override fun onCardClick(cardItem: DashboardCardItem) {
        if(cardItem.targetActivity != null) {
            val intent = Intent(this, cardItem.targetActivity)
            // Pass a flag to indicate admin access if necessary for ResourceManagementActivity
            if (cardItem.id == R.id.cardLearningResources) {
                intent.putExtra("IS_ADMIN_ACCESS", true)
            }
            startActivity(intent)
        }
    }

    private fun toggleAiAssistantDialog() {
        if (aiAssistantDialog == null || aiAssistantDialog?.isAdded == false) {
            aiAssistantDialog = AiAssistantDialogFragment()
            aiAssistantDialog?.setAiAssistantListener(this)
            aiAssistantDialog?.show(supportFragmentManager, AiAssistantDialogFragment.TAG)
        } else {
            aiAssistantDialog?.dismiss()
        }
    }

    override fun onCommandSent(command: String) {
        aiAssistantManager.processCommand(this, command)
    }

    override fun onDialogDismissed() {
        aiAssistantDialog = null
    }

    fun onAiResponseRequested(
        command: String,
        currentConversationState: AiAssistantDialogFragment.ConversationState,
        currentConversationData: Map<String, String>
    ) {
        Log.d(TAG, "AI Response requested (handled by dialog): Command='$command', State='$currentConversationState', Data='$currentConversationData'")
    }

    // Corrected: Use SubjectRepository
    override suspend fun getSubjectByName(name: String): Subject? = withContext(Dispatchers.IO) {
        subjectRepository.getAllSubjects().firstOrNull()?.find {
            it.subjectName.equals(name, ignoreCase = true)
        }
    }

    // Corrected: Use BatchRepository
    override suspend fun getBatchByName(name: String): Batch? = withContext(Dispatchers.IO) {
        batchRepository.getAllBatches().firstOrNull()?.find {
            it.batchName.equals(name, ignoreCase = true)
        }
    }

    // Corrected: Use UserRepository
    override suspend fun getTeacherByName(name: String): User? = withContext(Dispatchers.IO) {
        userRepository.getAllTeachers().firstOrNull()?.find {
            it.fullName.equals(name, ignoreCase = true) || it.username.equals(name, ignoreCase = true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_dashboard_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, GlobalSearchActivity::class.java))
                true
            }
            R.id.action_customize_dashboard -> {
                customizeDashboardLauncher.launch(Intent(this, CustomizeDashboardActivity::class.java))
                true
            }
            R.id.action_open_theme_picker -> {
                // Pass the theme key for Admin theme
                val intent = Intent(this, ThemeSelectionActivity::class.java)
                intent.putExtra("THEME_SAVE_KEY", ThemeManager.KEY_ADMIN_THEME)
                startActivity(intent)
                true
            }
            R.id.action_edit_profile_admin -> {
                startActivity(Intent(this, EditProfileActivity::class.java))
                true
            }
            R.id.action_logout_admin -> {
                logout()
                true
            }
            R.id.cardAdminMessages -> {
                startActivity(Intent(this, ConversationListActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        Log.i(TAG, "Admin logging out.")
        authManager.logoutUser()
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun observeQuickStats() {
        viewModel.totalStudents.observe(this) { count ->
            textViewTotalStudents.text = count.toString()
        }
        viewModel.totalTeachers.observe(this) { count ->
            textViewTeachersOnline.text = count.toString()
        }
        viewModel.upcomingClassesToday.observe(this) { count ->
            textViewUpcomingClasses.text = count.toString()
        }
        viewModel.pendingFees.observe(this) { count ->
            textViewPendingFees.text = count.toString()
        }
    }
}