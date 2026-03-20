// File: main/java/com/aquaa/edusoul/viewmodels/AttendanceReportViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.BatchRepository // ADD THIS IMPORT
import com.aquaa.edusoul.repositories.ClassSessionRepository // ADDED
import com.aquaa.edusoul.repositories.SubjectRepository // ADDED

class AttendanceReportViewModelFactory(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val batchRepository: BatchRepository, // ADD THIS PARAMETER
    private val classSessionRepository: ClassSessionRepository, // ADDED
    private val subjectRepository: SubjectRepository // ADDED
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceReportViewModel(
                attendanceRepository,
                studentRepository,
                batchRepository, // PASS THIS PARAMETER
                classSessionRepository, // PASSED
                subjectRepository // PASSED
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}