package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents an enrollment link between a Student and a Batch.
 */
data class StudentBatchLink(
    @DocumentId
    var id: String = "",
    val studentId: String="",
    val batchId: String="",
    val enrollmentDate: String="", // Date of enrollment (e.g., "YYYY-MM-DD")
    val status: String="" // Enrollment status (e.g., "Enrolled", "Active", "Inactive")
)