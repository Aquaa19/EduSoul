// In main/java/com/aquaa/edusoul/viewmodels/AssignHomeworkViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.BatchAssignment
import com.aquaa.edusoul.models.Homework
import com.aquaa.edusoul.models.StudentAssignment
import com.aquaa.edusoul.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AssignHomeworkViewModel(
    private val homeworkRepository: HomeworkRepository,
    private val batchRepository: BatchRepository,
    private val batchAssignmentRepository: BatchAssignmentRepository,
    private val studentRepository: StudentRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository
) : ViewModel() {

    private val _homework = MutableLiveData<Homework?>()
    val homework: LiveData<Homework?> get() = _homework

    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData(Dispatchers.IO)

    private val _assignedBatches = MutableLiveData<List<BatchAssignment>>()
    val assignedBatches: LiveData<List<BatchAssignment>> get() = _assignedBatches

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    fun loadHomework(homeworkId: String) {
        viewModelScope.launch {
            _homework.value = homeworkRepository.getHomeworkById(homeworkId)
            // Listen for assigned batches to this homework in real-time
            batchAssignmentRepository.getBatchAssignmentsByHomework(homeworkId).collect {
                _assignedBatches.value = it
            }
        }
    }

    fun assignToBatch(homeworkId: String, batchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if batch assignment already exists
            val existingBatchAssignment = batchAssignmentRepository.getBatchAssignment(homeworkId, batchId)
            if (existingBatchAssignment != null) {
                withContext(Dispatchers.Main) {
                    _toastMessage.postValue("Homework is already assigned to this batch.")
                }
                return@launch
            }

            // Insert new BatchAssignment
            val newBatchAssignment = BatchAssignment(homeworkId = homeworkId, batchId = batchId)
            val batchAssignmentId = batchAssignmentRepository.insertBatchAssignment(newBatchAssignment)

            if (batchAssignmentId.isNotBlank()) {
                // Now assign to students in that batch
                studentRepository.getStudentsByBatch(batchId).collect { studentsList ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val currentDate = sdf.format(Date())

                    studentsList.forEach { student ->
                        // Prevent duplicate StudentAssignments
                        val existingStudentAssignment = studentAssignmentRepository.getStudentAssignment(homeworkId, student.id)
                        if (existingStudentAssignment == null) {
                            val studentAssignment = StudentAssignment(
                                homeworkId = homeworkId,
                                studentId = student.id,
                                assignedDate = currentDate,
                                dueDate = _homework.value?.dueDate,
                                status = StudentAssignment.STATUS_ASSIGNED,
                                submissionDate = null,
                                marksObtained = null,
                                remarks = null,
                                submissionPath = null
                            )
                            studentAssignmentRepository.insertStudentAssignment(studentAssignment)
                        } else {
                            // Optionally update the existing assignment if needed, e.g., dueDate
                            // studentAssignmentRepository.updateStudentAssignment(existingStudentAssignment.copy(dueDate = _homework.value?.dueDate))
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    _toastMessage.postValue("Homework assigned to batch and its students.")
                }
            } else {
                withContext(Dispatchers.Main) {
                    _toastMessage.postValue("Failed to assign homework to batch.")
                }
            }
        }
    }

    fun removeBatchAssignment(homeworkId: String, batchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete the batch assignment link
            val batchAssignmentCompositeId = "${homeworkId}_${batchId}"
            val batchAssignmentDeleted = batchAssignmentRepository.deleteBatchAssignmentById(batchAssignmentCompositeId) > 0

            // Delete associated student assignments for this homework in this batch
            if (batchAssignmentDeleted) {
                val studentsInBatch = studentRepository.getStudentsByBatch(batchId).firstOrNull() ?: emptyList()
                studentsInBatch.forEach { student ->
                    val existingStudentAssignment = studentAssignmentRepository.getStudentAssignment(homeworkId, student.id)
                    existingStudentAssignment?.let { assignment ->
                        studentAssignmentRepository.deleteStudentAssignmentById(assignment.id)
                    }
                }
                withContext(Dispatchers.Main) {
                    _toastMessage.postValue("Assignment removed from batch and its students.")
                }
            } else {
                withContext(Dispatchers.Main) {
                    _toastMessage.postValue("Failed to remove batch assignment.")
                }
            }
        }
    }
}