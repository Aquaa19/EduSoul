// File: src/main/java/com/aquaa/edusoul/viewmodels/GradeHomeworkViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Homework
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentAssignment
import com.aquaa.edusoul.models.StudentAssignmentStatus
import com.aquaa.edusoul.repositories.BatchAssignmentRepository
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.StudentAssignmentRepository
import com.aquaa.edusoul.repositories.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull // Added import for firstOrNull

class GradeHomeworkViewModel(
    private val homeworkRepository: HomeworkRepository,
    private val studentRepository: StudentRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val batchAssignmentRepository: BatchAssignmentRepository
) : ViewModel() {

    private val _homework = MutableLiveData<Homework?>()
    val homework: LiveData<Homework?> get() = _homework

    private val _studentSubmissions = MutableLiveData<List<StudentAssignmentStatus>>()
    val studentSubmissions: LiveData<List<StudentAssignmentStatus>> get() = _studentSubmissions

    private val _isLoading = MutableLiveData<Boolean>() // Add isLoading LiveData
    val isLoading: LiveData<Boolean> get() = _isLoading


    // Changed homeworkId parameter type from Long to String
    fun loadSubmissions(homeworkId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true) // Set loading to true
            try {
                // homeworkRepository.getHomeworkById already expects String
                _homework.postValue(homeworkRepository.getHomeworkById(homeworkId))
                // studentAssignmentRepository.getStudentAssignmentsByHomework already expects String
                val assignments = studentAssignmentRepository.getStudentAssignmentsByHomework(homeworkId).firstOrNull() ?: emptyList()
                val students = studentRepository.getAllStudents().firstOrNull() // Fetch all students
                    ?.filter { s -> assignments.any { it.studentId == s.id } } // Filter to only students with assignments
                    ?.associateBy { it.id } ?: emptyMap() // Added ?: emptyMap() for safe initialization of map

                val statusList = assignments.mapNotNull { assignment ->
                    students[assignment.studentId]?.let { student ->
                        StudentAssignmentStatus(assignment, student.fullName)
                    }
                }
                _studentSubmissions.postValue(statusList)
            } finally {
                _isLoading.postValue(false) // Set loading to false in finally block
            }
        }
    }

    fun updateGrade(assignment: StudentAssignment) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true) // Set loading to true
            try {
                studentAssignmentRepository.updateStudentAssignment(assignment)
            } finally {
                _isLoading.postValue(false) // Set loading to false in finally block
            }
        }
    }
}