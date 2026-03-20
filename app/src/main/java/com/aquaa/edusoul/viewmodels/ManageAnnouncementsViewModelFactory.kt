// File: main/java/com/aquaa/edusoul/viewmodels/ManageAnnouncementsViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.AnnouncementRepository

class ManageAnnouncementsViewModelFactory(private val repository: AnnouncementRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageAnnouncementsViewModel::class.java)) {
            return ManageAnnouncementsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
