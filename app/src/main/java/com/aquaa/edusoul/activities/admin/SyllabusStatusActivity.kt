// File: main/java/com/aquaa/edusoul/activities/admin/SyllabusStatusActivity.kt
package com.aquaa.edusoul.activities.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.SyllabusStatusAdapter
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.SyllabusTopicStatusAdmin
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.repositories.SyllabusProgressRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.SyllabusStatusViewModel
import com.aquaa.edusoul.viewmodels.SyllabusStatusViewModelFactory

class SyllabusStatusActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var spinnerFilterBatch: Spinner
    private lateinit var spinnerFilterSubject: Spinner
    private lateinit var buttonViewStatus: Button
    private lateinit var recyclerViewStatus: RecyclerView
    private lateinit var textViewNoStatus: TextView

    private lateinit var statusAdapter: SyllabusStatusAdapter
    private lateinit var statusList: MutableList<SyllabusTopicStatusAdmin>
    private lateinit var batchListForSpinner: List<Batch>
    private lateinit var subjectListForSpinner: List<Subject>

    private lateinit var syllabusStatusViewModel: SyllabusStatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_syllabus_status_admin)

        toolbar = findViewById(R.id.toolbarSyllabusStatus)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // REMOVED: val appDatabase = AppDatabase.getDatabase(applicationContext) // This line caused the crash
        val batchRepository = BatchRepository()
        val subjectRepository = SubjectRepository()
        val syllabusTopicRepository = SyllabusTopicRepository()
        val syllabusProgressRepository = SyllabusProgressRepository()
        val userRepository = UserRepository()

        val factory = SyllabusStatusViewModelFactory(
            batchRepository,
            subjectRepository,
            syllabusTopicRepository,
            syllabusProgressRepository,
            userRepository
        )
        syllabusStatusViewModel = ViewModelProvider(this, factory)[SyllabusStatusViewModel::class.java]

        spinnerFilterBatch = findViewById(R.id.spinnerFilterBatchStatus)
        spinnerFilterSubject = findViewById(R.id.spinnerFilterSubjectStatus)
        buttonViewStatus = findViewById(R.id.buttonViewSyllabusStatus)
        recyclerViewStatus = findViewById(R.id.recyclerViewSyllabusStatus)
        textViewNoStatus = findViewById(R.id.textViewNoSyllabusStatus)

        statusList = mutableListOf()
        batchListForSpinner = mutableListOf()
        subjectListForSpinner = mutableListOf()

        statusAdapter = SyllabusStatusAdapter(this, statusList)
        recyclerViewStatus.layoutManager = LinearLayoutManager(this)
        recyclerViewStatus.adapter = statusAdapter

        setupSpinnersAndObservers()

        buttonViewStatus.setOnClickListener { loadStatusReport() }
    }

    private fun setupSpinnersAndObservers() {
        syllabusStatusViewModel.allBatches.observe(this) { batches ->
            batchListForSpinner = batches
            val batchAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, batchListForSpinner)
            batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFilterBatch.adapter = batchAdapter
        }

        syllabusStatusViewModel.allSubjects.observe(this) { subjects ->
            subjectListForSpinner = subjects
            val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjectListForSpinner)
            subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFilterSubject.adapter = subjectAdapter
        }

        syllabusStatusViewModel.syllabusStatusReport.observe(this) { reportData ->
            statusAdapter.setTopics(reportData)
            textViewNoStatus.visibility = if (reportData.isEmpty()) View.VISIBLE else View.GONE
        }

        syllabusStatusViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                syllabusStatusViewModel.clearErrorMessage()
            }
        }
    }

    private fun loadStatusReport() {
        val selectedBatch = spinnerFilterBatch.selectedItem as? Batch
        val selectedSubject = spinnerFilterSubject.selectedItem as? Subject

        if (selectedBatch == null || selectedSubject == null) {
            Toast.makeText(this, "Please select both a batch and a subject.", Toast.LENGTH_SHORT).show()
            return
        }

        syllabusStatusViewModel.selectBatch(selectedBatch.id)
        syllabusStatusViewModel.selectSubject(selectedSubject.id)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
