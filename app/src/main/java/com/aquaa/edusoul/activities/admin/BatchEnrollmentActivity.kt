package com.aquaa.edusoul.activities.admin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
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
import com.aquaa.edusoul.adapters.StudentEnrollmentAdapter
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.repositories.StudentBatchLinkRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.viewmodels.BatchEnrollmentViewModel
import com.aquaa.edusoul.viewmodels.BatchEnrollmentViewModelFactory
import java.util.ArrayList

class BatchEnrollmentActivity : BaseActivity(), StudentEnrollmentAdapter.OnEnrollmentActionListener {

    private val TAG = "BatchEnrollmentActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var textViewBatchEnrollmentInfo: TextView
    private lateinit var recyclerViewEnrolledStudents: RecyclerView
    private lateinit var recyclerViewAvailableStudents: RecyclerView
    private lateinit var textViewNoEnrolledStudents: TextView
    private lateinit var textViewNoAvailableStudents: TextView

    private lateinit var enrolledStudentsAdapter: StudentEnrollmentAdapter
    private lateinit var availableStudentsAdapter: StudentEnrollmentAdapter
    private lateinit var enrolledStudentList: ArrayList<Student>
    private lateinit var availableStudentList: ArrayList<Student>

    // Changed type from Long to String for Batch ID
    private var currentBatchId: String = ""
    private var currentBatchName: String = ""

    private lateinit var batchEnrollmentViewModel: BatchEnrollmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_enrollment)

        toolbar = findViewById(R.id.toolbarBatchEnrollment)
        setSupportActionBar(toolbar)

        if (intent.extras != null) {
            // Changed from getLongExtra to getStringExtra as Batch IDs are now String
            currentBatchId = intent.getStringExtra("BATCH_ID") ?: ""
            currentBatchName = intent.getStringExtra("BATCH_NAME") ?: ""
        }

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Enrollments: " + (if (currentBatchName.isNotBlank()) currentBatchName else "Batch")
        }

        textViewBatchEnrollmentInfo = findViewById(R.id.textViewBatchEnrollmentInfo)
        recyclerViewEnrolledStudents = findViewById(R.id.recyclerViewEnrolledStudents)
        recyclerViewAvailableStudents = findViewById(R.id.recyclerViewAvailableStudents)
        textViewNoEnrolledStudents = findViewById(R.id.textViewNoEnrolledStudents)
        textViewNoAvailableStudents = findViewById(R.id.textViewNoAvailableStudents)

        // Validate currentBatchId for blankness, as it's now a String
        if (currentBatchId.isBlank()) {
            Toast.makeText(this, "Error: Batch ID not found.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Batch ID not passed to BatchEnrollmentActivity or is blank.")
            finish()
            return
        }

        textViewBatchEnrollmentInfo.text = "Managing student enrollments for: $currentBatchName"

        enrolledStudentList = ArrayList<Student>()
        availableStudentList = ArrayList<Student>()

        enrolledStudentsAdapter = StudentEnrollmentAdapter(this, enrolledStudentList, true, this)
        recyclerViewEnrolledStudents.layoutManager = LinearLayoutManager(this)
        recyclerViewEnrolledStudents.adapter = enrolledStudentsAdapter

        availableStudentsAdapter = StudentEnrollmentAdapter(this, availableStudentList, false, this)
        recyclerViewAvailableStudents.layoutManager = LinearLayoutManager(this)
        recyclerViewAvailableStudents.adapter = availableStudentsAdapter

        val studentRepository = StudentRepository()
        val studentBatchLinkRepository = StudentBatchLinkRepository()

        val factory = BatchEnrollmentViewModelFactory(studentRepository, studentBatchLinkRepository)
        batchEnrollmentViewModel = ViewModelProvider(this, factory).get(BatchEnrollmentViewModel::class.java)

        // Pass String ID to ViewModel
        batchEnrollmentViewModel.setBatchId(currentBatchId)

        batchEnrollmentViewModel.enrolledStudents.observe(this) { students ->
            Log.d(TAG, "Enrolled Students LiveData updated. Size: ${students.size}")
            enrolledStudentsAdapter.setStudents(students)
            textViewNoEnrolledStudents.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
        }

        batchEnrollmentViewModel.availableStudents.observe(this) { students ->
            Log.d(TAG, "Available Students LiveData updated. Size: ${students.size}")
            availableStudentsAdapter.setStudents(students)
            textViewNoAvailableStudents.visibility = if (students.isEmpty()) View.VISIBLE else View.GONE
        }

        batchEnrollmentViewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading state: $isLoading")
        }

        batchEnrollmentViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error/Success message: $it")
                batchEnrollmentViewModel.clearErrorMessage()
            }
        }
    }

    override fun onEnrollStudent(student: Student, position: Int) {
        Log.i(TAG, "Attempting to enroll student: ${student.fullName} (ID: ${student.id}) into batch ID: $currentBatchId")
        // Pass String IDs to ViewModel
        batchEnrollmentViewModel.enrollStudent(student.id, currentBatchId, student.fullName)
    }

    override fun onUnenrollStudent(student: Student, position: Int) {
        Log.i(TAG, "Attempting to unenroll student: ${student.fullName} (ID: ${student.id}) from batch ID: $currentBatchId")

        AlertDialog.Builder(this)
            .setTitle("Confirm Unenrollment")
            .setMessage("Are you sure you want to unenroll ${student.fullName} from $currentBatchName?")
            .setPositiveButton("Unenroll") { _, _ ->
                // Pass String IDs to ViewModel
                batchEnrollmentViewModel.unenrollStudent(student.id, currentBatchId, student.fullName)
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

    override fun onResume() {
        super.onResume()
    }
}