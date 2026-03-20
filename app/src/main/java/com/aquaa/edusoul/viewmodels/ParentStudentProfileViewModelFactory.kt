// File: main/java/com/aquaa/edusoul/viewmodels/ParentStudentProfileViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository

class ParentStudentProfileViewModelFactory(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentStudentProfileViewModel::class.java)) {
            return ParentStudentProfileViewModel(studentRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}