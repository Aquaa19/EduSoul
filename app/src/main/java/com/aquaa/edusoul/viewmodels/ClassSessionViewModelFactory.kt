package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.*

class ClassSessionViewModelFactory(
    private val classSessionRepository: ClassSessionRepository,
    private val recurringClassSessionRepository: RecurringClassSessionRepository, // ADDED to constructor
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassSessionViewModel::class.java)) {
            return ClassSessionViewModel(
                classSessionRepository,
                recurringClassSessionRepository, // PASSED HERE
                batchRepository,
                subjectRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
