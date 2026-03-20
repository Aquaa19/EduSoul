// File: EduSoul/app/src/main/java/com/aquaa/edusoul/viewmodels/ManageExamsViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository // Import UserRepository

class ManageExamsViewModelFactory(
    private val examRepository: ExamRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository // Add UserRepository to the constructor
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageExamsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManageExamsViewModel(
                examRepository,
                subjectRepository,
                batchRepository,
                userRepository // Pass UserRepository to the ViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}