package com.aquaa.edusoul.models

/**
 * Represents the attendance for one student across all their sessions on a given day for a report.
 */
data class StudentDailyAttendance(
    val student: Student,
    val sessionDetails: List<SessionAttendanceDetail>
)