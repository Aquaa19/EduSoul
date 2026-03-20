package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

data class StudentAssignment(
    @DocumentId
    var id: String = "",
    val homeworkId: String="",
    val studentId: String="",
    val assignedDate: String?=null,
    val dueDate: String?=null,
    val status: String="",
    val submissionDate: String?=null,
    val marksObtained: Double?=0.0,
    val remarks: String?=null,
    val submissionPath: String?=null,
    val submissionMimeType: String?=null
) {
    companion object {
        const val STATUS_ASSIGNED = "Assigned"
        const val STATUS_SUBMITTED = "Submitted"
        const val STATUS_GRADED = "Graded"
        const val STATUS_MISSED = "Missed"
    }
}