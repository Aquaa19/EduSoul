// main/java/com/aquaa/edusoul/activities/teacher/AssignHomeworkActivity.kt
package com.aquaa.edusoul.activities.teacher

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.BatchSelectionAdapter
import com.aquaa.edusoul.models.Homework
import com.aquaa.edusoul.repositories.*
import com.aquaa.edusoul.viewmodels.AssignHomeworkViewModel

class AssignHomeworkActivity : BaseActivity(), BatchSelectionAdapter.OnBatchAssignmentActionListener { // Updated listener interface

    private lateinit var viewModel: AssignHomeworkViewModel
    private lateinit var batchAdapter: BatchSelectionAdapter
    private var homeworkId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_homework)

        val toolbar: Toolbar = findViewById(R.id.toolbarAssignHomework)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        homeworkId = intent.getStringExtra("HOMEWORK_ID") ?: ""
        if (homeworkId.isBlank()) {
            Toast.makeText(this, "Homework not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val homeworkRepository = HomeworkRepository()
        val batchRepository = BatchRepository()
        val batchAssignmentRepository = BatchAssignmentRepository()
        val studentRepository = StudentRepository()
        val studentAssignmentRepository = StudentAssignmentRepository()

        val factory = com.aquaa.edusoul.viewmodels.AssignHomeworkViewModelFactory( // Corrected package
            homeworkRepository,
            batchRepository,
            batchAssignmentRepository,
            studentRepository,
            studentAssignmentRepository
        )
        viewModel = ViewModelProvider(this, factory).get(AssignHomeworkViewModel::class.java)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewBatches)
        // Pass 'this' as listener and homeworkId to the adapter
        batchAdapter = BatchSelectionAdapter(mutableListOf(), mutableListOf(), this, homeworkId)
        recyclerView.adapter = batchAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.loadHomework(homeworkId)
        observeViewModel()

        findViewById<Button>(R.id.buttonConfirmAssignment).setOnClickListener {
            val selectedBatchIdsForAssignment = batchAdapter.getSelectedBatchIds()
            if (selectedBatchIdsForAssignment.isEmpty()) {
                Toast.makeText(this, "No new batches selected for assignment.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            selectedBatchIdsForAssignment.forEach { batchId ->
                viewModel.assignToBatch(homeworkId, batchId)
            }
            // Toast will be shown by ViewModel's toastMessage LiveData
        }
    }

    private fun observeViewModel() {
        viewModel.homework.observe(this) { hw ->
            findViewById<TextView>(R.id.textViewHomeworkTitle).text = "Assigning: ${hw?.title ?: "N/A"}"
        }

        // Combine allBatches and assignedBatches to update the adapter with the full state
        viewModel.allBatches.observe(this) { allBatches ->
            viewModel.assignedBatches.value?.let { assigned ->
                batchAdapter.setData(allBatches, assigned.map { it.batchId })
            } ?: run {
                // If assignedBatches is not yet loaded, just set allBatches
                batchAdapter.setData(allBatches, emptyList())
            }
        }

        viewModel.assignedBatches.observe(this) { assigned ->
            viewModel.allBatches.value?.let { allBatches ->
                batchAdapter.setData(allBatches, assigned.map { it.batchId })
            } ?: run {
                // If allBatches not yet loaded, wait for it
            }
        }

        viewModel.toastMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAssignBatch(homeworkId: String, batchId: String) {
        // This method is not directly used with the new checkbox-based assignment flow.
        // The "Confirm Assignment" button handles new assignments from checked boxes.
    }

    override fun onRemoveBatchAssignment(homeworkId: String, batchId: String) {
        // This is called when the 'X' button next to an assigned batch is clicked
        viewModel.removeBatchAssignment(homeworkId, batchId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}