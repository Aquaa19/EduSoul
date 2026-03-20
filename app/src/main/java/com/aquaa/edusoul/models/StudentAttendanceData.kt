package com.aquaa.edusoul.models

/**
 * A modern, immutable data class to hold the UI state for a student's attendance.
 * This replaces the old StudentAttendanceStatus.java POJO.
 */
data class StudentAttendanceData(
    val student: Student=Student(),
    var status: String = Attendance.STATUS_PRESENT,
    var remarks: String? = null,
    val attendanceId: String? = null
)
