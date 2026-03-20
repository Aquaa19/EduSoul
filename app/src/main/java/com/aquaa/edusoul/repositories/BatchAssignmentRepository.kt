// File: src/main/java/com/aquaa/edusoul/repositories/BatchAssignmentRepository.kt
package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.BatchAssignment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class BatchAssignmentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val assignmentsCollection = db.collection("batch_assignments")
    private val TAG = "BatchAssignmentRepo"

    /**
     * Inserts a new batch assignment into Firestore.
     * Uses a composite ID to ensure a homework is assigned to a batch only once.
     * @param batchAssignment The BatchAssignment object to insert.
     * @return The Firestore document ID of the newly created assignment, or an empty string if insertion fails.
     */
    suspend fun insertBatchAssignment(batchAssignment: BatchAssignment): String {
        return try {
            // Use a composite ID to ensure a homework is assigned to a batch only once
            val documentId = "${batchAssignment.homeworkId}_${batchAssignment.batchId}"
            batchAssignment.id = documentId // Corrected: Update the ID of the passed object
            assignmentsCollection.document(documentId).set(batchAssignment).await()
            Log.d(TAG, "Batch assignment inserted with ID: $documentId")
            documentId
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting batch assignment: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Retrieves batch assignments for a specific homework from Firestore.
     * Emits a list of BatchAssignment objects as a Flow.
     * @param homeworkId The ID of the homework.
     */
    fun getBatchAssignmentsByHomework(homeworkId: String): Flow<List<BatchAssignment>> = callbackFlow {
        val subscription = assignmentsCollection.whereEqualTo("homeworkId", homeworkId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for batch assignments by homework ID '$homeworkId' failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val assignments = snapshot.toObjects(BatchAssignment::class.java)
                    trySend(assignments).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to BatchAssignment list for homework ID '$homeworkId': ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves batch assignments for a specific batch from Firestore.
     * Emits a list of BatchAssignment objects as a Flow.
     * @param batchId The ID of the batch.
     */
    fun getBatchAssignmentsByBatch(batchId: String): Flow<List<BatchAssignment>> = callbackFlow {
        val subscription = assignmentsCollection.whereEqualTo("batchId", batchId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for batch assignments by batch ID '$batchId' failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val assignments = snapshot.toObjects(BatchAssignment::class.java)
                    trySend(assignments).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to BatchAssignment list for batch ID '$batchId': ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves all batch assignments from Firestore.
     * Emits a list of BatchAssignment objects as a Flow.
     */
    fun getAllBatchAssignments(): Flow<List<BatchAssignment>> = callbackFlow {
        val subscription = assignmentsCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all batch assignments failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val assignments = snapshot.toObjects(BatchAssignment::class.java)
                    trySend(assignments).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to BatchAssignment list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves a specific batch assignment from Firestore by homework ID and batch ID.
     * @param homeworkId The ID of the homework.
     * @param batchId The ID of the batch.
     * @return The BatchAssignment object if found, null otherwise.
     */
    suspend fun getBatchAssignment(homeworkId: String, batchId: String): BatchAssignment? {
        return try {
            val documentId = "${homeworkId}_${batchId}"
            val document = assignmentsCollection.document(documentId).get().await()
            document.toObject(BatchAssignment::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting batch assignment for homework '$homeworkId' and batch '$batchId': ${e.message}", e)
            null
        }
    }

    /**
     * Deletes a specific batch assignment from Firestore by its document ID.
     * @param batchAssignmentId The Firestore document ID of the batch assignment to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteBatchAssignmentById(batchAssignmentId: String): Int {
        return try {
            assignmentsCollection.document(batchAssignmentId).delete().await()
            Log.d(TAG, "Batch assignment deleted with ID: $batchAssignmentId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting batch assignment by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Updates an existing batch assignment in Firestore.
     * This method assumes the `batchAssignment` object's `id` field holds the Firestore document ID.
     * @param batchAssignment The BatchAssignment object to update.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateBatchAssignment(batchAssignment: BatchAssignment): Int {
        return try {
            if (batchAssignment.id.isNotBlank()) {
                assignmentsCollection.document(batchAssignment.id).set(batchAssignment).await()
                Log.d(TAG, "Batch assignment updated with ID: ${batchAssignment.id}")
                1 // Indicate success
            } else {
                Log.e(TAG, "Cannot update batch assignment: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating batch assignment: ${e.message}", e)
            0 // Indicate failure
        }
    }
}