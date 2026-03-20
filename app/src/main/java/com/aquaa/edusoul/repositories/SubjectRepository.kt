package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.SyllabusTopic // Added import for SyllabusTopic model
import com.aquaa.edusoul.models.TeacherSubjectBatchLink // Added import for TeacherSubjectBatchLink model
import com.aquaa.edusoul.models.Exam // Added import for Exam model
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class SubjectRepository {

    private val db = FirebaseFirestore.getInstance()
    private val subjectsCollection = db.collection("subjects")
    private val TAG = "SubjectRepository"

    /**
     * Inserts a new subject into Firestore.
     * @param subject The Subject object to insert.
     * @return The Firestore document ID of the newly created subject, or an empty string if insertion fails.
     */
    suspend fun insertSubject(subject: Subject): String {
        return try {
            val documentReference = subjectsCollection.add(subject).await()
            Log.d(TAG, "Subject inserted with ID: ${documentReference.id}")
            subject.id = documentReference.id
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting subject: ${e.message}", e)
            ""
        }
    }

    /**
     * Updates an existing subject in Firestore.
     * @param subject The Subject object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateSubject(subject: Subject): Int {
        return try {
            if (subject.id.isNotBlank()) {
                subjectsCollection.document(subject.id).set(subject).await()
                Log.d(TAG, "Subject updated with ID: ${subject.id}")
                1
            } else {
                Log.e(TAG, "Cannot update subject: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating subject: ${e.message}", e)
            0
        }
    }

    /**
     * Deletes a subject from Firestore by its document ID,
     * and also deletes all associated syllabus topics, teacher subject batch links,
     * exams, homeworks, learning resources, and announcements linked to this subject.
     * @param subjectId The Firestore document ID of the subject to delete.
     * @return The number of documents affected (1 for subject + N for related data for success, 0 for failure).
     */
    suspend fun deleteSubjectById(subjectId: String): Int {
        return try {
            val firestoreBatch = db.batch()
            var deletedCount = 0

            // 1. Delete the subject document itself
            val subjectDocRef = subjectsCollection.document(subjectId)
            firestoreBatch.delete(subjectDocRef)
            deletedCount++
            Log.d(TAG, "Scheduled deletion of subject with ID: $subjectId")

            // 2. Find and delete all associated SyllabusTopic documents
            val syllabusTopicsSnapshot = db.collection("syllabus_topics")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            for (document in syllabusTopicsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of syllabus topic with ID: ${document.id} for subject: $subjectId")
            }

            // 3. Find and delete all associated TeacherSubjectBatchLink documents
            val teacherLinksSnapshot = db.collection("teacher_assignments")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            for (document in teacherLinksSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of teacher assignment link with ID: ${document.id} for subject: $subjectId")
            }

            // 4. Find and delete all associated Exam documents
            val examsSnapshot = db.collection("exams")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            for (document in examsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of exam with ID: ${document.id} for subject: $subjectId")
            }

            // 5. Find and delete all associated Homework documents
            val homeworksSnapshot = db.collection("homework")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            for (document in homeworksSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of homework with ID: ${document.id} for subject: $subjectId")
            }

            // 6. Find and delete all associated LearningResource documents
            val learningResourcesSnapshot = db.collection("learning_resources")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            for (document in learningResourcesSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of learning resource with ID: ${document.id} for subject: $subjectId")
            }

            // 7. Find and delete all associated Announcement documents
            val announcementsSnapshot = db.collection("announcements")
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
            for (document in announcementsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of announcement with ID: ${document.id} for subject: $subjectId")
            }

            // Commit the batch write
            firestoreBatch.commit().await()
            Log.d(TAG, "Subject and ${deletedCount - 1} associated data deleted successfully for ID: $subjectId. Total documents deleted: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subject by ID $subjectId and its associated data: ${e.message}", e)
            0
        }
    }

    /**
     * Retrieves all subjects from Firestore, ordered by subject name ascending.
     * Emits a list of Subject objects as a Flow.
     */
    fun getAllSubjects(): Flow<List<Subject>> = callbackFlow {
        val subscription = subjectsCollection.orderBy("subjectName").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all subjects failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val subjects = snapshot.toObjects(Subject::class.java)
                Log.d(TAG, "Successfully fetched/updated all subjects. Count: ${subjects.size}")
                trySend(subjects).isSuccess
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting all subjects: ${e.message}", e)
        emit(emptyList())
    }

    /**
     * Retrieves a specific subject from Firestore by its document ID.
     * @param subjectId The Firestore document ID of the subject.
     * @return The Subject object if found, null otherwise.
     */
    suspend fun getSubjectById(subjectId: String): Subject? {
        return try {
            val document = subjectsCollection.document(subjectId).get().await()
            if (document.exists()) {
                Log.d(TAG, "Successfully fetched subject by ID '$subjectId'.")
                document.toObject(Subject::class.java)
            } else {
                Log.w(TAG, "Subject with ID '$subjectId' not found.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subject by ID '$subjectId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves subjects for a specific teacher and batch from Firestore.
     * This requires querying the `teacher_assignments` collection first to get subject IDs,
     * then fetching the corresponding subject documents.
     * Emits a list of Subject objects as a Flow.
     * @param teacherId The ID of the teacher.
     * @param batchId The ID of the batch.
     */
    fun getSubjectsForTeacherAndBatch(teacherId: String, batchId: String): Flow<List<Subject>> = callbackFlow {
        val teacherBatchLinksCollection = db.collection("teacher_assignments")

        val linksListenerRegistration = teacherBatchLinksCollection
            .whereEqualTo("teacherUserId", teacherId)
            .whereEqualTo("batchId", batchId)
            .addSnapshotListener { linksSnapshot, linksError ->
                if (linksError != null) {
                    Log.e(TAG, "Listen for teacher-batch links for teacher '$teacherId' and batch '$batchId' failed: ${linksError.message}", linksError)
                    close(linksError)
                    return@addSnapshotListener
                }

                val subjectIds = linksSnapshot?.map { it.getString("subjectId") ?: "" }?.filter { it.isNotBlank() } ?: emptyList()

                if (subjectIds.isEmpty()) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                val subjectsQuery = subjectsCollection.whereIn(FieldPath.documentId(), subjectIds.take(10))
                if (subjectIds.size > 10) {
                    Log.w(TAG, "More than 10 subject IDs for teacher '$teacherId' and batch '$batchId'. Only fetching first 10 due to Firestore query limit. Consider redesigning for more.")
                }

                subjectsQuery.get().addOnSuccessListener { subjectsSnapshot ->
                    val fetchedSubjects = subjectsSnapshot.toObjects(Subject::class.java).sortedBy { it.subjectName }
                    Log.d(TAG, "Successfully fetched/updated ${fetchedSubjects.size} subjects for teacher ID '$teacherId' and batch ID '$batchId'.")
                    trySend(fetchedSubjects).isSuccess
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch subject details for teacher '$teacherId' and batch '$batchId': ${e.message}", e)
                    close(e)
                }
            }

        awaitClose { linksListenerRegistration.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting subjects for teacher and batch: ${e.message}", e)
        emit(emptyList())
    }

    /**
     * Searches for subjects in Firestore where the subject name or subject code matches the query.
     * Emits a list of matching Subject objects as a Flow.
     * @param query The search string.
     */
    fun searchSubjects(query: String): Flow<List<Subject>> = callbackFlow {
        val subscription = subjectsCollection.whereGreaterThanOrEqualTo("subjectName", query)
            .whereLessThanOrEqualTo("subjectName", query + '\uf8ff')
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for search subjects failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val searchedSubjects = snapshot.toObjects(Subject::class.java)
                    Log.d(TAG, "Successfully searched subjects with query '$query'. Found: ${searchedSubjects.size}")
                    trySend(searchedSubjects).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error searching subjects with query: ${e.message}", e)
        emit(emptyList())
    }

    /**
     * Checks if a subject with the given code already exists.
     * @param subjectCode The subject code to check.
     * @return True if a subject with the code exists, false otherwise.
     */
    suspend fun isSubjectCodeExists(subjectCode: String): Boolean {
        return try {
            val snapshot = subjectsCollection.whereEqualTo("subjectCode", subjectCode).limit(1).get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if subject code exists '$subjectCode': ${e.message}", e)
            false
        }
    }

    /**
     * Checks if a subject with the given code already exists, excluding a specific subject ID.
     * @param subjectCode The subject code to check.
     * @param excludeSubjectId The ID of the subject to exclude from the check.
     * @return True if a subject with the code exists (and is not the excluded one), false otherwise.
     */
    suspend fun isSubjectCodeExists(subjectCode: String, excludeSubjectId: String): Boolean {
        return try {
            val snapshot = subjectsCollection.whereEqualTo("subjectCode", subjectCode).get().await()
            snapshot.documents.any { it.id != excludeSubjectId }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if subject code exists (excluding ID) '$subjectCode': ${e.message}", e)
            false
        }
    }
}