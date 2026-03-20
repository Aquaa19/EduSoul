package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Homework
import com.aquaa.edusoul.models.StudentAssignment // Added import for StudentAssignment model
import com.aquaa.edusoul.models.BatchAssignment // Added import for BatchAssignment model
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.catch

class HomeworkRepository {

    private val db = FirebaseFirestore.getInstance()
    private val homeworkCollection = db.collection("homework")
    private val TAG = "HomeworkRepo"

    suspend fun insertHomework(homework: Homework): String {
        return try {
            val documentReference = homeworkCollection.add(homework).await()
            Log.e(TAG, "Homework inserted with ID: ${documentReference.id}")
            homework.id = documentReference.id
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting homework: ${e.message}", e)
            ""
        }
    }

    suspend fun updateHomework(homework: Homework): Int {
        return try {
            if (homework.id.isNotBlank()) {
                homeworkCollection.document(homework.id).set(homework).await()
                Log.e(TAG, "Homework updated with ID: ${homework.id}")
                1
            } else {
                Log.e(TAG, "Cannot update homework: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating homework: ${e.message}", e)
            0
        }
    }

    /**
     * Deletes a homework from Firestore by its document ID,
     * and also deletes all associated student assignments and batch assignments for this homework.
     * @param homeworkId The Firestore document ID of the homework to delete.
     * @return The number of documents affected (1 for homework + N for assignments for success, 0 for failure).
     */
    suspend fun deleteHomeworkById(homeworkId: String): Int {
        return try {
            val firestoreBatch = db.batch()
            var deletedCount = 0

            // 1. Delete the homework document itself
            val homeworkDocRef = homeworkCollection.document(homeworkId)
            firestoreBatch.delete(homeworkDocRef)
            deletedCount++
            Log.e(TAG, "Scheduled deletion of homework with ID: $homeworkId")

            // 2. Find and delete all associated StudentAssignment documents
            val studentAssignmentsSnapshot = db.collection("student_assignments") // Assuming "student_assignments" is the collection name
                .whereEqualTo("homeworkId", homeworkId)
                .get()
                .await()
            for (document in studentAssignmentsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.e(TAG, "Scheduled deletion of student assignment with ID: ${document.id} for homework: $homeworkId")
            }

            // 3. Find and delete all associated BatchAssignment documents
            val batchAssignmentsSnapshot = db.collection("batch_assignments") // Assuming "batch_assignments" is the collection name
                .whereEqualTo("homeworkId", homeworkId)
                .get()
                .await()
            for (document in batchAssignmentsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.e(TAG, "Scheduled deletion of batch assignment with ID: ${document.id} for homework: $homeworkId")
            }

            // Commit the batch write
            firestoreBatch.commit().await()
            Log.e(TAG, "Homework and ${deletedCount - 1} associated assignments deleted successfully for ID: $homeworkId. Total documents deleted: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting homework by ID $homeworkId and its associated data: ${e.message}", e)
            0
        }
    }

    fun getAllHomework(): Flow<List<Homework>> = callbackFlow {
        val subscription = homeworkCollection.orderBy("dueDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for all homeworks failed: ${e.message}", e)
                    trySend(emptyList()).isSuccess
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val homeworks = snapshot.toObjects(Homework::class.java)
                    val homeworksWithIds = homeworks.map { homework ->
                        val docId = snapshot.documents.firstOrNull { it.toObject(Homework::class.java)?.id == homework.id }?.id ?: homework.id
                        homework.copy(id = docId)
                    }
                    Log.e(TAG, "Successfully fetched/updated all homeworks. Count: ${homeworksWithIds.size}")
                    trySend(homeworksWithIds).isSuccess
                } else {
                    Log.e(TAG, "No homeworks found or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting all homework: ${e.message}", e)
        emit(emptyList())
    }

    fun getHomeworksByTeacher(teacherId: String): Flow<List<Homework>> = callbackFlow {
        Log.e(TAG, "Querying homeworks for teacherId: $teacherId")
        try {
            val subscription = homeworkCollection
                .whereEqualTo("teacherId", teacherId)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen for homeworks by teacher ID '$teacherId' failed: ${e.message}", e)
                        trySend(emptyList()).isSuccess
                        close(e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val homeworks = snapshot.toObjects(Homework::class.java)
                        val homeworksWithIds = homeworks.map { homework ->
                            val docId = snapshot.documents.firstOrNull { it.toObject(Homework::class.java)?.id == homework.id }?.id ?: homework.id
                            homework.copy(id = docId)
                        }
                        Log.e(TAG, "Fetched ${homeworksWithIds.size} homeworks for teacher ID '$teacherId'.")
                        trySend(homeworksWithIds).isSuccess
                    } else {
                        Log.e(TAG, "No homeworks found for teacher ID '$teacherId' or snapshot is empty.")
                        trySend(emptyList()).isSuccess
                    }
                }
            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up homeworks by teacher listener: ${e.message}", e)
            close(e)
        }
    }

    suspend fun getHomeworkById(homeworkId: String): Homework? {
        return try {
            val document = homeworkCollection.document(homeworkId).get().await()
            if (document.exists()) {
                Log.e(TAG, "Successfully fetched homework by ID '$homeworkId'.")
                document.toObject(Homework::class.java)?.apply {
                    this.id = document.id
                }
            } else {
                Log.e(TAG, "Homework with ID '$homeworkId' not found.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting homework by ID '$homeworkId': ${e.message}", e)
            null
        }
    }

    fun getHomeworksByBatch(batchId: String): Flow<List<Homework>> = callbackFlow {
        Log.e(TAG, "Querying homeworks for batchId: $batchId")
        try {
            val subscription = homeworkCollection
                .whereArrayContains("assignedBatches", batchId)
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen for homeworks by batch ID '$batchId' failed: ${e.message}", e)
                        trySend(emptyList()).isSuccess
                        close(e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val homeworks = snapshot.toObjects(Homework::class.java)
                        val homeworksWithIds = homeworks.map { homework ->
                            val docId = snapshot.documents.firstOrNull { it.toObject(Homework::class.java)?.id == homework.id }?.id ?: homework.id
                            homework.copy(id = docId)
                        }
                        Log.e(TAG, "Fetched ${homeworksWithIds.size} homeworks for batch ID '$batchId'.")
                        trySend(homeworksWithIds).isSuccess
                    } else {
                        Log.e(TAG, "No homeworks found for batch ID '$batchId' or snapshot is empty.")
                        trySend(emptyList()).isSuccess
                    }
                }
            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up homeworks by batch listener: ${e.message}", e)
            close(e)
        }
    }
}