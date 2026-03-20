package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Represents a recurring weekly class session template for a specific batch.
 * This entity stores the blueprint for a class that occurs at the same time
 * on the same day every week.
 */
data class RecurringClassSession(
    @DocumentId
    var id: String = "",
    val batchId: String="",
    val subjectId: String="",
    val teacherUserId: String="",
    // Day of the week (e.g., 1 for Monday, 7 for Sunday, following java.util.Calendar constants)
    val dayOfWeek: Int=0,
    val startTime: String="", // "HH:MM" (24-hour)
    val endTime: String="" // "HH:MM" (24-hour)
)