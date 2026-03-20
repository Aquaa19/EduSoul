package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Homework // Added import for Homework model
import com.aquaa.edusoul.models.LearningResource // Added import for LearningResource model
import com.aquaa.edusoul.models.Announcement // Added import for Announcement model
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.FieldValue // Added import for FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class BatchRepository {

    private val db = FirebaseFirestore.getInstance()
    private val batchesCollection = db.collection("batches")
    private val TAG = "BatchRepository"

    // Repositories for associated data (injected via constructor or manually instantiated)
    // Assuming these are already instantiated and available in your Dagger/Hilt setup,
    // or you manually provide them here if not using DI.
    private val studentBatchLinkRepository = StudentBatchLinkRepository()
    private val classSessionRepository = ClassSessionRepository()
    private val recurringClassSessionRepository = RecurringClassSessionRepository() // Instantiated
    private val teacherSubjectBatchLinkRepository = TeacherSubjectBatchLinkRepository() // Instantiated
    private val syllabusProgressRepository = SyllabusProgressRepository() // Instantiated
    private val learningResourceRepository = LearningResourceRepository() // Instantiated
    private val announcementRepository = AnnouncementRepository() // Instantiated
    private val homeworkRepository = HomeworkRepository() // Instantiated


    /**
     * Inserts a new batch into Firestore.
     * @param batch The Batch object to insert.
     * @return The Firestore document ID of the newly created batch, or an empty string if insertion fails.
     */
    suspend fun insertBatch(batch: Batch): String {
        return try {
            val documentReference = batchesCollection.add(batch).await()
            Log.d(TAG, "Batch inserted with ID: ${documentReference.id}")
            batch.id = documentReference.id
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting batch: ${e.message}", e)
            ""
        }
    }

    /**
     * Updates an existing batch in Firestore.
     * @param batch The Batch object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateBatch(batch: Batch): Int {
        return try {
            if (batch.id.isNotBlank()) {
                batchesCollection.document(batch.id).set(batch).await()
                Log.d(TAG, "Batch updated with ID: ${batch.id}")
                1
            } else {
                Log.e(TAG, "Cannot update batch: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating batch: ${e.message}", e)
            0
        }
    }

    /**
     * Deletes a batch from Firestore by its document ID,
     * and also deletes all associated student enrollment links and teacher assignment links,
     * class sessions, recurring class sessions, syllabus progress, learning resources, and announcements linked to this batch.
     * Homeworks are either deleted or have the batch ID removed from their assigned list.
     * @param batchId The Firestore document ID of the batch to delete.
     * @return The number of documents affected (1 for batch + N for links/related data for success, 0 for failure).
     */
    suspend fun deleteBatchById(batchId: String): Int {
        return try {
            val firestoreBatch = db.batch()
            var deletedCount = 0

            // 1. Delete the batch document itself
            val batchDocRef = batchesCollection.document(batchId)
            firestoreBatch.delete(batchDocRef)
            deletedCount++
            Log.d(TAG, "Scheduled deletion of batch with ID: $batchId")

            // 2. Find and delete all associated StudentBatchLink documents in "student_enrollments" collection
            val studentLinksSnapshot = db.collection("student_enrollments")
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            for (document in studentLinksSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of student enrollment link with ID: ${document.id} for batch: $batchId")
            }

            // 3. Find and delete all associated TeacherSubjectBatchLink documents in "teacher_assignments" collection
            val teacherLinksSnapshot = db.collection("teacher_assignments")
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            for (document in teacherLinksSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of teacher assignment link with ID: ${document.id} for batch: $batchId")
            }

            // 4. Find and delete all associated ClassSession documents
            val classSessionsSnapshot = db.collection("class_sessions")
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            for (document in classSessionsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of class session with ID: ${document.id} for batch: $batchId")
            }

            // 5. Find and delete all associated RecurringClassSession documents - ADDED THIS PART
            val recurringClassSessionsSnapshot = db.collection("recurring_class_sessions")
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            for (document in recurringClassSessionsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of recurring class session with ID: ${document.id} for batch: $batchId")
            }


            // 6. Find and delete all associated SyllabusProgress documents
            val syllabusProgressSnapshot = db.collection("syllabus_progress")
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            for (document in syllabusProgressSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of syllabus progress with ID: ${document.id} for batch: $batchId")
            }

            // 7. Find and delete all associated LearningResource documents
            val learningResourcesSnapshot = db.collection("learning_resources")
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            for (document in learningResourcesSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of learning resource with ID: ${document.id} for batch: $batchId")
            }

            // 8. Find and delete all associated Announcement documents
            val announcementsSnapshot = db.collection("announcements")
                .whereEqualTo("batchId", batchId)
                .get()
                .await()
            for (document in announcementsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of announcement with ID: ${document.id} for batch: $batchId")
            }

            // 9. Handle Homework documents: remove batchId from 'assignedBatches' or delete if only assigned to this batch
            val homeworksSnapshot = db.collection("homework")
                .whereArrayContains("assignedBatches", batchId)
                .get()
                .await()

            for (document in homeworksSnapshot.documents) {
                val homework = document.toObject(Homework::class.java)
                if (homework != null) {
                    if (homework.assignedBatches.size == 1 && homework.assignedBatches.contains(batchId)) {
                        // If this batch is the *only* assigned batch, delete the homework
                        firestoreBatch.delete(document.reference)
                        deletedCount++
                        Log.d(TAG, "Scheduled deletion of homework with ID: ${document.id} as it was only assigned to batch: $batchId")
                    } else {
                        // Otherwise, just remove the batchId from the array
                        firestoreBatch.update(document.reference, "assignedBatches", FieldValue.arrayRemove(batchId))
                        Log.d(TAG, "Scheduled update of homework with ID: ${document.id} to remove batch: $batchId")
                    }
                }
            }

            // Commit the batch write
            firestoreBatch.commit().await()
            Log.d(TAG, "Batch and ${deletedCount - 1} associated data deleted successfully for ID: $batchId. Total documents deleted: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting batch by ID $batchId and its associated data: ${e.message}", e)
            0
        }
    }

    /**
     * Retrieves a specific batch from Firestore by its document ID.
     * @param batchId The Firestore document ID of the batch.
     * @return The Batch object if found, null otherwise.
     */
    suspend fun getBatchById(batchId: String): Batch? {
        return try {
            val document = batchesCollection.document(batchId).get().await()
            document.toObject(Batch::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting batch by ID '$batchId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves all batches from Firestore in real-time.
     * Emits a list of Batch objects as a Flow whenever the data changes.
     */
    fun getAllBatches(): Flow<List<Batch>> = callbackFlow {
        val subscription = batchesCollection.orderBy("batchName").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all batches failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val batches = snapshot.toObjects(Batch::class.java)
                    trySend(batches).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Batch list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves batches associated with a specific teacher from Firestore.
     * This requires querying the `teacher_assignments` collection first to get batch IDs,
     * then fetching the corresponding batch documents.
     * Emits a list of Batch objects as a Flow.
     * @param teacherId The ID of the teacher.
     */
    fun getBatchesForTeacher(teacherId: String): Flow<List<Batch>> = callbackFlow {
        val teacherBatchLinksCollection = db.collection("teacher_assignments")

        val linksListenerRegistration = teacherBatchLinksCollection
            .whereEqualTo("teacherUserId", teacherId)
            .addSnapshotListener { linksSnapshot, linksError ->
                if (linksError != null) {
                    Log.e(TAG, "Listen for teacher-batch links for teacher '$teacherId' failed: ${linksError.message}", linksError)
                    close(linksError)
                    return@addSnapshotListener
                }

                val batchIds = linksSnapshot?.map { it.getString("batchId") ?: "" }?.filter { it.isNotBlank() } ?: emptyList()

                if (batchIds.isEmpty()) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                // Firestore `whereIn` has a limit of 10. If more, multiple queries are needed.
                val batchesQuery = batchesCollection.whereIn(FieldPath.documentId(), batchIds.take(10))
                if (batchIds.size > 10) {
                    Log.w(TAG, "More than 10 batch IDs for teacher '$teacherId'. Only fetching first 10 due to Firestore query limit. Consider redesigning for more.")
                }

                batchesQuery.get().addOnSuccessListener { batchesSnapshot ->
                    val fetchedBatches = batchesSnapshot.toObjects(Batch::class.java).sortedBy { it.batchName }
                    Log.d(TAG, "Successfully fetched/updated ${fetchedBatches.size} batches for teacher ID '$teacherId'.")
                    trySend(fetchedBatches).isSuccess
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch batch details for teacher '$teacherId': ${e.message}", e)
                    close(e)
                }
            }

        awaitClose { linksListenerRegistration.remove() } // Corrected reference
    }

    /**
     * Retrieves batches a specific student is enrolled in from Firestore.
     * This requires querying the `student_enrollments` collection first to get batch IDs,
     * then fetching the corresponding batch documents.
     * Emits a list of Batch objects as a Flow.
     * @param studentId The ID of the student.
     */
    fun getBatchesForStudent(studentId: String): Flow<List<Batch>> = callbackFlow {
        val studentBatchLinksCollection = db.collection("student_enrollments")

        val linksListenerRegistration = studentBatchLinksCollection
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { linksSnapshot, linksError ->
                if (linksError != null) {
                    Log.e(TAG, "Listen for student-batch links for student '$studentId' failed: ${linksError.message}", linksError)
                    close(linksError)
                    return@addSnapshotListener
                }

                val batchIds = linksSnapshot?.map { it.getString("batchId") ?: "" }?.filter { it.isNotBlank() } ?: emptyList()

                if (batchIds.isEmpty()) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                val batchesQuery = batchesCollection.whereIn(FieldPath.documentId(), batchIds.take(10))
                if (batchIds.size > 10) {
                    Log.w(TAG, "More than 10 batch IDs for student '$studentId'. Only fetching first 10 due to Firestore query limit. Consider redesigning for more.")
                }

                batchesQuery.get().addOnSuccessListener { batchesSnapshot ->
                    val fetchedBatches = batchesSnapshot.toObjects(Batch::class.java).sortedBy { it.batchName }
                    Log.d(TAG, "Successfully fetched/updated ${fetchedBatches.size} batches for student ID '$studentId'.")
                    trySend(fetchedBatches).isSuccess
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch batch details for student '$studentId': ${e.message}", e)
                    close(e)
                }
            }

        awaitClose { linksListenerRegistration.remove() } // Corrected reference
    }

    /**
     * Searches for batches in Firestore where the batch name, grade level, or academic year matches the query.
     * Emits a list of matching Batch objects as a Flow.
     * @param query The search string.
     */
    fun searchBatches(query: String): Flow<List<Batch>> = callbackFlow {
        val searchListenerRegistration = batchesCollection.whereGreaterThanOrEqualTo("batchName", query) // Renamed to searchListenerRegistration
            .whereLessThanOrEqualTo("batchName", query + '\uf8ff')
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for search batches failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val searchedBatches = snapshot.toObjects(Batch::class.java)
                    Log.d(TAG, "Successfully searched batches with query '$query'. Found: ${searchedBatches.size}")
                    trySend(searchedBatches).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { searchListenerRegistration.remove() } // Corrected reference
    }
}