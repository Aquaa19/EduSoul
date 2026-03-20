package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents an Attendance record for a Student in a ClassSession.
 */
data class Attendance(
    @DocumentId
    var id: String = "",
    val studentId: String="",
    val classSessionId: String="",
    val attendanceStatus: String="", // e.g., "Present", "Absent", "Late"
    val remarks: String?=null,
    val markedAt: String?=null
) {
    override fun toString(): String {
        return "Attendance for Student " + studentId + " in Session " + classSessionId + ": " + attendanceStatus
    }

    companion object {
        const val STATUS_PRESENT = "Present"
        const val STATUS_ABSENT = "Absent"
        const val STATUS_LATE = "Late"
    }
}