// main/java/com/aquaa/edusoul/viewmodels/GradeHomeworkViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchAssignmentRepository
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.StudentAssignmentRepository
import com.aquaa.edusoul.repositories.StudentRepository

class GradeHomeworkViewModelFactory(
    private val homeworkRepository: HomeworkRepository,
    private val studentRepository: StudentRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val batchAssignmentRepository: BatchAssignmentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GradeHomeworkViewModel::class.java)) {
            return GradeHomeworkViewModel(
                homeworkRepository,
                studentRepository,
                studentAssignmentRepository,
                batchAssignmentRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}