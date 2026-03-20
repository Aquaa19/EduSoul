package com.aquaa.edusoul.activities.teacher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log // Ensure Log is imported
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
import com.aquaa.edusoul.adapters.AdminDashboardCardsAdapter
import com.aquaa.edusoul.adapters.DashboardCardItem
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.activities.admin.CustomizeDashboardActivity
import com.aquaa.edusoul.dialogs.AiAssistantDialogFragment
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.*
import com.aquaa.edusoul.utils.AiAssistantManager
import com.aquaa.edusoul.utils.ThemeManager
import com.aquaa.edusoul.viewmodels.TeacherDashboardViewModel
import com.aquaa.edusoul.viewmodels.TeacherDashboardViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

class TeacherDashboardActivity : BaseActivity(), AiAssistantDialogFragment.AiAssistantListener, AdminDashboardCardsAdapter.OnItemClickListener {

    private val TAG = "TeacherDashboardActivity"
    private lateinit var authManager: AuthManager

    private lateinit var recyclerViewTeacherDashboard: RecyclerView
    private lateinit var teacherDashboardAdapter: AdminDashboardCardsAdapter
    private val dashboardItems = mutableListOf<DashboardCardItem>()
    private lateinit var progressBarTeacher: ProgressBar

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var customizeDashboardLauncher: ActivityResultLauncher<Intent>
    private lateinit var itemTouchHelper: ItemTouchHelper

    // Quick Stats TextViews
    private lateinit var textViewAssignedStudents: TextView
    private lateinit var textViewAssignedSubjects: TextView
    private lateinit var textViewAssignedBatches: TextView
    private lateinit var textViewUpcomingClasses: TextView

    // "My Day" Overview TextViews and Buttons
    private lateinit var textViewNextClass: TextView
    private lateinit var textViewPendingHomework: TextView // RESTORED: Original TextView
    private lateinit var textViewUpcomingDeadlines: TextView
    private lateinit var buttonMarkAttendance: TextView
    private lateinit var buttonUpdateSyllabus: TextView


    // Repositories used directly for AI Assistant callbacks and ViewModel
    private lateinit var studentRepository: StudentRepository
    private lateinit var userRepository: UserRepository
    private lateinit var varclassSessionRepository: ClassSessionRepository
    private lateinit var subjectRepository: SubjectRepository
    private lateinit var batchRepository: BatchRepository
    private lateinit var teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository
    private lateinit var homeworkRepository: HomeworkRepository
    private lateinit var studentAssignmentRepository: StudentAssignmentRepository // Ensure this is initialized


    private lateinit var viewModel: TeacherDashboardViewModel

    private lateinit var fabAiAssistant: FloatingActionButton
    private lateinit var aiAssistantManager: AiAssistantManager
    private var aiAssistantDialog: AiAssistantDialogFragment? = null

    private var dX = 0f
    private var dY = 0f
    private var lastAction: Int = 0

