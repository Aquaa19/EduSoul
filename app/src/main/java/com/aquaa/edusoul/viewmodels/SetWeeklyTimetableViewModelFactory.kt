package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.RecurringClassSessionRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository

class SetWeeklyTimetableViewModelFactory(
    private val recurringClassSessionRepository: RecurringClassSessionRepository,
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetWeeklyTimetableViewModel::class.java)) {
            return SetWeeklyTimetableViewModel(
                recurringClassSessionRepository,
                batchRepository,
                subjectRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}