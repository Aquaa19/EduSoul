package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a linking entity for Teacher-Subject-Batch assignments.
 */
data class TeacherSubjectBatchLink(
    @DocumentId
    var id: String = "",
    val teacherUserId: String="",
    val subjectId: String="",
    val batchId: String=""
)