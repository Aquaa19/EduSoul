// File: main/java/com/aquaa/edusoul/repositories/MessageRepository.kt
package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.combine // Import combine

class MessageRepository {

    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = db.collection("messages")
    private val TAG = "MessageRepository"

    /**
     * Inserts a new message into Firestore.
     * If the message has an ID, it attempts to set it. If not, Firestore generates one.
     * @param message The Message object to insert.
     * @return The ID of the inserted message, or an empty string on failure.
     */
    suspend fun insertMessage(message: Message): String {
        return try {
            val docRef = if (message.id.isNotBlank()) messagesCollection.document(message.id) else messagesCollection.document()
            message.id = docRef.id // Ensure the message object has the Firestore ID
            docRef.set(message).await()
            Log.d(TAG, "Message inserted with ID: ${message.id}")
            message.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting message: ${e.message}", e)
            "" // Return empty string on failure
        }
    }

    /**
     * Retrieves all messages from Firestore in real-time.
     * @return A Flow emitting a list of all Message objects whenever the data changes.
     */
    fun getAllMessages(): Flow<List<Message>> = callbackFlow {
        val subscription = messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all messages failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val messages = snapshot.toObjects(Message::class.java)
                    trySend(messages).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Message list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves messages for a specific conversation between two users in real-time.
     * Messages are ordered by timestamp.
     * @param user1Id The ID of the first user in the conversation.
     * @param user2Id The ID of the second user in the conversation.
     * @return A Flow emitting a list of Message objects in the conversation whenever the data changes.
     */
    fun getMessagesForConversation(user1Id: String, user2Id: String): Flow<List<Message>> = combine(
        callbackFlow {
            // Listener for messages from user1 to user2
            val listenerRegistration = messagesCollection
                .whereEqualTo("senderId", user1Id)
                .whereEqualTo("receiverId", user2Id)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen for messages (user1 to user2) failed: ${e.message}", e)
                        close(e)
                        return@addSnapshotListener
                    }
                    // FIX: Explicitly specify type for emptyList()
                    trySend(snapshot?.toObjects(Message::class.java) ?: emptyList<Message>()).isSuccess
                }
            awaitClose { listenerRegistration.remove() }
        },
        callbackFlow {
            // Listener for messages from user2 to user1
            val listenerRegistration = messagesCollection
                .whereEqualTo("senderId", user2Id)
                .whereEqualTo("receiverId", user1Id)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen for messages (user2 to user1) failed: ${e.message}", e)
                        close(e)
                        return@addSnapshotListener
                    }
                    // FIX: Explicitly specify type for emptyList()
                    trySend(snapshot?.toObjects(Message::class.java) ?: emptyList<Message>()).isSuccess
                }
            awaitClose { listenerRegistration.remove() }
        }
    ) { messages1, messages2 ->
        // Combine and sort the messages from both directions
        (messages1 + messages2).sortedBy { it.timestamp }
    }

    /**
     * Retrieves a single message by its ID.
     * @param messageId The ID of the message.
     * @return The Message object, or null if not found.
     */
    suspend fun getMessageById(messageId: String): Message? {
        return try {
            messagesCollection.document(messageId).get().await().toObject(Message::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting message by ID '$messageId': ${e.message}", e)
            null
        }
    }

    /**
     * Deletes a message by its ID from Firestore.
     * @param messageId The ID of the message to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteMessage(messageId: String): Int {
        return try {
            messagesCollection.document(messageId).delete().await()
            Log.d(TAG, "Message deleted with ID: $messageId")
            1 // Return 1 for success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message with ID '$messageId': ${e.message}", e)
            0 // Return 0 for failure
        }
    }

    /**
     * Deletes all messages from Firestore.
     * @return The number of documents affected.
     */
    suspend fun deleteAllMessages(): Int {
        var deletedCount = 0
        return try {
            val snapshot = messagesCollection.get().await()
            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
                deletedCount++
            }
            batch.commit().await()
            Log.d(TAG, "All messages deleted. Count: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all messages: ${e.message}", e)
            0
        }
    }

    /**
     * Marks messages as read in a conversation.
     * Updates the status of messages where the receiver is currentUserId and sender is otherUserId to STATUS_READ.
     * @param currentUserId The ID of the user who is receiving and reading the messages.
     * @param otherUserId The ID of the user who sent the messages.
     */
    suspend fun markMessagesAsRead(currentUserId: String, otherUserId: String) {
        try {
            val querySnapshot = messagesCollection
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", otherUserId)
                .whereEqualTo("status", Message.STATUS_SENT) // Only mark 'sent' messages as 'read'
                .get()
                .await()

            val batch = db.batch()
            for (document in querySnapshot.documents) {
                batch.update(document.reference, "status", Message.STATUS_READ)
            }
            batch.commit().await()
            Log.d(TAG, "Marked ${querySnapshot.size()} messages as read for conversation $currentUserId - $otherUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}", e)
        }
    }
}