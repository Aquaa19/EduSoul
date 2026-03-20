// File: com/aquaa/edusoul/viewmodels/SubjectViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.SubjectRepository // Import your SubjectRepository

// Modify the constructor to accept SubjectRepository
class SubjectViewModelFactory(private val repository: SubjectRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // Suppress warning for type casting
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel is SubjectViewModel
        if (modelClass.isAssignableFrom(SubjectViewModel::class.java)) {
            // Return a new instance of SubjectViewModel with the provided SubjectRepository
            return SubjectViewModel(repository) as T
        }
        // If an unknown ViewModel is requested, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}