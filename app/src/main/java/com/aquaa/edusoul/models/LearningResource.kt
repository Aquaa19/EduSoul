package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class for LearningResource.
 * Represents a learning material uploaded by a teacher.
 */
data class LearningResource(
    @DocumentId
    var id: String = "",
    val title: String="",
    val description: String?=null,
    val filePathOrUrl: String="", // Local path or URL to the resource file
    val fileMimeType: String="", // e.g., "application/pdf", "video/mp4"
    val subjectId: String?=null,
    val batchId: String?=null,
    val uploadedByTeacherId: String?=null,
    val uploadDate: String="" // "YYYY-MM-DD HH:MM:SS" format
)