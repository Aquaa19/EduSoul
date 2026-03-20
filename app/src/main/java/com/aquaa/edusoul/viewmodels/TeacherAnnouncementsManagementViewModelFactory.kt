// File: main/java/com/aquaa/edusoul/viewmodels/TeacherAnnouncementsManagementViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.AnnouncementRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.UserRepository

class TeacherAnnouncementsManagementViewModelFactory(
    private val announcementRepository: AnnouncementRepository,
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository,
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherAnnouncementsManagementViewModel::class.java)) {
            return TeacherAnnouncementsManagementViewModel(
                announcementRepository,
                batchRepository,
                subjectRepository,
                userRepository,
                teacherSubjectBatchLinkRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}