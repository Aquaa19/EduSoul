// main/java/com/aquaa/edusoul/viewmodels/ParentViewHomeworkViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.StudentAssignmentRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository

class ParentViewHomeworkViewModelFactory(
    private val studentRepository: StudentRepository,
    private val homeworkRepository: HomeworkRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val subjectRepository: SubjectRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentViewHomeworkViewModel::class.java)) {
            return ParentViewHomeworkViewModel(
                studentRepository,
                homeworkRepository,
                studentAssignmentRepository,
                subjectRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}