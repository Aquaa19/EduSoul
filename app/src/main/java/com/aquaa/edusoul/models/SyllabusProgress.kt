package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName // Import PropertyName

data class SyllabusProgress(
    @DocumentId
    var id: String = "",
    val syllabusTopicId: String="",
    val batchId: String="",
    @get:PropertyName("completed")
    @field:PropertyName("completed")
    val isCompleted: Boolean=false, // Mapped to 'completed' in Firestore
    val completionDate: String?=null,
    val updatedByTeacherId: String?=null,
    val remarks: String?=null
)