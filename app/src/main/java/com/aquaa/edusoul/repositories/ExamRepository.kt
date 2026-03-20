package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.Homework
import com.aquaa.edusoul.models.Result // Added import for Result model
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class ExamRepository {

    private val db = FirebaseFirestore.getInstance()
    private val examsCollection = db.collection("exams")
    private val TAG = "ExamRepository"

    /**
     * Inserts a new exam into Firestore.
     * @param exam The Exam object to insert.
     * @return The Firestore document ID of the newly created exam, or an empty string if insertion fails.
     */
    suspend fun insertExam(exam: Exam): String {
        return try {
            val documentReference = examsCollection.add(exam).await()
            Log.d(TAG, "Exam inserted with ID: ${documentReference.id}")
            exam.id = documentReference.id
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting exam: ${e.message}", e)
            ""
        }
    }

    /**
     * Updates an existing exam in Firestore.
     * @param exam The Exam object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateExam(exam: Exam): Int {
        return try {
            if (exam.id.isNotBlank()) {
                examsCollection.document(exam.id).set(exam).await()
                Log.d(TAG, "Exam updated with ID: ${exam.id}")
                1
            } else {
                Log.w(TAG, "Cannot update exam: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating exam: ${e.message}", e)
            0
        }
    }

    /**
     * Deletes an exam from Firestore by its document ID,
     * and also deletes all associated result records for this exam.
     * @param examId The Firestore document ID of the exam to delete.
     * @return The number of documents affected (1 for exam + N for results for success, 0 for failure).
     */
    suspend fun deleteExamById(examId: String): Int {
        return try {
            val firestoreBatch = db.batch()
            var deletedCount = 0

            // 1. Delete the exam document itself
            val examDocRef = examsCollection.document(examId)
            firestoreBatch.delete(examDocRef)
            deletedCount++
            Log.d(TAG, "Scheduled deletion of exam with ID: $examId")

            // 2. Find and delete all associated Result documents
            val resultsSnapshot = db.collection("results") // Assuming "results" is the collection name
                .whereEqualTo("examId", examId)
                .get()
                .await()
            for (document in resultsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of result record with ID: ${document.id} for exam: $examId")
            }

            // Commit the batch write
            firestoreBatch.commit().await()
            Log.d(TAG, "Exam and ${deletedCount - 1} associated results deleted successfully for ID: $examId. Total documents deleted: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting exam by ID $examId and its associated data: ${e.message}", e)
            0
        }
    }

    /**
     * Retrieves all exams from Firestore in real-time.
     * Emits a list of Exam objects as a Flow.
     */
    fun getAllExams(): Flow<List<Exam>> = callbackFlow {
        val subscription = examsCollection.orderBy("examDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for all exams failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val exams = snapshot.toObjects(Exam::class.java)
                    val examsWithIds = exams.map { exam ->
                        val docId = snapshot.documents.firstOrNull { it.toObject(Exam::class.java)?.id == exam.id }?.id ?: exam.id
                        exam.copy(id = docId)
                    }
                    trySend(examsWithIds).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting all exams: ${e.message}", e)
        emit(emptyList())
    }

    /**
     * Retrieves exams created by a specific teacher from Firestore in real-time.
     * @param teacherId The ID of the teacher.
     * @return A Flow emitting a list of Exam objects.
     */
    fun getExamsByTeacher(teacherId: String): Flow<List<Exam>> = callbackFlow {
        val subscription = examsCollection
            .whereEqualTo("teacherId", teacherId)
            .orderBy("examDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for exams for teacher ID '$teacherId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val exams = snapshot.toObjects(Exam::class.java)
                    val examsWithIds = exams.map { exam ->
                        val docId = snapshot.documents.firstOrNull { it.toObject(Exam::class.java)?.id == exam.id }?.id ?: exam.id
                        exam.copy(id = docId)
                    }
                    trySend(examsWithIds).isSuccess
                } else {
                    Log.d(TAG, "No exams found for teacher ID '$teacherId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting exams by teacher: ${e.message}", e)
        emit(emptyList())
    }

    /**
     * Retrieves a specific exam from Firestore by its document ID.
     * @param examId The Firestore document ID of the exam.
     * @return The Exam object if found, null otherwise.
     */
    suspend fun getExamById(examId: String): Exam? {
        return try {
            val document = examsCollection.document(examId).get().await()
            if (document.exists()) {
                Log.d(TAG, "Successfully fetched exam by ID '$examId'.")
                document.toObject(Exam::class.java)?.apply {
                    this.id = document.id
                }
            } else {
                Log.w(TAG, "Exam with ID '$examId' not found.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting exam by ID '$examId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves homework assignments for a specific batch from Firestore in real-time.
     * @param batchId The ID of the batch.
     * @return A Flow emitting a list of Homework objects.
     */
    fun getHomeworksByBatch(batchId: String): Flow<List<Homework>> = callbackFlow {
        val subscription = db.collection("homework")
            .whereArrayContains("assignedBatches", batchId)
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for homeworks by batch ID '$batchId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val homeworks = snapshot.toObjects(Homework::class.java)
                    val homeworksWithIds = homeworks.map { homework ->
                        val docId = snapshot.documents.firstOrNull { it.toObject(Homework::class.java)?.id == homework.id }?.id ?: homework.id
                        homework.copy(id = docId)
                    }
                    trySend(homeworksWithIds).isSuccess
                } else {
                    Log.d(TAG, "No homeworks found for batch ID '$batchId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting homeworks by batch: ${e.message}", e)
        emit(emptyList())
    }
}