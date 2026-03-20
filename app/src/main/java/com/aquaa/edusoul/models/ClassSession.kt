package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a scheduled Class Session.
 * Added 'isAttendanceMarked' property.
 */
data class ClassSession(
    @DocumentId
    var id: String = "",
    val batchId: String = "", // Added default value
    val subjectId: String = "", // Added default value
    val teacherUserId: String = "", // Added default value
    val sessionDate: String = "", // Added default value
    val startTime: String = "", // Added default value
    val endTime: String = "", // Added default value
    val topicCovered: String? = null,
    val status: String = "Scheduled", // Added default value
    var isAttendanceMarked: Boolean = false
) {
    override fun toString(): String {
        return "ClassSession on $sessionDate from $startTime to $endTime (Status: $status, Attendance Marked: $isAttendanceMarked)"
    }
}