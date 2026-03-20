// In main/java/com/aquaa/edusoul/viewmodels/StudentAssignmentViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentAssignmentRepository

class StudentAssignmentViewModelFactory(private val repository: StudentAssignmentRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentAssignmentViewModel::class.java)) {
            return StudentAssignmentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}