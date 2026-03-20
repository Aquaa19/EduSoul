// File: main/java/com/aquaa/edusoul/viewmodels/EnterScoresViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository

class EnterScoresViewModelFactory(
    private val examRepository: ExamRepository,
    private val studentRepository: StudentRepository,
    private val resultRepository: ResultRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnterScoresViewModel::class.java)) {
            return EnterScoresViewModel(
                examRepository,
                studentRepository,
                resultRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}