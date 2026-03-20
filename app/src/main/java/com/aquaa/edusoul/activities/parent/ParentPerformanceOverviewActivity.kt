// File: main/java/com/aquaa/edusoul/activities/parent/ParentPerformanceOverviewActivity.kt
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
import com.aquaa.edusoul.adapters.ParentExamResultAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.ParentExamResultDetails
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.viewmodels.ParentPerformanceOverviewViewModel
import com.aquaa.edusoul.viewmodels.ParentPerformanceOverviewViewModelFactory
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext

class ParentPerformanceOverviewActivity : AppCompatActivity(), ParentExamResultAdapter.OnItemClickListener {

    private val TAG = "ParentPerformanceOverviewActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewExamResults: RecyclerView
    private lateinit var textViewNoResults: TextView
    private lateinit var authManager: AuthManager
    private var currentParent: User? = null
    private lateinit var examResultAdapter: ParentExamResultAdapter
    private lateinit var textViewOverallSummary: TextView
    private lateinit var parentPerformanceOverviewViewModel: ParentPerformanceOverviewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_performance_overview)

        toolbar = findViewById(R.id.toolbarParentPerformanceOverview)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Performance Overview"

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            currentParent = authManager.getLoggedInUser()

            if (currentParent == null || currentParent!!.role != User.ROLE_PARENT) {
                Toast.makeText(this@ParentPerformanceOverviewActivity, "Access Denied: Only parents can view performance overview.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            initializeViews()
            setupViewModel()
            setupRecyclerView()
        }
    }

    private fun initializeViews() {
        recyclerViewExamResults = findViewById(R.id.recyclerViewParentExamResults)
        textViewNoResults = findViewById(R.id.textViewParentNoResults)
        textViewOverallSummary = findViewById(R.id.textViewOverallSummary)
    }

    private fun setupViewModel() {
        // AppDatabase.getDatabase is not used for DAOs directly
        val studentRepository = StudentRepository()
        val resultRepository = ResultRepository()
        val examRepository = ExamRepository()
        val subjectRepository = SubjectRepository()

        val factory = ParentPerformanceOverviewViewModelFactory(
            studentRepository,
            resultRepository,
            examRepository,
            subjectRepository
        )
        parentPerformanceOverviewViewModel = ViewModelProvider(this, factory)[ParentPerformanceOverviewViewModel::class.java]

        currentParent?.id?.let { parentId ->
            parentPerformanceOverviewViewModel.setParentUserId(parentId) // parentId is String
        }

        parentPerformanceOverviewViewModel.parentExamResultDetailsList.observe(this) { results ->
            examResultAdapter.updateData(results)
            textViewNoResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
            recyclerViewExamResults.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
        }

        parentPerformanceOverviewViewModel.overallSummary.observe(this) { summaryText ->
            if (summaryText.contains("No graded exams")) {
                textViewOverallSummary.visibility = View.GONE
            } else {
                textViewOverallSummary.text = summaryText
                textViewOverallSummary.visibility = View.VISIBLE
            }
        }

        parentPerformanceOverviewViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                parentPerformanceOverviewViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupRecyclerView() {
        examResultAdapter = ParentExamResultAdapter(this, mutableListOf(), this)
        recyclerViewExamResults.layoutManager = LinearLayoutManager(this)
        recyclerViewExamResults.adapter = examResultAdapter
    }

    override fun onExamResultCardClick(examResultDetails: ParentExamResultDetails) {
        Toast.makeText(
            this,
            "Clicked on Exam: ${examResultDetails.examName} - Score: ${examResultDetails.marksObtained}/${examResultDetails.examMaxMarks}",
            Toast.LENGTH_SHORT
        ).show()
        Log.d(TAG, "Exam result card clicked: ${examResultDetails.examName}")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}