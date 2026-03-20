// File: main/java/com/aquaa/edusoul/viewmodels/TeacherViewScheduleViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.RecurringClassSessionRepository // Keep the import if other factories use it, but not needed for THIS ViewModel
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository

// Removed: FIX comment, as this is the correction
class TeacherViewScheduleViewModelFactory(
    private val classSessionRepository: ClassSessionRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository
    // Removed: private val recurringClassSessionRepository: RecurringClassSessionRepository // This dependency is not needed by TeacherViewScheduleViewModel
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherViewScheduleViewModel::class.java)) {
            // Removed: FIX comment, as this is the correction
            return TeacherViewScheduleViewModel(
                classSessionRepository,
                subjectRepository,
                batchRepository,
                userRepository
                // Removed: recurringClassSessionRepository // Do not pass it to TeacherViewScheduleViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}