// File: main/java/com/aquaa/edusoul/viewmodels/MarkAttendanceViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.BatchRepository

class MarkAttendanceViewModelFactory(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val classSessionRepository: ClassSessionRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarkAttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarkAttendanceViewModel(
                attendanceRepository,
                studentRepository,
                classSessionRepository,
                subjectRepository,
                batchRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}