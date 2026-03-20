// File: main/java/com/aquaa/edusoul/viewmodels/StudentViewModel.kt
package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentViewModel(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // LiveData for all students to display in the RecyclerView
    val allStudents: LiveData<List<Student>> = studentRepository.getAllStudents().asLiveData(Dispatchers.IO)

    // LiveData for parent users to populate the spinner in the dialog
    val parentUsers: LiveData<List<User>> = userRepository.getAllParentUsers().asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Adds a new student.
     * Changed id parameters to String?
     */
    fun addStudent(
        fullName: String,
        dateOfBirth: String?,
        gender: String?,
        gradeOrClass: String?,
        admissionNumber: String,
        admissionDate: String?,
        parentUserId: String?, // Changed from Long? to String?
        schoolName: String?,
        address: String?,
        profileImagePath: String?,
        notes: String?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newStudent = Student(
                    id = "", // Firestore will generate String ID
                    fullName = fullName,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    gradeOrClass = gradeOrClass,
                    admissionNumber = admissionNumber,
                    admissionDate = admissionDate,
                    parentUserId = parentUserId, // Now String?
                    schoolName = schoolName,
                    address = address,
                    profileImagePath = profileImagePath,
                    notes = notes
                )
                // Repository's insertStudent returns String
                val newRowId = studentRepository.insertStudent(newStudent)
                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) { // Check for non-blank String ID
                        _errorMessage.value = "Student added successfully."
                    } else {
                        _errorMessage.value = "Failed to add student (Admission number might exist or other error)."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error adding student: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Updates an existing student.
     * Changed id parameters to String?
     */
    fun updateStudent(
        studentId: String, // Changed from Long to String
        fullName: String,
        dateOfBirth: String?,
        gender: String?,
        gradeOrClass: String?,
        admissionNumber: String,
        admissionDate: String?,
        parentUserId: String?, // Changed from Long? to String?
        schoolName: String?,
        address: String?,
        profileImagePath: String?,
        notes: String?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedStudent = Student(
                    id = studentId, // Student ID is now String
                    fullName = fullName,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    gradeOrClass = gradeOrClass,
                    admissionNumber = admissionNumber,
                    admissionDate = admissionDate,
                    parentUserId = parentUserId, // Now String?
                    schoolName = schoolName,
                    address = address,
                    profileImagePath = profileImagePath,
                    notes = notes
                )
                // Repository's updateStudent returns Int (rows affected)
                val rowsAffected = studentRepository.updateStudent(updatedStudent)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Student updated successfully."
                    } else {
                        _errorMessage.value = "Failed to update student."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating student: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Deletes a student by ID.
     * Changed studentId parameter to String.
     */
    fun deleteStudent(studentId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Repository's deleteStudentById returns Int (rows affected)
                val rowsAffected = studentRepository.deleteStudentById(studentId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Student deleted successfully."
                    } else {
                        _errorMessage.value = "Failed to delete student."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting student: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}