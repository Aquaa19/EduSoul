// File: main/java/com/aquaa/edusoul/viewmodels/ParentResourceViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.LearningResourceRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
// REMOVED: import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
// REMOVED: import com.aquaa.edusoul.repositories.UserRepository

class ParentResourceViewModelFactory(
    // Corrected order and removed unnecessary repositories to match ParentResourceViewModel's constructor
    private val studentRepository: StudentRepository,
    private val learningResourceRepository: LearningResourceRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentResourceViewModel::class.java)) {
            return ParentResourceViewModel(
                studentRepository, // Pass in correct order
                learningResourceRepository, // Pass in correct order
                subjectRepository, // Pass in correct order
                batchRepository // Pass in correct order
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}