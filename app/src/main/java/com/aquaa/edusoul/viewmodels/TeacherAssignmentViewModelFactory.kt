// File: main/java/com/aquaa/edusoul/viewmodels/TeacherAssignmentViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository

class TeacherAssignmentViewModelFactory(
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherAssignmentViewModel::class.java)) {
            return TeacherAssignmentViewModel(
                subjectRepository,
                batchRepository,
                teacherSubjectBatchLinkRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}