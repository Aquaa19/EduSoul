// File: main/java/com/aquaa/edusoul/viewmodels/ParentPerformanceOverviewViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.SubjectRepository

class ParentPerformanceOverviewViewModelFactory(
    private val studentRepository: StudentRepository,
    private val resultRepository: ResultRepository,
    private val examRepository: ExamRepository,
    private val subjectRepository: SubjectRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentPerformanceOverviewViewModel::class.java)) {
            return ParentPerformanceOverviewViewModel(
                studentRepository,
                resultRepository,
                examRepository,
                subjectRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}