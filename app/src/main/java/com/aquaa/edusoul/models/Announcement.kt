package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class for Announcements.
 * Represents system-wide, batch-specific, or subject-specific announcements.
 */
data class Announcement(
    @DocumentId
    var id: String = "",
    val title: String="",
    val content: String="",
    val targetAudience: String="", // e.g., "ALL", "PARENTS", "TEACHERS", "BATCH", "SUBJECT"
    val batchId: String?=null, // Only for "BATCH" audience.
    val subjectId: String?=null, // Only for "SUBJECT" audience.
    var publishDate: String?=null, // "YYYY-MM-DD HH:MM:SS" format. When the announcement becomes active.
    val expiryDate: String?=null, // "YYYY-MM-DD HH:MM:SS" format. When the announcement expires.
    var authorUserId: String?=null, // User who created the announcement.
    val authorName: String? = null // For displaying author's name directly without extra lookup
) {
    // Companion object for constants (e.g., audience types)
    companion object {
        const val AUDIENCE_ALL = "ALL"
        const val AUDIENCE_PARENTS = "PARENTS"
        const val AUDIENCE_TEACHERS = "TEACHERS"
        const val AUDIENCE_BATCH = "BATCH"
        const val AUDIENCE_SUBJECT = "SUBJECT"
    }
}