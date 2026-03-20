package com.aquaa.edusoul.models

/**
 * Represents the attendance status for one student for a single class session in a report.
 */
data class SessionAttendanceDetail(
    val sessionTime: String="",
    val subjectName: String="",
    val status: String="",
    val remarks: String?=null
)