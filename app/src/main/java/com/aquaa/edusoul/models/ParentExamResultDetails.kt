package com.aquaa.edusoul.models

import java.io.Serializable

/**
 * Model data class to encapsulate detailed exam result information for a parent's view.
 * It combines data from Result, Exam, and Subject entities.
 * Converted from Java POJO to Kotlin data class.
 */
data class ParentExamResultDetails(
    // From Result
    var resultId: String = "", // Changed to 'String' for Firestore compatibility
    val studentId: String="", // Changed to 'String' for Firestore compatibility
    val examId: String="",    // Changed to 'String' for Firestore compatibility
    val marksObtained: Double?=0.0, // Use Double? to allow null/not applicable
    val resultRemarks: String?=null, // Remarks specific to the result

    // From Exam (details needed for display)
    val examName: String="",
    val examDate: String="",
    val examMaxMarks: Double=0.0, // Converted to Double for consistency with marksObtained
    val examDescription: String?=null,

    // From Subject (details needed for display)
    val subjectId: String="", // Changed to 'String' for Firestore compatibility
    val subjectName: String=""
) : Serializable