// File: main/java/com/aquaa/edusoul/viewmodels/TeacherDashboardViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.StudentAssignmentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.StudentRepository

class TeacherDashboardViewModelFactory(
    private val classSessionRepository: ClassSessionRepository,
    private val homeworkRepository: HomeworkRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository,
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository,
    private val studentRepository: StudentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherDashboardViewModel::class.java)) {
            return TeacherDashboardViewModel(
                classSessionRepository,
                homeworkRepository,
                studentAssignmentRepository,
                subjectRepository,
                batchRepository,
                userRepository,
                attendanceRepository,
                teacherSubjectBatchLinkRepository,
                studentRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}