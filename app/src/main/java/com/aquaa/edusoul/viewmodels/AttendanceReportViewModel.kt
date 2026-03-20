// File: EduSoul/app/src/main/java/com/aquaa/edusoul/viewmodels/AttendanceReportViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.*
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentDailyAttendance
import com.aquaa.edusoul.models.AttendanceReportDetails
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.SessionAttendanceDetail
import com.aquaa.edusoul.repositories.AttendanceRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import java.text.DecimalFormat
import android.util.Log // Import Log
import com.aquaa.edusoul.models.Attendance

class AttendanceReportViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val studentRepository: StudentRepository,
    private val batchRepository: BatchRepository,
    private val classSessionRepository: ClassSessionRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val TAG = "AttendanceReportVM" // TAG for logging

    private data class ReportRequest(val batchId: String, val date: String)
    private val _reportRequest = MutableLiveData<ReportRequest>()

    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData(Dispatchers.IO)

    val processedReport: LiveData<List<StudentDailyAttendance>> = _reportRequest.switchMap { request ->
        liveData(Dispatchers.IO) {
            Log.d(TAG, "Processing report for Batch ID: ${request.batchId}, Date: ${request.date}")

            val studentsInBatch = studentRepository.getStudentsByBatch(request.batchId).firstOrNull() ?: emptyList()
            Log.d(TAG, "Found ${studentsInBatch.size} students in batch ${request.batchId}.")

            val markedAttendanceDetails = mutableListOf<AttendanceReportDetails>()

            val allClassSessionsForDate = classSessionRepository.getClassSessionsForDate(request.date).firstOrNull() ?: emptyList()
            Log.d(TAG, "Found ${allClassSessionsForDate.size} class sessions for date ${request.date}.")

            val relevantSessions = allClassSessionsForDate.filter { it.batchId == request.batchId }
            Log.d(TAG, "Found ${relevantSessions.size} relevant sessions for batch ${request.batchId} on date ${request.date}.")


            for (session in relevantSessions) {
                Log.d(TAG, "Fetching attendance for session ID: ${session.id} (Subject: ${session.subjectId}, Batch: ${session.batchId}, Time: ${session.startTime})")
                val attendanceRecordsForSession = attendanceRepository.getAttendanceForSession(session.id).firstOrNull() ?: emptyList()
                Log.d(TAG, "Found ${attendanceRecordsForSession.size} attendance records for session ID: ${session.id}.")

                for (attendance in attendanceRecordsForSession) {
                    val student = studentsInBatch.find { it.id == attendance.studentId }
                    val subject = subjectRepository.getSubjectById(session.subjectId)

                    if (student != null && subject != null) {
                        markedAttendanceDetails.add(
                            AttendanceReportDetails(
                                attendance = attendance,
                                studentName = student.fullName,
                                subjectName = subject.subjectName,
                                sessionStartTime = session.startTime,
                                sessionEndTime = session.endTime
                            )
                        )
                    } else {
                        if (student == null) Log.w(TAG, "Student with ID ${attendance.studentId} not found for attendance record ${attendance.id}")
                        if (subject == null) Log.w(TAG, "Subject with ID ${session.subjectId} not found for session ${session.id}")
                    }
                }
            }
            Log.d(TAG, "Total marked attendance details collected: ${markedAttendanceDetails.size}")

            val allStudentsDailyAttendance = studentsInBatch.map { student ->
                val studentDetails = markedAttendanceDetails.filter { it.attendance.studentId == student.id }
                val sessionDetails = studentDetails.map { detail ->
                    SessionAttendanceDetail(
                        sessionTime = "${detail.sessionStartTime} - ${detail.sessionEndTime}",
                        subjectName = detail.subjectName,
                        status = detail.attendance.attendanceStatus,
                        remarks = detail.attendance.remarks
                    )
                }.sortedBy { it.sessionTime }
                StudentDailyAttendance(student, sessionDetails)
            }.sortedBy { it.student.fullName }

            emit(allStudentsDailyAttendance)
            Log.d(TAG, "Emitted final processed report with ${allStudentsDailyAttendance.size} students.")
        }
    }

    val reportSummary: LiveData<String> = processedReport.map { reportList ->
        generateSummary(reportList)
    }

    fun generateReport(batchId: String, date: String) {
        if (batchId.isBlank() || date.isBlank()) {
            Log.w(TAG, "generateReport called with blank batchId or date. BatchId: '$batchId', Date: '$date'")
            // Optionally, emit an empty list or an error message to processedReport/errorMessage LiveData
            // For now, just return
            _reportRequest.value = ReportRequest("", "") // Clear previous request
            return
        }
        _reportRequest.value = ReportRequest(batchId, date)
    }

    private fun processReportData( // This function is not currently used by processedReport LiveData, but exists in code. Keep for completeness.
        allStudents: List<Student>,
        markedAttendanceDetails: List<AttendanceReportDetails>
    ): List<StudentDailyAttendance> {
        val detailsByStudentId = markedAttendanceDetails.groupBy { it.attendance.studentId }
        return allStudents.map { student ->
            val studentDetails = detailsByStudentId[student.id] ?: emptyList()
            val sessionDetails = studentDetails.map { detail ->
                SessionAttendanceDetail(
                    sessionTime = "${detail.sessionStartTime} - ${detail.sessionEndTime}",
                    subjectName = detail.subjectName,
                    status = detail.attendance.attendanceStatus,
                    remarks = detail.attendance.remarks
                )
            }
            StudentDailyAttendance(student, sessionDetails)
        }
    }

    private fun generateSummary(reportList: List<StudentDailyAttendance>): String {
        if (reportList.isEmpty()) return "No students found in this batch."

        val totalStudents = reportList.size
        var totalPresent = 0

        reportList.forEach { dailyAttendance ->
            // A student is considered 'present' if they have at least one 'Present' or 'Late' status for any session.
            // Ensure the string comparison matches the constants defined in Attendance.kt
            if (dailyAttendance.sessionDetails.any { it.status == Attendance.STATUS_PRESENT || it.status == Attendance.STATUS_LATE }) {
                totalPresent++
            }
        }
        val totalAbsent = totalStudents - totalPresent

        val percentage = if (totalStudents > 0) (totalPresent.toDouble() / totalStudents.toDouble()) * 100 else 0.0
        val df = DecimalFormat("#.##")

        return "Summary: Total Students: $totalStudents | Present: $totalPresent | Absent/Unmarked: $totalAbsent | Overall: ${df.format(percentage)}%"
    }
}