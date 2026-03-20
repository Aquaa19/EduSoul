// In main/java/com/aquaa/edusoul/viewmodels/BatchAssignmentViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchAssignmentRepository

class BatchAssignmentViewModelFactory(private val repository: BatchAssignmentRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BatchAssignmentViewModel::class.java)) {
            return BatchAssignmentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}