// File: main/java/com/aquaa/edusoul/viewmodels/ParentViewAttendanceViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository

class ParentViewAttendanceViewModelFactory(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val classSessionRepository: ClassSessionRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentViewAttendanceViewModel::class.java)) {
            return ParentViewAttendanceViewModel(
                studentRepository,
                attendanceRepository,
                classSessionRepository,
                subjectRepository,
                batchRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}