// File: main/java/com/aquaa/edusoul/viewmodels/ResultViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.ExamRepository // Added
import com.aquaa.edusoul.repositories.StudentRepository // Added
import com.aquaa.edusoul.repositories.SubjectRepository // Added
import com.aquaa.edusoul.repositories.UserRepository // Added

class ResultViewModelFactory(
    private val resultRepository: ResultRepository, // Keep this one
    private val examRepository: ExamRepository,     // Add this one
    private val studentRepository: StudentRepository, // Add this one
    private val subjectRepository: SubjectRepository, // Add this one
    private val userRepository: UserRepository      // Add this one
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResultViewModel::class.java)) {
            return ResultViewModel(
                resultRepository,
                examRepository,
                studentRepository,
                subjectRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}