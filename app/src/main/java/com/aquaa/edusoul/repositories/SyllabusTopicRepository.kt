package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.SyllabusTopic
import com.aquaa.edusoul.models.SyllabusProgress // Added import for SyllabusProgress model
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class SyllabusTopicRepository {

    private val db = FirebaseFirestore.getInstance()
    private val topicsCollection = db.collection("syllabus_topics")
    private val TAG = "SyllabusTopicRepository"

    /**
     * Inserts a new syllabus topic into Firestore.
     * @param topic The SyllabusTopic object to insert.
     * @return The Firestore document ID of the newly created topic, or an empty string if insertion fails.
     */
    suspend fun insertSyllabusTopic(topic: SyllabusTopic): String {
        return try {
            val documentReference = topicsCollection.add(topic).await()
            Log.d(TAG, "Syllabus topic inserted with ID: ${documentReference.id}")
            topic.id = documentReference.id
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting syllabus topic: ${e.message}", e)
            ""
        }
    }

    /**
     * Updates an existing syllabus topic in Firestore.
     * @param topic The SyllabusTopic object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateSyllabusTopic(topic: SyllabusTopic): Int {
        return try {
            if (topic.id.isNotBlank()) {
                topicsCollection.document(topic.id).set(topic).await()
                Log.d(TAG, "Syllabus topic updated with ID: ${topic.id}")
                1
            } else {
                Log.e(TAG, "Cannot update syllabus topic: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating syllabus topic: ${e.message}", e)
            0
        }
    }

    /**
     * Deletes a syllabus topic from Firestore by its document ID,
     * and also deletes all associated syllabus progress records for this topic.
     * @param topicId The Firestore document ID of the topic to delete.
     * @return The number of documents affected (1 for topic + N for progress records for success, 0 for failure).
     */
    suspend fun deleteSyllabusTopicById(topicId: String): Int {
        return try {
            val firestoreBatch = db.batch()
            var deletedCount = 0

            // 1. Delete the syllabus topic document itself
            val topicDocRef = topicsCollection.document(topicId)
            firestoreBatch.delete(topicDocRef)
            deletedCount++
            Log.d(TAG, "Scheduled deletion of syllabus topic with ID: $topicId")

            // 2. Find and delete all associated SyllabusProgress documents
            val syllabusProgressSnapshot = db.collection("syllabus_progress") // Assuming "syllabus_progress" is the collection name
                .whereEqualTo("syllabusTopicId", topicId)
                .get()
                .await()
            for (document in syllabusProgressSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of syllabus progress with ID: ${document.id} for topic: $topicId")
            }

            // Commit the batch write
            firestoreBatch.commit().await()
            Log.d(TAG, "Syllabus topic and ${deletedCount - 1} associated progress records deleted successfully for ID: $topicId. Total documents deleted: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting syllabus topic by ID $topicId and its associated data: ${e.message}", e)
            0
        }
    }

    /**
     * Retrieves syllabus topics for a specific subject from Firestore in real-time,
     * ordered by topic order ascending and then by topic name ascending.
     * Emits a list of SyllabusTopic objects as a Flow.
     * @param subjectId The ID of the subject.
     */
    fun getTopicsForSubject(subjectId: String): Flow<List<SyllabusTopic>> = callbackFlow {
        val subscription = topicsCollection.whereEqualTo("subjectId", subjectId)
            .orderBy("order", Query.Direction.ASCENDING)
            .orderBy("topicName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for syllabus topics for subject ID '$subjectId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val topics = snapshot.toObjects(SyllabusTopic::class.java)
                    Log.d(TAG, "Successfully fetched/updated ${topics.size} syllabus topics for subject ID '$subjectId'.")
                    trySend(topics).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting syllabus topics for subject ID: ${e.message}", e)
        emit(emptyList())
    }

    /**
     * Retrieves all syllabus topics from Firestore in real-time,
     * ordered by subject ID, topic order, and topic name.
     * Emits a list of SyllabusTopic objects as a Flow.
     */
    fun getAllSyllabusTopics(): Flow<List<SyllabusTopic>> = callbackFlow {
        val subscription = topicsCollection
            .orderBy("subjectId")
            .orderBy("order")
            .orderBy("topicName")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for all syllabus topics failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val topics = snapshot.toObjects(SyllabusTopic::class.java)
                    Log.d(TAG, "Successfully fetched/updated all syllabus topics. Count: ${topics.size}")
                    trySend(topics).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }.catch { e ->
        Log.e(TAG, "Error getting all syllabus topics: ${e.message}", e)
        emit(emptyList())
    }

    /**
     * Retrieves a specific syllabus topic from Firestore by its document ID.
     * @param topicId The Firestore document ID of the topic.
     * @return The SyllabusTopic object if found, null otherwise.
     */
    suspend fun getSyllabusTopicById(topicId: String): SyllabusTopic? {
        return try {
            val document = topicsCollection.document(topicId).get().await()
            document.toObject(SyllabusTopic::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting syllabus topic by ID '$topicId': ${e.message}", e)
            null
        }
    }
}