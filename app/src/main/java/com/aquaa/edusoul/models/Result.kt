package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class for Result.
 * Represents a student's score for a specific exam.
 */
data class Result(
    @DocumentId
    var id: String = "",
    val studentId: String="",
    val examId: String="",
    val marksObtained: Double=0.0,
    val remarks: String?=null,
    val enteredByUserId: String?=null,
    val entryTimestamp: String?=null
)