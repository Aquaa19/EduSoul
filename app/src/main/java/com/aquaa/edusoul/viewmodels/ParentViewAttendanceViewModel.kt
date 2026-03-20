// File: main/java/com/aquaa/edusoul/viewmodels/ParentViewAttendanceViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.liveData // Keep if other parts use it for simple emissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine // Ensure this import is for kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest // Import flatMapLatest
import kotlinx.coroutines.flow.flowOf // Import flowOf for empty or single value flows
import android.util.Log // Import Log
import androidx.lifecycle.asFlow
import com.aquaa.edusoul.models.Attendance
import com.aquaa.edusoul.models.ClassSession
import com.aquaa.edusoul.models.ParentAttendanceDetail
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository

class ParentViewAttendanceViewModel(
    private val studentRepository: StudentRepository,
    private val attendanceRepository: AttendanceRepository,
    private val classSessionRepository: ClassSessionRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "ParentViewAttendanceVM"

    private val _parentUserId = MutableLiveData<String>()
    private val _filterCriteria = MutableLiveData<Triple<String?, String?, String?>>()

    val childrenOfParent: LiveData<List<Student>> = _parentUserId.switchMap { parentId ->
        if (parentId.isNotBlank()) {
            studentRepository.getStudentsByParent(parentId).asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList())
        }
    }

    val attendanceReport: LiveData<List<ParentAttendanceDetail>> =
        _filterCriteria.asFlow().flatMapLatest { (studentId, startDate, endDate) ->
            if (studentId != null && studentId.isNotBlank() && !startDate.isNullOrBlank() && !endDate.isNullOrBlank()) {
                Log.d(TAG, "Fetching attendance report for student: $studentId, StartDate: $startDate, EndDate: $endDate")

                // These suspend calls (firstOrNull()) are fine here,
                // as `flatMapLatest` provides a suspend context.
                val allSubjectsList = subjectRepository.getAllSubjects().firstOrNull() ?: emptyList()
                val subjectsMap = allSubjectsList.associateBy { it.id }

                val allBatchesList = batchRepository.getAllBatches().firstOrNull() ?: emptyList()
                val batchesMap = allBatchesList.associateBy { it.id }

                // 1. Get all class sessions within the specified date range.
                // This call returns List<ClassSession>, convert it to Flow<List<ClassSession>> using flowOf.
                val sessionsInDateRange = classSessionRepository.getClassSessionsForDateRange(startDate, endDate)
                val sessionsInDateRangeFlow = flowOf(sessionsInDateRange) // Convert List to Flow

                // 2. Get all attendance records for the specific student.
                val allAttendanceForStudentFlow = attendanceRepository.getAttendanceForStudent(studentId)

                // Combine these two flows. The transform lambda is now correctly suspend.
                sessionsInDateRangeFlow.combine(allAttendanceForStudentFlow) {
                        sessions: List<ClassSession>, allStudentAttendance: List<Attendance> ->
                    val detailedAttendanceList = mutableListOf<ParentAttendanceDetail>()

                    val sessionsMap = sessions.associateBy { it.id }

                    val filteredAttendanceForStudent = allStudentAttendance.filter { attendance: Attendance ->
                        sessionsMap.containsKey(attendance.classSessionId)
                    }

                    for (attendance in filteredAttendanceForStudent) {
                        val session = sessionsMap[attendance.classSessionId]
                        if (session != null) {
                            val subject = subjectsMap[session.subjectId]
                            val batch = batchesMap[session.batchId]

                            detailedAttendanceList.add(
                                ParentAttendanceDetail(
                                    attendanceId = attendance.id,
                                    classSessionId = attendance.classSessionId,
                                    sessionDate = session.sessionDate,
                                    startTime = session.startTime,
                                    endTime = session.endTime,
                                    subjectName = subject?.subjectName ?: "Unknown Subject",
                                    batchName = batch?.batchName ?: "Unknown Batch",
                                    attendanceStatus = attendance.attendanceStatus,
                                    remarks = attendance.remarks
                                )
                            )
                        }
                    }
                    Log.d(TAG, "Processed ${detailedAttendanceList.size} attendance details for student $studentId in date range $startDate to $endDate.")
                    detailedAttendanceList.sortedWith(compareBy({ it.sessionDate }, { it.startTime }))
                }
            } else {
                Log.d(TAG, "Invalid filter criteria: studentId=$studentId, startDate=$startDate, endDate=$endDate. Emitting empty list.")
                flowOf(emptyList()) // Return an empty flow if criteria are not met
            }
        }.asLiveData(Dispatchers.IO) // Convert the final Flow to LiveData


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setParentUserId(userId: String) {
        _parentUserId.value = userId
    }

    fun selectChildAndDateRange(studentId: String, startDate: String, endDate: String) {
        _filterCriteria.value = Triple(studentId, startDate, endDate)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}