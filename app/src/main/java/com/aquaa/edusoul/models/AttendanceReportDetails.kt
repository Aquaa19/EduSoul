package com.aquaa.edusoul.models

import androidx.room.Embedded

/**
 * A data class to hold the result of a JOIN query for the attendance report.
 * It combines the Attendance record with details from the Student and Subject tables.
 */
data class AttendanceReportDetails(
    // Embeds all columns from the Attendance entity
    @Embedded
    val attendance: Attendance,

    // Specific columns from other tables
    val studentName: String="",
    val subjectName: String="",
    val sessionStartTime: String="",
    val sessionEndTime: String=""
)