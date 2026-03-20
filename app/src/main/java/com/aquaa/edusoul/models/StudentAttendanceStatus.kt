// File: main/java/com/aquaa/edusoul/models/StudentAttendanceStatus.kt
package com.aquaa.edusoul.models

/*
 * StudentAttendanceStatus: A helper model data class to hold summarized
 * attendance information for a single student, suitable for reports.
 * Converted from Java POJO to Kotlin data class.
 */
data class StudentAttendanceStatus(
    val studentId: String="", // Changed to 'String' for Firestore compatibility
    val studentName: String="",
    val admissionNumber: String?=null, // Made nullable, was String in Java
    val totalClasses: Int=0,
    val presentDays: Int=0,
    val absentDays: Int=0,
    val lateDays: Int=0,
    val percentage: Double=0.0
)