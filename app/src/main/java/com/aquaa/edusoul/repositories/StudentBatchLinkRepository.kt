// File: src/main/java/com/aquaa/edusoul/repositories/StudentBatchLinkRepository.kt
package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.StudentBatchLink
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class StudentBatchLinkRepository {

    private val db = FirebaseFirestore.getInstance()
    private val linksCollection = db.collection("student_enrollments")
    private val TAG = "StudentBatchLinkRepo"

    /**
     * Inserts a new student-batch link into Firestore.
     * Uses a composite ID to ensure a student can only be enrolled in a batch once.
     * @param link The StudentBatchLink object to insert.
     * @return The Firestore document ID of the newly created link, or an empty string if insertion fails.
     */
    suspend fun insertStudentBatchLink(link: StudentBatchLink): String {
        return try {
            // Use a composite ID to ensure a student can only be enrolled in a batch once
            val documentId = "${link.studentId}_${link.batchId}"
            link.id = documentId // Corrected: Update the ID of the passed object
            linksCollection.document(documentId).set(link).await()
            Log.d(TAG, "Student batch link inserted with ID: $documentId")
            documentId
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting student batch link: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Deletes a student-batch link from Firestore by student ID and batch ID.
     * @param studentId The ID of the student.
     * @param batchId The ID of the batch.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteStudentBatchLink(studentId: String, batchId: String): Int {
        return try {
            val documentId = "${studentId}_${batchId}"
            linksCollection.document(documentId).delete().await()
            Log.d(TAG, "Student batch link deleted for student '$studentId' and batch '$batchId'.")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting student batch link: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Retrieves student-batch links for a specific student from Firestore.
     * Emits a list of StudentBatchLink objects as a Flow.
     * @param studentId The ID of the student.
     */
    fun getStudentBatchLinksForStudent(studentId: String): Flow<List<StudentBatchLink>> = callbackFlow {
        val subscription = linksCollection.whereEqualTo("studentId", studentId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for student batch links for student '$studentId' failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val links = snapshot.toObjects(StudentBatchLink::class.java)
                    trySend(links).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to StudentBatchLink list for student '$studentId': ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves student-batch links for a specific batch from Firestore.
     * Emits a list of StudentBatchLink objects as a Flow.
     * @param batchId The ID of the batch.
     */
    fun getStudentBatchLinksForBatch(batchId: String): Flow<List<StudentBatchLink>> = callbackFlow {
        val subscription = linksCollection.whereEqualTo("batchId", batchId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for student batch links for batch '$batchId' failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val links = snapshot.toObjects(StudentBatchLink::class.java)
                    trySend(links).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to StudentBatchLink list for batch '$batchId': ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves all student-batch links from Firestore.
     * Emits a list of StudentBatchLink objects as a Flow.
     */
    fun getAllStudentBatchLinks(): Flow<List<StudentBatchLink>> = callbackFlow {
        val subscription = linksCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all student batch links failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val links = snapshot.toObjects(StudentBatchLink::class.java)
                    trySend(links).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to StudentBatchLink list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Counts the number of student-batch links for a given student-batch pair.
     * @param studentId The ID of the student.
     * @param batchId The ID of the batch.
     * @return The count (1 if exists, 0 if not).
     */
    suspend fun countStudentBatchLinks(studentId: String, batchId: String): Int {
        return try {
            val documentId = "${studentId}_${batchId}"
            val snapshot = linksCollection.document(documentId).get().await()
            if (snapshot.exists()) 1 else 0
        } catch (e: Exception) {
            Log.e(TAG, "Error counting student batch links for student '$studentId' and batch '$batchId': ${e.message}", e)
            0 // Return 0 on error
        }
    }

    /**
     * Retrieves a specific student-batch link by student ID and batch ID.
     * @param studentId The ID of the student.
     * @param batchId The ID of the batch.
     * @return The StudentBatchLink object if found, null otherwise.
     */
    suspend fun getStudentBatchLink(studentId: String, batchId: String): StudentBatchLink? {
        return try {
            val documentId = "${studentId}_${batchId}"
            val document = linksCollection.document(documentId).get().await()
            document.toObject(StudentBatchLink::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student batch link for student '$studentId' and batch '$batchId': ${e.message}", e)
            null
        }
    }
}