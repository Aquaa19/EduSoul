package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId // Added import for DocumentId

data class Homework(
    @DocumentId // Added this for Firestore document ID mapping
    var id: String = "",
    val title: String="",
    val description: String?=null,
    val dueDate: String="", // "YYYY-MM-DD"
    val subjectId: String="",
    val teacherId: String="",
    val attachmentPath: String?=null,
    var attachmentMimeType: String? = null,
    val maxMarks: Double = 0.0,
    val homeworkType: String = "Homework",
    val assignedBatches: List<String> = emptyList()
)