// File: EduSoul/app/src/main/java/com/aquaa/edusoul/models/InstituteAttendance.kt
package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

data class InstituteAttendance(
    @DocumentId
    var id: String = "",
    var studentId: String = "",
    var markedAt: Long = 0L,
    var status: String = "",
    var markedByUserId: String = ""
)