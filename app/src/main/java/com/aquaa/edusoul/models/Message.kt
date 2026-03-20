package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class for Message.
 * Represents a private message between two users.
 */
data class Message(
    @DocumentId
    var id: String = "",
    val senderId: String="",
    val receiverId: String="",
    val messageContent: String="",
    val timestamp: String="", // "YYYY-MM-DD HH:MM:SS" format
    val status: String="" // e.g., "SENT", "DELIVERED", "READ"
) {
    companion object {
        const val STATUS_SENT = "SENT"
        const val STATUS_DELIVERED = "DELIVERED"
        const val STATUS_READ = "READ"
    }
}