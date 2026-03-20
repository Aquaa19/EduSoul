package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParentStudentProfileViewModel(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository // Needed if fetching parent details or for AuthManager logic
) : ViewModel() {

    private val TAG = "ParentStudentProfileVM"

    private val _parentUserId = MutableLiveData<String>()
    private val _selectedChildId = MutableLiveData<String>()

    // LiveData for all children of the logged-in parent (for the spinner)
    val childrenOfParent: LiveData<List<Student>> = _parentUserId.switchMap { parentId ->
        if (parentId.isNotBlank()) {
            // FIX: Corrected method name from getStudentsByParentId to getStudentsByParent
            studentRepository.getStudentsByParent(parentId).asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList())
        }
    }

    // LiveData for the detailed profile of the selected student
    val selectedStudentProfile: LiveData<Student?> = _selectedChildId.switchMap { studentId ->
        liveData(Dispatchers.IO) {
            if (studentId.isNotBlank()) {
                emit(studentRepository.getStudentById(studentId)) // Suspend call
            } else {
                emit(null)
            }
        }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setParentUserId(userId: String) {
        _parentUserId.value = userId
    }

    fun selectChild(studentId: String) {
        _selectedChildId.value = studentId
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}