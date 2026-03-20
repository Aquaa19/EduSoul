// File: src/main/java/com/aquaa/edusoul/repositories/TeacherSubjectBatchLinkRepository.kt
package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.TeacherSubjectBatchLink
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class TeacherSubjectBatchLinkRepository {

    private val db = FirebaseFirestore.getInstance()
    private val teacherSubjectBatchLinksCollection = db.collection("teacher_assignments")
    private val TAG = "TeacherAssignmentLinkRepo"

    suspend fun insertTeacherSubjectBatchLink(link: TeacherSubjectBatchLink): String {
        return try {
            val documentId = "${link.teacherUserId}_${link.subjectId}_${link.batchId}"
            link.id = documentId // Update the ID of the passed object
            val linkWithId = link.copy(id = documentId)
            teacherSubjectBatchLinksCollection.document(documentId).set(linkWithId).await()
            Log.e(TAG, "TeacherSubjectBatchLink inserted with ID: $documentId")
            documentId
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting TeacherSubjectBatchLink: ${e.message}", e)
            ""
        }
    }

    suspend fun updateTeacherSubjectBatchLink(link: TeacherSubjectBatchLink): Int {
        return try {
            if (link.id.isNotBlank()) {
                teacherSubjectBatchLinksCollection.document(link.id).set(link).await()
                Log.e(TAG, "TeacherSubjectBatchLink updated with ID: ${link.id}")
                1
            } else {
                Log.e(TAG, "Cannot update TeacherSubjectBatchLink: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating TeacherSubjectBatchLink: ${e.message}", e)
            0
        }
    }

    suspend fun deleteTeacherSubjectBatchLink(linkId: String): Int {
        return try {
            teacherSubjectBatchLinksCollection.document(linkId).delete().await()
            Log.e(TAG, "TeacherSubjectBatchLink deleted with ID: $linkId")
            1
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting TeacherSubjectBatchLink by ID: ${e.message}", e)
            0
        }
    }

    suspend fun getTeacherSubjectBatchLinkById(linkId: String): TeacherSubjectBatchLink? {
        return try {
            val document = teacherSubjectBatchLinksCollection.document(linkId).get().await()
            document.toObject(TeacherSubjectBatchLink::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting TeacherSubjectBatchLink by ID '$linkId': ${e.message}", e)
            null
        }
    }

    fun getAllTeacherSubjectBatchLinks(): Flow<List<TeacherSubjectBatchLink>> = callbackFlow {
        val subscription = teacherSubjectBatchLinksCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all TeacherSubjectBatchLinks failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val links = snapshot.toObjects(TeacherSubjectBatchLink::class.java)
                    Log.e(TAG, "Fetched ${links.size} all TeacherSubjectBatchLinks.")
                    trySend(links).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to TeacherSubjectBatchLink list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    fun getTeacherSubjectBatchLinksForTeacher(teacherId: String): Flow<List<TeacherSubjectBatchLink>> = callbackFlow {
        Log.e(TAG, "Querying TeacherSubjectBatchLink for teacherId: $teacherId")
        val subscription = teacherSubjectBatchLinksCollection.whereEqualTo("teacherUserId", teacherId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for TeacherSubjectBatchLink for teacherId '$teacherId' failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val links = snapshot.toObjects(TeacherSubjectBatchLink::class.java)
                    Log.e(TAG, "Fetched ${links.size} TeacherSubjectBatchLink documents for teacherId: $teacherId")
                    trySend(links).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to TeacherSubjectBatchLink list for teacherId '$teacherId': ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun getExistingLink(teacherId: String, subjectId: String, batchId: String): TeacherSubjectBatchLink? {
        return try {
            val documentId = "${teacherId}_${subjectId}_${batchId}"
            val document = teacherSubjectBatchLinksCollection.document(documentId).get().await()
            if (document.exists()) {
                document.toObject(TeacherSubjectBatchLink::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking existing TeacherSubjectBatchLink: ${e.message}", e)
            null
        }
    }
}