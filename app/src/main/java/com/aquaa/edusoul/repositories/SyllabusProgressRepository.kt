package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.SyllabusProgress
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.catch // Import the catch operator

class SyllabusProgressRepository {

    private val db = FirebaseFirestore.getInstance()
    private val progressCollection = db.collection("syllabus_progress")
    private val TAG = "SyllabusProgressRepo"

    /**
     * Inserts or updates a syllabus progress record in Firestore.
     * Uses a composite ID (topicId_batchId) to ensure uniqueness.
     * @param progress The SyllabusProgress object to insert/update.
     * @return The Firestore document ID of the upserted record, or an empty string if operation fails.
     */
    suspend fun insertSyllabusProgress(progress: SyllabusProgress): String {
        return try {
            // For upserting, we use a composite ID to ensure uniqueness.
            val documentId = "${progress.syllabusTopicId}_${progress.batchId}"
            progress.id = documentId // Ensure the ID in the object matches the document ID
            progressCollection.document(documentId).set(progress).await()
            Log.d(TAG, "Syllabus progress upserted with ID: $documentId")
            documentId
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting syllabus progress: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Retrieves a specific syllabus progress record from Firestore by topic ID and batch ID.
     * @param topicId The ID of the syllabus topic.
     * @param batchId The ID of the batch.
     * @return The SyllabusProgress object if found, null otherwise.
     */
    suspend fun getSyllabusProgress(topicId: String, batchId: String): SyllabusProgress? {
        return try {
            val documentId = "${topicId}_${batchId}"
            val document = progressCollection.document(documentId).get().await()
            document.toObject(SyllabusProgress::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting syllabus progress for topic '$topicId' and batch '$batchId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves syllabus progress records for a specific batch from Firestore in real-time,
     * ordered by completion date descending.
     * Emits a list of SyllabusProgress objects as a Flow.
     * @param batchId The ID of the batch.
     */
    fun getSyllabusProgressForBatch(batchId: String): Flow<List<SyllabusProgress>> = callbackFlow {
        val subscription = progressCollection.whereEqualTo("batchId", batchId)
            .orderBy("completionDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for syllabus progress for batch '$batchId' failed: ${e.message}", e)
                    // No need for trySend(emptyList()).isSuccess here, as .catch will handle it
                    close(e) // Close the flow with the exception
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val progresses = snapshot.toObjects(SyllabusProgress::class.java)
                    trySend(progresses).isSuccess
                } else {
                    trySend(emptyList()).isSuccess // Emit empty list if snapshot is null
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e -> // Use .catch operator for exception transparency
        Log.e(TAG, "Error getting syllabus progress for batch '$batchId': ${e.message}", e)
        emit(emptyList()) // Emit empty list on error
    }

    /**
     * Retrieves all syllabus progress records from Firestore in real-time, ordered by completion date descending.
     * Emits a list of SyllabusProgress objects as a Flow.
     */
    fun getAllSyllabusProgress(): Flow<List<SyllabusProgress>> = callbackFlow {
        val subscription = progressCollection.orderBy("completionDate", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all syllabus progress failed: ${e.message}", e)
                // No need for trySend(emptyList()).isSuccess here, as .catch will handle it
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val progresses = snapshot.toObjects(SyllabusProgress::class.java)
                trySend(progresses).isSuccess
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }.catch { e -> // Use .catch operator for exception transparency
        Log.e(TAG, "Error getting all syllabus progress: ${e.message}", e)
        emit(emptyList()) // Emit empty list on error
    }

    /**
     * Retrieves syllabus progress records for a specific batch and a list of topics from Firestore.
     * @param batchId The ID of the batch.
     * @param topicIds A list of syllabus topic IDs.
     * @return A list of matching SyllabusProgress objects.
     */
    suspend fun getSyllabusProgressForBatchAndTopics(batchId: String, topicIds: List<String>): List<SyllabusProgress> {
        return try {
            // Firestore `whereIn` has a limit of 10. If more, multiple queries are needed.
            // For simplicity, assuming less than 10 for now.
            if (topicIds.isEmpty()) return emptyList()

            val snapshot = progressCollection
                .whereEqualTo("batchId", batchId)
                .whereIn("syllabusTopicId", topicIds)
                .get()
                .await()
            snapshot.toObjects(SyllabusProgress::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting syllabus progress for batch '$batchId' and topics '$topicIds': ${e.message}", e)
            emptyList() // Return empty list on error
        }
    }

    /**
     * Deletes a syllabus progress record from Firestore by its document ID.
     * @param progressId The Firestore document ID of the progress record to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteSyllabusProgressById(progressId: String): Int {
        return try {
            progressCollection.document(progressId).delete().await()
            Log.d(TAG, "Syllabus progress deleted with ID: $progressId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting syllabus progress by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }
}
