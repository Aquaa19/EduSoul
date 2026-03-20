// File: src/main/java/com/aquaa/edusoul/models/ParentHomeworkDetails.kt
package com.aquaa.edusoul.models

import java.io.Serializable

data class ParentHomeworkDetails(
    // From StudentAssignment
    var studentAssignmentId: String = "",
    val homeworkId: String = "",
    val studentId: String = "",
    val assignedDate: String? = null,
    val dueDate: String? = null,
    val status: String = "",
    val submissionDate: String? = null,
    val marksObtained: Double? = null,
    val remarks: String? = null,
    val submissionPath: String? = null,
    val submissionMimeType: String? = null, // ADDED in previous turns

    // From Homework
    val homeworkTitle: String = "",
    val homeworkDescription: String? = null,
    val homeworkMaxMarks: Double = 0.0,
    val homeworkType: String = "",
    val homeworkAttachmentPath: String? = null,
    val homeworkAttachmentMimeType: String? = null,

    // Joined data for display
    val studentName: String = "",
    val subjectName: String = ""
) : Serializable