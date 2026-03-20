package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.FeeStructure
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FeeStructureRepository {

    private val db = FirebaseFirestore.getInstance()
    private val feeStructuresCollection = db.collection("feeStructures")
    private val TAG = "FeeStructureRepository"

    /**
     * Inserts a new fee structure into Firestore.
     * @param feeStructure The FeeStructure object to insert.
     * @return The Firestore document ID of the newly created fee structure, or an empty string if insertion fails.
     */
    suspend fun insertFeeStructure(feeStructure: FeeStructure): String {
        return try {
            val documentReference = feeStructuresCollection.add(feeStructure).await()
            Log.d(TAG, "Fee structure inserted with ID: ${documentReference.id}")
            feeStructure.id = documentReference.id // Corrected: Update the ID of the passed object
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting fee structure: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Updates an existing fee structure in Firestore.
     * @param feeStructure The FeeStructure object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateFeeStructure(feeStructure: FeeStructure): Int {
        return try {
            if (feeStructure.id.isNotBlank()) {
                feeStructuresCollection.document(feeStructure.id).set(feeStructure).await()
                Log.d(TAG, "Fee structure updated with ID: ${feeStructure.id}")
                1 // Indicate success
            } else {
                Log.w(TAG, "Cannot update fee structure: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating fee structure: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Deletes a fee structure from Firestore by its document ID.
     * @param feeStructureId The Firestore document ID of the fee structure to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteFeeStructureById(feeStructureId: String): Int {
        return try {
            feeStructuresCollection.document(feeStructureId).delete().await()
            Log.d(TAG, "Fee structure deleted with ID: $feeStructureId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting fee structure by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Retrieves a specific fee structure from Firestore by its document ID.
     * @param feeStructureId The Firestore document ID of the fee structure.
     * @return The FeeStructure object if found, null otherwise.
     */
    suspend fun getFeeStructureById(feeStructureId: String): FeeStructure? {
        return try {
            val document = feeStructuresCollection.document(feeStructureId).get().await()
            if (document.exists()) {
                Log.d(TAG, "Successfully fetched fee structure by ID '$feeStructureId'.")
                document.toObject(FeeStructure::class.java)
            } else {
                Log.w(TAG, "Fee structure with ID '$feeStructureId' not found.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting fee structure by ID '$feeStructureId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves all fee structures from Firestore in real-time.
     * Emits a list of FeeStructure objects as a Flow whenever the data changes.
     */
    fun getAllFeeStructures(): Flow<List<FeeStructure>> = callbackFlow {
        val subscription = feeStructuresCollection
            .orderBy("title", Query.Direction.ASCENDING) // Order by title ascending
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for all fee structures failed: ${e.message}", e)
                    close(e) // Close the flow with the exception
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val feeStructures = snapshot.toObjects(FeeStructure::class.java)
                    Log.d(TAG, "Successfully fetched/updated all fee structures. Count: ${feeStructures.size}")
                    trySend(feeStructures).isSuccess // Emit the new list of fee structures
                } else {
                    Log.d(TAG, "No fee structures found or snapshot is empty.")
                    trySend(emptyList()).isSuccess // Emit an empty list if no data
                }
            }
        awaitClose { subscription.remove() } // Unregister the listener when the flow is cancelled
    }

    /**
     * Checks if a fee structure with the given title already exists.
     * This is used for adding new fee structures to prevent duplicates.
     * @param title The title to check.
     * @return True if a fee structure with the title exists, false otherwise.
     */
    suspend fun feeStructureTitleExists(title: String): Boolean {
        return try {
            val snapshot = feeStructuresCollection.whereEqualTo("title", title).get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if fee structure title exists '$title': ${e.message}", e)
            false // Assume it doesn't exist on error or handle as needed
        }
    }

    /**
     * Counts fee structures with a given title, excluding a specific ID.
     * This is used for updating fee structures to check for duplicate titles.
     * @param title The title to check.
     * @param excludeId The ID of the fee structure to exclude from the count (i.e., the one being edited).
     * @return The number of fee structures found with the given title, excluding the specified ID.
     */
    suspend fun countFeeStructuresWithTitle(title: String, excludeId: String): Int {
        return try {
            val snapshot = feeStructuresCollection
                .whereEqualTo("title", title)
                .get()
                .await()

            // Filter client-side to exclude the current item being edited
            val count = snapshot.documents.count { it.id != excludeId }
            Log.d(TAG, "Count of fee structures with title '$title' (excluding '$excludeId'): $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error counting fee structures with title '$title' (excluding '$excludeId'): ${e.message}", e)
            0 // Return 0 on error
        }
    }
}