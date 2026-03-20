// main/java/com/aquaa/edusoul/viewmodels/AssignHomeworkViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchAssignmentRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.StudentAssignmentRepository
import com.aquaa.edusoul.repositories.StudentRepository

class AssignHomeworkViewModelFactory(
    private val homeworkRepository: HomeworkRepository,
    private val batchRepository: BatchRepository,
    private val batchAssignmentRepository: BatchAssignmentRepository,
    private val studentRepository: StudentRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssignHomeworkViewModel::class.java)) {
            return AssignHomeworkViewModel(
                homeworkRepository,
                batchRepository,
                batchAssignmentRepository,
                studentRepository,
                studentAssignmentRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}