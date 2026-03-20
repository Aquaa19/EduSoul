package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Announcement
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class AnnouncementRepository {

    private val db = FirebaseFirestore.getInstance()
    private val announcementsCollection = db.collection("announcements")
    private val TAG = "AnnouncementRepository"

    /**
     * Inserts a new announcement into Firestore.
     * @param announcement The Announcement object to insert.
     * @return The Firestore document ID of the newly created announcement, or an empty string if insertion fails.
     */
    suspend fun insertAnnouncement(announcement: Announcement): String {
        return try {
            val documentReference = announcementsCollection.add(announcement).await()
            Log.d(TAG, "Announcement inserted with ID: ${documentReference.id}")
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting announcement: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Updates an existing announcement in Firestore.
     * @param announcement The Announcement object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateAnnouncement(announcement: Announcement): Int {
        return try {
            if (announcement.id.isNotBlank()) {
                announcementsCollection.document(announcement.id).set(announcement).await()
                Log.d(TAG, "Announcement updated with ID: ${announcement.id}")
                1 // Indicate success
            } else {
                Log.e(TAG, "Cannot update announcement: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating announcement: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Deletes an announcement from Firestore by its document ID.
     * @param announcementId The Firestore document ID of the announcement to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteAnnouncementById(announcementId: String): Int {
        return try {
            announcementsCollection.document(announcementId).delete().await()
            Log.d(TAG, "Announcement deleted with ID: $announcementId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting announcement by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Retrieves a specific announcement from Firestore by its document ID.
     * @param announcementId The Firestore document ID of the announcement.
     * @return The Announcement object if found, null otherwise.
     */
    suspend fun getAnnouncementById(announcementId: String): Announcement? {
        return try {
            val document = announcementsCollection.document(announcementId).get().await()
            document.toObject(Announcement::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting announcement by ID '$announcementId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves all announcements from Firestore in real-time, ordered by publish date descending.
     * Emits a list of announcements as a Flow whenever the data changes.
     */
    fun getAllAnnouncements(): Flow<List<Announcement>> = callbackFlow {
        val subscription = announcementsCollection.orderBy("publishDate", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all announcements failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val announcements = snapshot.toObjects(Announcement::class.java)
                    trySend(announcements).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Announcement list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves announcements from Firestore filtered by target audience in real-time, ordered by publish date descending.
     * Emits a list of announcements as a Flow whenever the data changes.
     * @param audience The target audience (e.g., "ALL", "PARENTS", "TEACHERS").
     */
    fun getAnnouncementsByAudience(audience: String): Flow<List<Announcement>> = callbackFlow {
        val subscription = announcementsCollection.whereEqualTo("targetAudience", audience)
            .orderBy("publishDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for announcements by audience '$audience' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val announcements = snapshot.toObjects(Announcement::class.java)
                        trySend(announcements).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to Announcement list for audience '$audience': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves announcements from Firestore filtered by batch ID in real-time, ordered by publish date descending.
     * Emits a list of announcements as a Flow whenever the data changes.
     * @param batchId The ID of the target batch.
     */
    fun getAnnouncementsByBatch(batchId: String): Flow<List<Announcement>> = callbackFlow {
        val subscription = announcementsCollection.whereEqualTo("batchId", batchId)
            .orderBy("publishDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for announcements by batch ID '$batchId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val announcements = snapshot.toObjects(Announcement::class.java)
                        trySend(announcements).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to Announcement list for batch ID '$batchId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves announcements from Firestore filtered by subject ID in real-time, ordered by publish date descending.
     * Emits a list of announcements as a Flow whenever the data changes.
     * @param subjectId The ID of the target subject.
     */
    fun getAnnouncementsBySubject(subjectId: String): Flow<List<Announcement>> = callbackFlow {
        val subscription = announcementsCollection.whereEqualTo("subjectId", subjectId)
            .orderBy("publishDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for announcements by subject ID '$subjectId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val announcements = snapshot.toObjects(Announcement::class.java)
                        trySend(announcements).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to Announcement list for subject ID '$subjectId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }
}