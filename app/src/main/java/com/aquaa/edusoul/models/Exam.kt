package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class for Exam.
 * Represents an exam entity with its details.
 */
data class Exam(
    @DocumentId
    var id: String = "",
    val examName: String="",
    val subjectId: String?=null,
    val batchId: String?=null,
    val examDate: String="",
    val maxMarks: Int=0,
    val description: String?=null,
    var teacherId: String? = null
) {
    override fun toString(): String {
        return "$examName ($examDate)"
    }
}