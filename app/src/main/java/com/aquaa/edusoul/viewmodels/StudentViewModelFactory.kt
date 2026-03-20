// File: com/aquaa/edusoul/viewmodels/StudentViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository // Import your new UserRepository

/**
 * Factory for creating instances of StudentViewModel, injecting the StudentRepository and UserRepository.
 */
class StudentViewModelFactory(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository // Add UserRepository to the constructor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // Suppress warning for type casting to T
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel is StudentViewModel
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            // Pass both repositories to the StudentViewModel constructor
            return StudentViewModel(studentRepository, userRepository) as T
        }
        // If an unknown ViewModel is requested, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}