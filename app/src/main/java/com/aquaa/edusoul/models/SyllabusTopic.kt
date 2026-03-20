package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class for SyllabusTopic.
 * Represents a single topic or chapter within a subject.
 */
data class SyllabusTopic(
    @DocumentId
    var id: String = "",
    val topicName: String="",
    val subjectId: String="",
    val description: String?=null,
    val order: Int?=null // To maintain the order of topics, if applicable
)