// File: EduSoul/app/src/main/java/com/aquaa/edusoul/activities/manager/ManagerDashboardActivity.kt
package com.aquaa.edusoul.activities.manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.activities.messages.ConversationListActivity
import com.aquaa.edusoul.auth.AuthManager
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.aquaa.edusoul.models.InstituteAttendance
import com.aquaa.edusoul.repositories.InstituteAttendanceRepository
import com.aquaa.edusoul.repositories.StudentRepository
import java.util.UUID

class ManagerDashboardActivity : BaseActivity() {

    private val TAG = "ManagerDashboardActivity"
    private lateinit var authManager: AuthManager
    private lateinit var tvWelcomeUser: TextView
    private lateinit var cardScanQrCode: MaterialCardView
    private lateinit var cardMessages: MaterialCardView

    // Repositories
    private lateinit var instituteAttendanceRepository: InstituteAttendanceRepository
    private lateinit var studentRepository: StudentRepository

    // LiveData for loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    // ActivityResultLauncher for QR Scanner
    private val qrScannerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val scannedQrCode = result.data?.getStringExtra("SCANNED_QR_CODE")
                if (!scannedQrCode.isNullOrBlank()) {
                    Log.d(TAG, "Scanned QR Code: $scannedQrCode")
                    markStudentAttendance(scannedQrCode)
                } else {
                    showToast("QR code scan cancelled or no data found.")
                }
            } else {
                showToast("QR code scanning failed or cancelled.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_dashboard)

        authManager = AuthManager(this)
        instituteAttendanceRepository = InstituteAttendanceRepository()
        studentRepository = StudentRepository()

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
        cardScanQrCode = findViewById(R.id.cardScanQrCode)
        cardMessages = findViewById(R.id.cardMessages)

        loadUserDetails()
        setupClickListeners()
    }

    private fun loadUserDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentUser = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                currentUser?.let {
                    // Use 'username' for User model as it exists there
                    tvWelcomeUser.text = "Welcome, Manager ${it.fullName ?: it.username}!"
                } ?: run {
                    tvWelcomeUser.text = "Welcome, Manager!"
                    Log.e(TAG, "Failed to load manager details.")
                }
            }
        }
    }

    private fun setupClickListeners() {
        cardScanQrCode.setOnClickListener {
            Log.d(TAG, "Scan QR Code for Attendance clicked. Launching ScanQrActivity.")
            val intent = Intent(this, ScanQrActivity::class.java)
            qrScannerLauncher.launch(intent)
        }

        cardMessages.setOnClickListener {
            Log.d(TAG, "Messages clicked. Navigating to ConversationListActivity.")
            val intent = Intent(this, ConversationListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun markStudentAttendance(studentId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            _isLoading.postValue(true)
            try {
                val student = studentRepository.getStudentById(studentId)
                if (student == null) {
                    withContext(Dispatchers.Main) {
                        showToast("Student with ID $studentId not found.")
                        Log.e(TAG, "Student with ID $studentId not found for attendance marking.")
                    }
                    return@launch
                }

                val currentUser = authManager.getLoggedInUser()
                val managerId = currentUser?.id

                if (managerId == null) {
                    withContext(Dispatchers.Main) {
                        showToast("Error: Manager not logged in.")
                        Log.e(TAG, "Manager ID is null. Cannot mark attendance.")
                    }
                    return@launch
                }

                val todayAttendance = instituteAttendanceRepository.getInstituteAttendanceForStudent(studentId, System.currentTimeMillis())
                if (todayAttendance.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        // Corrected: Use student.fullName as Student model does not have 'username'
                        showToast("${student.fullName} attendance already marked for today.")
                        Log.d(TAG, "${student.fullName} attendance already marked for today.")
                    }
                    return@launch
                }

                val newAttendance = InstituteAttendance(
                    id = UUID.randomUUID().toString(),
                    studentId = studentId,
                    markedAt = System.currentTimeMillis(),
                    status = "Arrived",
                    markedByUserId = managerId
                )

                val success = instituteAttendanceRepository.insertInstituteAttendance(newAttendance)

                withContext(Dispatchers.Main) {
                    if (success) {
                        // Corrected: Use student.fullName
                        showToast("Attendance marked for ${student.fullName}!")
                        Log.i(TAG, "Attendance successfully marked for student ${studentId}.")
                    } else {
                        // Corrected: Use student.fullName
                        showToast("Failed to mark attendance for ${student.fullName}.")
                        Log.e(TAG, "Failed to insert attendance record for student ${studentId}.")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("An error occurred: ${e.message}")
                    Log.e(TAG, "Error marking student attendance: ${e.message}", e)
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserDetails()
    }
}