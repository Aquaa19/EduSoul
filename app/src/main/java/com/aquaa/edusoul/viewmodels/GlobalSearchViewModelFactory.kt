// File: main/java/com/aquaa/edusoul/viewmodels/GlobalSearchViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.BatchRepository

class GlobalSearchViewModelFactory(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository
    // Add other repositories here
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GlobalSearchViewModel::class.java)) {
            return GlobalSearchViewModel(
                studentRepository,
                userRepository,
                subjectRepository,
                batchRepository
                // Pass other repositories here
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}