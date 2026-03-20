// File: main/java/com/aquaa/edusoul/viewmodels/ParentAnnouncementsViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.AnnouncementRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository

class ParentAnnouncementsViewModelFactory(
    // Ensure the order and type of parameters match the ParentAnnouncementsViewModel constructor
    private val announcementRepository: AnnouncementRepository,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val batchRepository: BatchRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentAnnouncementsViewModel::class.java)) {
            return ParentAnnouncementsViewModel(
                announcementRepository,
                userRepository,
                studentRepository,
                batchRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}