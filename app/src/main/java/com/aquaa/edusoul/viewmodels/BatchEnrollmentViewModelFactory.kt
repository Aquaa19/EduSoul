// File: com/aquaa/edusoul/viewmodels/BatchEnrollmentViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.StudentBatchLinkRepository

/**
 * Factory for creating instances of [BatchEnrollmentViewModel],
 * injecting [StudentRepository] and [StudentBatchLinkRepository].
 */
class BatchEnrollmentViewModelFactory(
    private val studentRepository: StudentRepository,
    private val studentBatchLinkRepository: StudentBatchLinkRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BatchEnrollmentViewModel::class.java)) {
            return BatchEnrollmentViewModel(studentRepository, studentBatchLinkRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}