package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.LearningResource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class LearningResourceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val resourcesCollection = db.collection("learning_resources")
    private val TAG = "LearningResourceRepo"

    /**
     * Inserts a new learning resource into Firestore.
     * @param resource The LearningResource object to insert.
     * @return The Firestore document ID of the newly created resource, or an empty string if insertion fails.
     */
    suspend fun insertLearningResource(resource: LearningResource): String {
        return try {
            // --- START ADDED LOGGING FOR LEARNING RESOURCE OBJECT IN REPO ---
            Log.d(TAG, "Inserting LearningResource into Firestore: $resource")
            // --- END ADDED LOGGING FOR LEARNING RESOURCE OBJECT IN REPO ---
            val documentReference = resourcesCollection.add(resource).await()
            Log.d(TAG, "Learning resource inserted with ID: ${documentReference.id}")
            resource.id = documentReference.id // Corrected: Update the ID of the passed object
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting learning resource: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Updates an existing learning resource in Firestore.
     * @param resource The LearningResource object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateLearningResource(resource: LearningResource): Int {
        return try {
            if (resource.id.isNotBlank()) {
                Log.d(TAG, "Updating LearningResource in Firestore: ${resource.id} with data: $resource")
                resourcesCollection.document(resource.id).set(resource).await()
                Log.d(TAG, "Learning resource updated with ID: ${resource.id}")
                1 // Indicate success
            } else {
                Log.e(TAG, "Cannot update learning resource: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating learning resource: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Deletes a learning resource from Firestore by its document ID.
     * @param resourceId The Firestore document ID of the resource to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteLearningResourceById(resourceId: String): Int {
        return try {
            Log.d(TAG, "Deleting learning resource with ID: $resourceId")
            resourcesCollection.document(resourceId).delete().await()
            Log.d(TAG, "Learning resource deleted with ID: $resourceId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting learning resource by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Retrieves a specific learning resource from Firestore by its document ID.
     * @param resourceId The Firestore document ID of the resource.
     * @return The LearningResource object if found, null otherwise.
     */
    suspend fun getLearningResourceById(resourceId: String): LearningResource? {
        return try {
            val document = resourcesCollection.document(resourceId).get().await()
            val resource = document.toObject(LearningResource::class.java)
            Log.d(TAG, "Fetched learning resource by ID '$resourceId': $resource")
            resource
        } catch (e: Exception) {
            Log.e(TAG, "Error getting learning resource by ID '$resourceId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves all learning resources from Firestore in real-time, ordered by upload date descending.
     * Emits a list of LearningResource objects as a Flow whenever the data changes.
     */
    fun getAllLearningResources(): Flow<List<LearningResource>> = callbackFlow {
        Log.d(TAG, "Setting up listener for all learning resources.")
        val subscription = resourcesCollection.orderBy("uploadDate", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all learning resources failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val resources = snapshot.toObjects(LearningResource::class.java)
                    Log.d(TAG, "Received ${resources.size} all learning resources from Firestore.")
                    resources.forEach { Log.v(TAG, "All Resource Item: ID=${it.id}, Title=${it.title}, TeacherID=${it.uploadedByTeacherId}") }
                    trySend(resources).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to LearningResource list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                Log.d(TAG, "No all learning resources found or snapshot is empty.")
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves learning resources from Firestore filtered by subject ID in real-time, ordered by upload date descending.
     * Emits a list of LearningResource objects as a Flow whenever the data changes.
     * @param subjectId The ID of the subject.
     */
    fun getLearningResourcesBySubject(subjectId: String): Flow<List<LearningResource>> = callbackFlow {
        Log.d(TAG, "Setting up listener for learning resources by subject ID: $subjectId")
        val subscription = resourcesCollection.whereEqualTo("subjectId", subjectId)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for learning resources by subject ID '$subjectId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val resources = snapshot.toObjects(LearningResource::class.java)
                        Log.d(TAG, "Received ${resources.size} learning resources for subject ID '$subjectId'.")
                        resources.forEach { Log.v(TAG, "Subject Resource Item: ID=${it.id}, Title=${it.title}, TeacherID=${it.uploadedByTeacherId}") }
                        trySend(resources).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to LearningResource list for subject ID '$subjectId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    Log.d(TAG, "No learning resources found for subject ID '$subjectId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves learning resources from Firestore filtered by batch ID in real-time, ordered by upload date descending.
     * Emits a list of LearningResource objects as a Flow whenever the data changes.
     * @param batchId The ID of the batch.
     */
    fun getLearningResourcesByBatch(batchId: String): Flow<List<LearningResource>> = callbackFlow {
        Log.d(TAG, "Setting up listener for learning resources by batch ID: $batchId")
        val subscription = resourcesCollection.whereEqualTo("batchId", batchId)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for learning resources by batch ID '$batchId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val resources = snapshot.toObjects(LearningResource::class.java)
                        Log.d(TAG, "Received ${resources.size} learning resources for batch ID '$batchId'.")
                        resources.forEach { Log.v(TAG, "Batch Resource Item: ID=${it.id}, Title=${it.title}, TeacherID=${it.uploadedByTeacherId}") }
                        trySend(resources).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to LearningResource list for batch ID '$batchId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    Log.d(TAG, "No learning resources found for batch ID '$batchId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves learning resources from Firestore filtered by teacher ID in real-time, ordered by upload date descending.
     * Emits a list of LearningResource objects as a Flow whenever the data changes.
     * @param teacherId The ID of the teacher.
     */
    fun getLearningResourcesByTeacher(teacherId: String): Flow<List<LearningResource>> = callbackFlow {
        Log.d(TAG, "Setting up listener for learning resources by teacher ID: $teacherId")
        val subscription = resourcesCollection.whereEqualTo("uploadedByTeacherId", teacherId)
            .orderBy("uploadDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for learning resources by teacher ID '$teacherId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val resources = snapshot.toObjects(LearningResource::class.java)
                        Log.d(TAG, "Received ${resources.size} learning resources for teacher ID '$teacherId'.")
                        resources.forEach { Log.v(TAG, "Teacher Resource Item: ID=${it.id}, Title=${it.title}, SubjectID=${it.subjectId}, BatchID=${it.batchId}") }
                        trySend(resources).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to LearningResource list for teacher ID '$teacherId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    Log.d(TAG, "No learning resources found for teacher ID '$teacherId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }
}
