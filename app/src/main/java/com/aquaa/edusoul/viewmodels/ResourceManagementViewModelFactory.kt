// File: src/main/java/com/aquaa/edusoul/viewmodels/ResourceManagementViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.LearningResourceRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.UserRepository

class ResourceManagementViewModelFactory(
    private val resourceRepository: LearningResourceRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository,
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResourceManagementViewModel::class.java)) {
            return ResourceManagementViewModel(
                resourceRepository,
                subjectRepository,
                batchRepository,
                userRepository,
                teacherSubjectBatchLinkRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}