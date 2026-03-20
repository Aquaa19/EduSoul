// File: main/java/com/aquaa/edusoul/models/StudentResultDetails.kt
package com.aquaa.edusoul.models

/*
 * This is a helper model data class to hold the combined, detailed
 * information for a single student's result for display in reports.
 * It now includes the unique ID of the result record itself.
 * Converted from Java POJO to Kotlin data class.
 */
data class StudentResultDetails(
    var resultId: String = "", // Corrected default ID to empty string for Firebase compatibility
    val studentName: String = "",
    val admissionNumber: String = "",
    val marksObtained: Double = 0.0,
    val maxMarks: Int = 0,
    val teacherName: String? = null, // Made nullable, was String in Java
    val examId: String="", // Added: The ID of the exam
    val resultRemarks: String? =null// Added: Remarks specific to this result
)