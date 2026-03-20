package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.RecurringClassSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class RecurringClassSessionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val recurringSessionsCollection = db.collection("recurring_class_sessions")
    private val TAG = "RecurringSessionRepo"

    /**
     * Inserts a new recurring class session template into Firestore.
     * @param recurringSession The RecurringClassSession object to insert.
     * @return The Firestore document ID of the newly created session, or an empty string if insertion fails.
     */
    suspend fun insert(recurringSession: RecurringClassSession): String {
        return try {
            val documentReference = recurringSessionsCollection.add(recurringSession).await()
            Log.d(TAG, "Recurring session inserted with ID: ${documentReference.id}")
            recurringSession.id = documentReference.id // Corrected: Update the ID of the passed object
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting recurring session: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Deletes a recurring class session template from Firestore by its document ID.
     * @param sessionId The Firestore document ID of the session to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteById(sessionId: String): Int {
        return try {
            recurringSessionsCollection.document(sessionId).delete().await()
            Log.d(TAG, "Recurring session deleted with ID: $sessionId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recurring session by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Retrieves recurring class session templates for a specific batch from Firestore in real-time,
     * ordered by day of week and start time.
     * Emits a list of RecurringClassSession objects as a Flow whenever the data changes.
     * @param batchId The ID of the batch.
     */
    fun getRecurringSessionsForBatch(batchId: String): Flow<List<RecurringClassSession>> = callbackFlow {
        val listenerRegistration = recurringSessionsCollection
            .whereEqualTo("batchId", batchId)
            .orderBy("dayOfWeek", Query.Direction.ASCENDING)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for recurring sessions for batch '$batchId'", e)
                    close(e) // Close the flow with an error
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        val sessions = snapshot.toObjects(RecurringClassSession::class.java)
                        trySend(sessions) // Send the updated list to the flow
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to RecurringClassSession list: ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    trySend(emptyList()) // Send an empty list if no documents or snapshot is null
                }
            }

        // The callbackFlow will remain open as long as there are collectors.
        // When the collector is cancelled (e.g., Activity stops observing),
        // awaitClose will be called, allowing us to remove the listener.
        awaitClose {
            listenerRegistration.remove()
            Log.d(TAG, "Firestore listener for batch '$batchId' recurring sessions removed.")
        }
    }

    /**
     * Retrieves recurring class session templates for a specific day of the week from Firestore.
     * @param dayOfWeek The day of the week (e.g., Calendar.MONDAY).
     * @return A list of matching RecurringClassSession objects.
     */
    suspend fun getRecurringSessionsForDay(dayOfWeek: Int): List<RecurringClassSession> {
        return try {
            val snapshot = recurringSessionsCollection.whereEqualTo("dayOfWeek", dayOfWeek).get().await()
            snapshot.toObjects(RecurringClassSession::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recurring sessions for day '$dayOfWeek': ${e.message}", e)
            emptyList() // Return empty list on error
        }
    }

    /**
     * Clears all recurring class session templates for a specific batch from Firestore.
     * @param batchId The ID of the batch.
     */
    suspend fun clearTimetableForBatch(batchId: String) {
        try {
            val snapshot = recurringSessionsCollection.whereEqualTo("batchId", batchId).get().await()
            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
            Log.d(TAG, "Cleared timetable for batch: $batchId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing timetable for batch '$batchId': ${e.message}", e)
            // Consider re-throwing or handling the error appropriately
        }
    }

    /**
     * Updates an existing recurring class session in Firestore.
     * This method assumes the `session` object's `id` field holds the Firestore document ID.
     * @param session The RecurringClassSession object to update.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateRecurringSession(session: RecurringClassSession): Int {
        return try {
            if (session.id.isNotBlank()) {
                recurringSessionsCollection.document(session.id).set(session).await()
                Log.d(TAG, "Recurring session updated with ID: ${session.id}")
                1 // Indicate success
            } else {
                Log.e(TAG, "Cannot update recurring session: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating recurring session: ${e.message}", e)
            0 // Indicate failure
        }
    }
}