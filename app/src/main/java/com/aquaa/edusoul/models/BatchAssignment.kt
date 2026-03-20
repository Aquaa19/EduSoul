package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

data class BatchAssignment(
    @DocumentId
    var id: String = "",
    val homeworkId: String = "", // Added default value
    val batchId: String = "" // Added default value
)