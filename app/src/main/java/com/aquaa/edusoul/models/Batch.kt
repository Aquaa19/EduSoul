package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a Batch or a class group.
 */
data class Batch(
    @DocumentId
    var id: String = "",
    var batchName: String="",
    var gradeLevel: String?=null,
    var academicYear: String?=null,
    var description: String?=null,
    var feeStructureId: String? = null
) {
    override fun toString(): String {
        return batchName + (if (!gradeLevel.isNullOrBlank()) " - $gradeLevel" else "")
    }
}