package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.ClassSession
import com.aquaa.edusoul.models.Attendance // Added import for Attendance model
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class ClassSessionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val classSessionsCollection = db.collection("class_sessions")
    private val TAG = "ClassSessionRepo"

    suspend fun insertClassSession(classSession: ClassSession): String {
        return try {
            val documentReference = classSessionsCollection.add(classSession).await()
            Log.e(TAG, "Class session inserted with ID: ${documentReference.id}")
            // Update the object with the new ID, though this might not be necessary depending on usage
            classSession.id = documentReference.id
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting class session: ${e.message}", e)
            ""
        }
    }

    suspend fun updateClassSession(classSession: ClassSession): Int {
        return try {
            if (classSession.id.isNotBlank()) {
                classSessionsCollection.document(classSession.id).set(classSession).await()
                Log.e(TAG, "Class session updated with ID: ${classSession.id}")
                1
            } else {
                Log.e(TAG, "Cannot update class session: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating class session: ${e.message}", e)
            0
        }
    }

    suspend fun deleteClassSessionById(sessionId: String): Int {
        return try {
            val firestoreBatch = db.batch()
            var deletedCount = 0

            val sessionDocRef = classSessionsCollection.document(sessionId)
            firestoreBatch.delete(sessionDocRef)
            deletedCount++
            Log.e(TAG, "Scheduled deletion of class session with ID: $sessionId")

            val attendanceSnapshot = db.collection("attendance")
                .whereEqualTo("classSessionId", sessionId)
                .get()
                .await()
            for (document in attendanceSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.e(TAG, "Scheduled deletion of attendance record with ID: ${document.id} for class session: $sessionId")
            }

            firestoreBatch.commit().await()
            Log.e(TAG, "Class session and ${deletedCount - 1} associated attendance records deleted successfully for ID: $sessionId. Total documents deleted: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting class session by ID $sessionId and its associated data: ${e.message}", e)
            0
        }
    }

    suspend fun getClassSessionById(sessionId: String): ClassSession? {
        return try {
            val document = classSessionsCollection.document(sessionId).get().await()
            document.toObject(ClassSession::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting class session by ID '$sessionId': ${e.message}", e)
            null
        }
    }

    fun getClassSessionsForDate(date: String): Flow<List<ClassSession>> = callbackFlow {
        Log.e(TAG, "Querying class sessions for date: $date")
        try {
            val listenerRegistration = classSessionsCollection.whereEqualTo("sessionDate", date)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed for class sessions for date '$date'", e)
                        close(e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val sessions = snapshot.toObjects(ClassSession::class.java)
                            Log.e(TAG, "Fetched ${sessions.size} class sessions for date: $date")
                            trySend(sessions).isSuccess
                        } catch (castException: Exception) {
                            Log.e(TAG, "Error converting snapshot to ClassSession list for date '$date': ${castException.message}", castException)
                            close(castException)
                        }
                    } else {
                        Log.e(TAG, "No class sessions found for date '$date' or snapshot is empty.")
                        trySend(emptyList()).isSuccess
                    }
                }

            awaitClose {
                listenerRegistration.remove()
                Log.e(TAG, "Firestore listener for class sessions for date '$date' removed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up class sessions for date listener: ${e.message}", e)
            close(e)
        }
    }

    /**
     * NEW FUNCTION
     * Retrieves all class sessions for a specific date by querying a timestamp range.
     * This is ideal for the manager's view.
     * Assumes 'startTime' in Firestore is a Timestamp or Long representing the start of the session.
     */
    fun getSessionsByDate(date: Date): Flow<List<ClassSession>> = callbackFlow {
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        Log.d(TAG, "Querying sessions between $startOfDay and $endOfDay")

        val subscription = classSessionsCollection
            .whereGreaterThanOrEqualTo("startTime", startOfDay)
            .whereLessThanOrEqualTo("startTime", endOfDay)
            .orderBy("startTime")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for getSessionsByDate", e)
                    close(e)
                    return@addSnapshotListener
                }
                val sessions = snapshot?.toObjects(ClassSession::class.java) ?: emptyList()
                Log.d(TAG, "Fetched ${sessions.size} sessions for the selected date.")
                trySend(sessions).isSuccess
            }
        awaitClose { subscription.remove() }
    }


    fun getScheduledSessionsForTeacher(teacherId: String, date: String): Flow<List<ClassSession>> = callbackFlow {
        Log.e(TAG, "Querying scheduled sessions for teacherId: $teacherId on date: $date")
        try {
            val listenerRegistration = classSessionsCollection.whereEqualTo("teacherUserId", teacherId)
                .whereEqualTo("sessionDate", date)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed for scheduled sessions for teacher '$teacherId' on date '$date': ${e.message}", e)
                        close(e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val sessions = snapshot.toObjects(ClassSession::class.java)
                            Log.e(TAG, "Fetched ${sessions.size} scheduled sessions for teacher '$teacherId' on date '$date'.")
                            trySend(sessions).isSuccess
                        } catch (castException: Exception) {
                            Log.e(TAG, "Error converting snapshot to ClassSession list for teacher '$teacherId' on date '$date': ${castException.message}", castException)
                            close(castException)
                        }
                    } else {
                        Log.e(TAG, "No scheduled sessions found for teacher '$teacherId' on date '$date' or snapshot is empty.")
                        trySend(emptyList()).isSuccess
                    }
                }

            awaitClose {
                listenerRegistration.remove()
                Log.e(TAG, "Firestore listener for scheduled sessions for teacher '$teacherId' on date '$date' removed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up scheduled sessions for teacher listener: ${e.message}", e)
            close(e)
        }
    }

    fun getClassSessionsForBatchAndDate(batchId: String, date: String): Flow<List<ClassSession>> = callbackFlow {
        Log.e(TAG, "Querying class sessions for batchId: $batchId on date: $date")
        try {
            val listenerRegistration = classSessionsCollection.whereEqualTo("batchId", batchId)
                .whereEqualTo("sessionDate", date)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed for class sessions for batch '$batchId' on date '$date': ${e.message}", e)
                        close(e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val sessions = snapshot.toObjects(ClassSession::class.java)
                            Log.e(TAG, "Fetched ${sessions.size} class sessions for batch '$batchId' on date '$date'.")
                            trySend(sessions).isSuccess
                        } catch (castException: Exception) {
                            Log.e(TAG, "Error converting snapshot to ClassSession list for batch '$batchId' on date '$date': ${castException.message}", castException)
                            close(castException)
                        }
                    } else {
                        Log.e(TAG, "No class sessions found for batch '$batchId' on date '$date' or snapshot is empty.")
                        trySend(emptyList()).isSuccess
                    }
                }

            awaitClose {
                listenerRegistration.remove()
                Log.e(TAG, "Firestore listener for class sessions for batch '$batchId' on date '$date' removed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up class sessions for batch and date listener: ${e.message}", e)
            close(e)
        }
    }

    fun getAllClassSessions(): Flow<List<ClassSession>> = callbackFlow {
        Log.e(TAG, "Querying all class sessions")
        try {
            val listenerRegistration = classSessionsCollection
                .orderBy("sessionDate", Query.Direction.DESCENDING)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen for all class sessions failed", e)
                        close(e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        try {
                            val sessions = snapshot.toObjects(ClassSession::class.java)
                            Log.e(TAG, "Fetched ${sessions.size} all class sessions.")
                            trySend(sessions).isSuccess
                        } catch (castException: Exception) {
                            Log.e(TAG, "Error converting snapshot to ClassSession list for all sessions: ${castException.message}", castException)
                            close(castException)
                        }
                    } else {
                        Log.e(TAG, "No class sessions found or snapshot is empty.")
                        trySend(emptyList()).isSuccess
                    }
                }

            awaitClose {
                listenerRegistration.remove()
                Log.e(TAG, "Firestore listener for all class sessions removed.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up all class sessions listener: ${e.message}", e)
            close(e)
        }
    }

    suspend fun getClassSessionsForDateRange(startDate: String, endDate: String): List<ClassSession> {
        return try {
            Log.e(TAG, "Fetching class sessions for date range: $startDate to $endDate")
            val snapshot = classSessionsCollection
                .whereGreaterThanOrEqualTo("sessionDate", startDate)
                .whereLessThanOrEqualTo("sessionDate", endDate)
                .orderBy("sessionDate", Query.Direction.ASCENDING)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
            val sessions = snapshot.toObjects(ClassSession::class.java)
            Log.e(TAG, "Fetched ${sessions.size} class sessions for date range.")
            sessions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting class sessions for date range '$startDate' to '$endDate': ${e.message}", e)
            emptyList()
        }
    }

    suspend fun findClassSessionByDetails(date: String, batchId: String, startTime: String): ClassSession? {
        return try {
            Log.e(TAG, "Finding class session by details: date=$date, batchId=$batchId, startTime=$startTime")
            val snapshot = classSessionsCollection
                .whereEqualTo("sessionDate", date)
                .whereEqualTo("batchId", batchId)
                .whereEqualTo("startTime", startTime)
                .limit(1)
                .get()
                .await()
            val session = snapshot.documents.firstOrNull()?.toObject(ClassSession::class.java)
            if (session != null) {
                Log.e(TAG, "Found class session by details: ${session.id}")
            } else {
                Log.e(TAG, "No class session found for details: date=$date, batchId=$batchId, startTime=$startTime")
            }
            session
        } catch (e: Exception) {
            Log.e(TAG, "Error finding class session by details: ${e.message}", e)
            null
        }
    }
}
