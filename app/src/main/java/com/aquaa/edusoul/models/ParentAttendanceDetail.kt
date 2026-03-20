package com.aquaa.edusoul.models

/**
 * Represents a detailed attendance record for a single class session from a parent's perspective.
 * Converted from Java POJO to Kotlin data class.
 */
data class ParentAttendanceDetail(
    var attendanceId: String = "", // Changed to 'String' for Firestore compatibility
    val classSessionId: String="", // Changed to 'String' for Firestore compatibility
    val sessionDate: String="",
    val startTime: String="",
    val endTime: String="",
    val subjectName: String="",
    val batchName: String="",
    val attendanceStatus: String="", // e.g., Present, Absent, Late
    val remarks: String?=null // Remarks from teacher, if any
)