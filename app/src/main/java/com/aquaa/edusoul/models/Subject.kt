package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a Subject taught at the center.
 */
data class Subject(
    @DocumentId
    var id: String = "",
    var subjectName: String="",
    var subjectCode: String?=null,
    var description: String?=null
) {
    override fun toString(): String {
        return subjectName + (if (!subjectCode.isNullOrBlank()) " ($subjectCode)" else "")
    }
}