    companion object {
        const val PREFS_NAME = "TeacherDashboardPrefs"
        const val KEY_CARD_ORDER = "teacher_card_order"
        const val KEY_CARD_VISIBILITY_PREFIX = "teacher_card_visibility_"

        val DEFAULT_TEACHER_DASHBOARD_CARDS = listOf(
            DashboardCardItem(R.id.cardViewSchedule, "View Schedule", R.drawable.ic_schedule, ViewScheduleActivity::class.java),
            DashboardCardItem(R.id.cardMarkAttendance, "Mark Attendance", R.drawable.ic_mark_attendance, MarkAttendanceActivity::class.java),
            DashboardCardItem(R.id.cardUpdateSyllabus, "Update Syllabus", R.drawable.ic_update_syllabus, UpdateSyllabusActivity::class.java),
            DashboardCardItem(R.id.cardManageHomework, "Manage Homework", R.drawable.ic_assignment, ManageHomeworkActivity::class.java),
            DashboardCardItem(R.id.cardEnterScores, "Enter Scores", R.drawable.ic_enter_scores, EnterScoresActivity::class.java),
            DashboardCardItem(R.id.cardResourceManagement, "Resources", R.drawable.ic_resource, ResourceManagementActivity::class.java),
            DashboardCardItem(R.id.cardTeacherAnnouncements, "Announcements", R.drawable.ic_announcements, TeacherAnnouncementsManagementActivity::class.java),
            DashboardCardItem(R.id.cardTeacherMessages, "Messages", R.drawable.ic_message, ConversationListActivity::class.java)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e(TAG, "onCreate: TeacherDashboardActivity started.")
        ThemeManager.applyTheme(this, ThemeManager.loadTheme(this, ThemeManager.KEY_TEACHER_THEME))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbarTeacherDashboard)
        setSupportActionBar(toolbar)

        progressBarTeacher = findViewById(R.id.progressBarTeacher)
        progressBarTeacher.visibility = View.VISIBLE

        authManager = AuthManager(this)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        recyclerViewTeacherDashboard = findViewById(R.id.recyclerViewTeacherDashboard)
        recyclerViewTeacherDashboard.layoutManager = GridLayoutManager(this, 2)

        textViewAssignedStudents = findViewById(R.id.textViewAssignedStudents)
        textViewAssignedSubjects = findViewById(R.id.textViewAssignedSubjects)
        textViewAssignedBatches = findViewById(R.id.textViewAssignedBatches)
        textViewUpcomingClasses = findViewById(R.id.textViewUpcomingClasses)

        // Initialize "My Day" UI elements
        textViewNextClass = findViewById(R.id.textViewNextClass)
        textViewPendingHomework = findViewById(R.id.textViewPendingHomework) // RESTORED: Initialize original TextView
        textViewUpcomingDeadlines = findViewById(R.id.textViewUpcomingDeadlines)
        buttonMarkAttendance = findViewById(R.id.buttonMarkAttendanceDirect)
        buttonUpdateSyllabus = findViewById(R.id.buttonUpdateSyllabusDirect)


        // Initialize all repositories directly needed for AI Assistant callbacks and ViewModel
        studentRepository = StudentRepository()
        userRepository = UserRepository()
        varclassSessionRepository = ClassSessionRepository()
        subjectRepository = SubjectRepository()
        batchRepository = BatchRepository()
        teacherSubjectBatchLinkRepository = TeacherSubjectBatchLinkRepository()
        homeworkRepository = HomeworkRepository()
        studentAssignmentRepository = StudentAssignmentRepository()


        val factory = TeacherDashboardViewModelFactory(
            varclassSessionRepository,
            homeworkRepository,
            studentAssignmentRepository,
            subjectRepository,
            batchRepository,
            userRepository,
            AttendanceRepository(),
            TeacherSubjectBatchLinkRepository(),
            studentRepository
        )
        viewModel = ViewModelProvider(this, factory).get(TeacherDashboardViewModel::class.java)

        fabAiAssistant = findViewById(R.id.fabAiAssistant)
        aiAssistantManager = AiAssistantManager()

        teacherDashboardAdapter = AdminDashboardCardsAdapter(this, dashboardItems, this)
        recyclerViewTeacherDashboard.adapter = teacherDashboardAdapter

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
                teacherDashboardAdapter.notifyItemMoved(fromPosition, toPosition)
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
        itemTouchHelper.attachToRecyclerView(recyclerViewTeacherDashboard)


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
            Log.e(TAG, "lifecycleScope.launch: Attempting to get current user.")
            val currentUser: User? = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                val textViewWelcomeTeacher = findViewById<TextView>(R.id.textViewWelcomeTeacher)
                if (currentUser != null) {
                    textViewWelcomeTeacher.text = if (currentUser.fullName != null) "Welcome, ${currentUser.fullName}!" else "Welcome, ${currentUser.username}!"
                    Log.e(TAG, "CurrentUser found: ID=${currentUser.id}, Role=${currentUser.role}")
                    viewModel.setTeacherId(currentUser.id)
                    loadAndApplyCardPreferences()
                    observeQuickStats()
                    observeMyDayOverview()
                } else {
                    Log.e(TAG, "No user logged in to TeacherDashboardActivity. Redirecting.")
                    val intent = Intent(this@TeacherDashboardActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                    Toast.makeText(this@TeacherDashboardActivity, "Please log in to continue.", Toast.LENGTH_LONG).show()
                }
                progressBarTeacher.visibility = View.GONE
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
        val defaultCardsMap = DEFAULT_TEACHER_DASHBOARD_CARDS.associateBy { it.id }

        if (savedOrderIds != null) {
            for (id in savedOrderIds) {
                defaultCardsMap[id]?.let {
                    val isVisible = sharedPreferences.getBoolean(KEY_CARD_VISIBILITY_PREFIX + id, true)
                    orderedCards.add(it.copy(isVisible = isVisible))
                }
            }
            val existingIds = orderedCards.map { it.id }.toSet()
            DEFAULT_TEACHER_DASHBOARD_CARDS.forEach {
                if (it.id !in existingIds) {
                    val isVisible = sharedPreferences.getBoolean(KEY_CARD_VISIBILITY_PREFIX + it.id, true)
                    orderedCards.add(it.copy(isVisible = isVisible))
                }
            }
        } else {
            DEFAULT_TEACHER_DASHBOARD_CARDS.forEach {
                val isVisible = sharedPreferences.getBoolean(KEY_CARD_VISIBILITY_PREFIX + it.id, true)
                orderedCards.add(it.copy(isVisible = isVisible))
            }
        }
        dashboardItems.clear()
        dashboardItems.addAll(orderedCards)
        teacherDashboardAdapter.updateList(orderedCards)
    }

    private fun saveCardOrder() {
        val currentOrderIds = teacherDashboardAdapter.getCurrentOrder().map { it.id }
        val json = Gson().toJson(currentOrderIds)
        sharedPreferences.edit().putString(KEY_CARD_ORDER, json).apply()
        Log.e(TAG, "Saved card order: $currentOrderIds")
    }

    override fun onCardClick(cardItem: DashboardCardItem) {
        if(cardItem.targetActivity != null) {
            val intent = Intent(this, cardItem.targetActivity)
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

    override suspend fun getSubjectByName(name: String): Subject? = withContext(Dispatchers.IO) {
        subjectRepository.getAllSubjects().firstOrNull()?.find {
            it.subjectName.equals(name, ignoreCase = true)
        }
    }

    override suspend fun getBatchByName(name: String): Batch? = withContext(Dispatchers.IO) {
        batchRepository.getAllBatches().firstOrNull()?.find {
            it.batchName.equals(name, ignoreCase = true)
        }
    }

    override suspend fun getTeacherByName(name: String): User? = withContext(Dispatchers.IO) {
        userRepository.getAllTeachers().firstOrNull()?.find {
            it.fullName.equals(name, ignoreCase = true) || it.username.equals(name, ignoreCase = true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.teacher_dashboard_menu, menu)
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
            R.id.action_choose_theme -> {
                val intent = Intent(this, ThemeSelectionActivity::class.java)
                intent.putExtra("THEME_SAVE_KEY", ThemeManager.KEY_TEACHER_THEME)
                startActivity(intent)
                true
            }
            R.id.action_edit_profile_teacher -> {
                startActivity(Intent(this, EditProfileActivity::class.java))
                true
            }
            R.id.action_logout_teacher -> {
                logout()
                true
            }
            R.id.action_messages_teacher -> {
                startActivity(Intent(this, ConversationListActivity::class.java))
                true
            }
            R.id.action_notifications_teacher -> {
                val intent = Intent(this, TeacherAnnouncementsManagementActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        Log.e(TAG, "Teacher logging out.")
        authManager.logoutUser()
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun observeQuickStats() {
        viewModel.assignedStudentsCount.observe(this) { count ->
            textViewAssignedStudents.text = count.toString()
            Log.e(TAG, "Quick Stats: Assigned Students Count: $count")
        }
        viewModel.assignedSubjectsCount.observe(this) { count ->
            textViewAssignedSubjects.text = count.toString()
            Log.e(TAG, "Quick Stats: Assigned Subjects Count: $count")
        }
        viewModel.assignedBatchesCount.observe(this) { count ->
            textViewAssignedBatches.text = count.toString()
            Log.e(TAG, "Quick Stats: Assigned Batches Count: $count")
        }
        viewModel.upcomingClassesToday.observe(this) { count ->
            textViewUpcomingClasses.text = count.toString()
            Log.e(TAG, "Quick Stats: Upcoming Classes Today: $count")
        }
    }

    private fun observeMyDayOverview() {
        viewModel.nextClassSession.observe(this) { sessionDetails ->
            if (sessionDetails != null) {
                val sessionDate = sessionDetails.classSession.sessionDate
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val datePrefix = if (sessionDate == todayDate) "" else "on $sessionDate "

                textViewNextClass.text = "Next Class: ${datePrefix}${sessionDetails.classSession.startTime} - ${sessionDetails.subjectName} (${sessionDetails.batchName})"
                Log.e(TAG, "My Day: Next Class: ${datePrefix}${sessionDetails.classSession.startTime} - ${sessionDetails.subjectName} (${sessionDetails.batchName})")
                buttonMarkAttendance.visibility = View.VISIBLE
                buttonUpdateSyllabus.visibility = View.VISIBLE

                buttonMarkAttendance.setOnClickListener {
                    val intent = Intent(this, MarkAttendanceActivity::class.java).apply {
                        putExtra("SESSION_ID", sessionDetails.classSession.id)
                        putExtra("SESSION_DATE", sessionDetails.classSession.sessionDate)
                    }
                    startActivity(intent)
                }

                buttonUpdateSyllabus.setOnClickListener {
                    val intent = Intent(this, UpdateSyllabusActivity::class.java).apply {
                        putExtra("SUBJECT_ID", sessionDetails.classSession.subjectId)
                        putExtra("BATCH_ID", sessionDetails.classSession.batchId)
                    }
                    startActivity(intent)
                }
            } else {
                textViewNextClass.text = "Next Class: No classes scheduled soon."
                Log.e(TAG, "My Day: Next Class: No classes scheduled soon.")
                buttonMarkAttendance.visibility = View.GONE
                buttonUpdateSyllabus.visibility = View.GONE
            }
        }

        // RESTORED: Original observation for pending homework count
        viewModel.pendingHomeworkToGradeCount.observe(this) { count ->
            textViewPendingHomework.text = "Pending Homework to Grade: $count"
            Log.e(TAG, "My Day: Pending Homework to Grade: $count")
            // The "Grade Now" link visibility and click logic is removed as part of the revert.
            // It will no longer appear or function.
        }

        viewModel.upcomingHomeworkDeadlinesCount.observe(this) { count ->
            textViewUpcomingDeadlines.text = "Upcoming Homework Deadlines (7 days): $count"
            Log.e(TAG, "My Day: Upcoming Homework Deadlines (7 days): $count")
        }
    }
}