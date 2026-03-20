// File: EduSoul/app/src/main/java/com/aquaa/edusoul/repositories/AttendanceRepository.kt
package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Attendance
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch // Import catch operator
import kotlinx.coroutines.tasks.await

class AttendanceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val attendanceCollection = db.collection("attendance")
    private val TAG = "AttendanceRepository"

    /**
     * Inserts a new attendance record into Firestore.
     * @param attendance The Attendance object to insert.
     * @return The Firestore document ID of the newly created attendance record, or an empty string if insertion fails.
     */
    suspend fun insertAttendance(attendance: Attendance): String {
        return try {
            val documentReference = attendanceCollection.add(attendance).await()
            Log.d(TAG, "Attendance inserted with ID: ${documentReference.id}")
            attendance.id = documentReference.id // Corrected: Update the ID of the passed object
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting attendance: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Updates an existing attendance record in Firestore.
     * @param attendance The Attendance object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateAttendance(attendance: Attendance): Int {
        return try {
            if (attendance.id.isNotBlank()) {
                attendanceCollection.document(attendance.id).set(attendance).await()
                Log.d(TAG, "Attendance updated with ID: ${attendance.id}")
                1 // Indicate success
            } else {
                Log.w(TAG, "Cannot update attendance: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating attendance: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Deletes an attendance record from Firestore by its document ID.
     * @param attendanceId The Firestore document ID of the attendance to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteAttendanceById(attendanceId: String): Int {
        return try {
            attendanceCollection.document(attendanceId).delete().await()
            Log.d(TAG, "Attendance deleted with ID: $attendanceId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting attendance by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Retrieves all attendance records from Firestore in real-time.
     * Emits a list of Attendance objects as a Flow.
     */
    fun getAllAttendance(): Flow<List<Attendance>> = callbackFlow {
        // FIX: Changed orderBy field from "timestamp" to "markedAt"
        val subscription = attendanceCollection.orderBy("markedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for all attendance failed: ${e.message}", e)
                    trySend(emptyList()).isSuccess // Emit empty list on error
                    close(e) // Close the flow with the exception
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val attendanceList = snapshot.toObjects(Attendance::class.java)
                    Log.d(TAG, "Successfully fetched/updated all attendance. Count: ${attendanceList.size}")
                    trySend(attendanceList).isSuccess
                } else {
                    Log.d(TAG, "No attendance records found or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves attendance records for a specific class session from Firestore in real-time.
     * @param classSessionId The ID of the class session.
     * @return A Flow emitting a list of Attendance objects.
     */
    fun getAttendanceForSession(classSessionId: String): Flow<List<Attendance>> = callbackFlow {
        val subscription = attendanceCollection
            .whereEqualTo("classSessionId", classSessionId)
            // Removed: .orderBy("studentId") // No specific order needed here for this query
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for attendance for session '$classSessionId': ${e.message}", e)
                    trySend(emptyList()).isSuccess // Emit empty list on error
                    close(e) // Close the flow with the exception
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val attendanceList = mutableListOf<Attendance>()
                    if (snapshot.documents.isEmpty()) {
                        Log.d(TAG, "Snapshot for session '$classSessionId' is not null, but contains 0 documents.")
                    } else {
                        Log.d(TAG, "Snapshot for session '$classSessionId' contains ${snapshot.documents.size} documents. Attempting deserialization...")
                        for (document in snapshot.documents) {
                            try {
                                val attendance = document.toObject(Attendance::class.java)
                                if (attendance != null) {
                                    attendanceList.add(attendance)
                                    Log.d(TAG, "Deserialized attendance record: ${attendance.id}")
                                } else {
                                    Log.w(TAG, "Failed to deserialize document ${document.id} to Attendance object (returned null).")
                                }
                            } catch (deserializationE: Exception) {
                                Log.e(TAG, "Error deserializing document ${document.id}: ${deserializationE.message}", deserializationE)
                            }
                        }
                    }

                    Log.d(TAG, "Successfully fetched/updated ${attendanceList.size} attendance records for session '$classSessionId'.")
                    trySend(attendanceList).isSuccess
                } else {
                    Log.d(TAG, "No attendance records found for session '$classSessionId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves attendance records for a specific student from Firestore in real-time.
     * @param studentId The ID of the student.
     * @return A Flow emitting a list of Attendance objects.
     */
    fun getAttendanceForStudent(studentId: String): Flow<List<Attendance>> = callbackFlow {
        // FIX: Changed orderBy field from "timestamp" to "markedAt"
        val subscription = attendanceCollection
            .whereEqualTo("studentId", studentId)
            .orderBy("markedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed for attendance for student '$studentId': ${e.message}", e)
                    trySend(emptyList()).isSuccess // Emit empty list on error
                    close(e) // Close the flow with the exception
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val attendanceList = snapshot.toObjects(Attendance::class.java)
                    Log.d(TAG, "Successfully fetched/updated ${attendanceList.size} attendance records for student '$studentId'.")
                    trySend(attendanceList).isSuccess
                } else {
                    Log.d(TAG, "No attendance records found for student '$studentId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Inserts or updates multiple attendance records using a batch write.
     * For new records (id is blank), Firestore generates an ID. For existing records, it updates.
     * @param attendances The list of Attendance objects to upsert.
     */
    suspend fun upsertAll(attendances: List<Attendance>) {
        val batch = db.batch()
        try {
            for (attendance in attendances) {
                val docRef = if (attendance.id.isNotBlank()) attendanceCollection.document(attendance.id) else attendanceCollection.document()
                // Update the attendance object with the generated ID if it was new
                if (attendance.id.isBlank()) {
                    attendance.id = docRef.id
                }
                batch.set(docRef, attendance)
            }
            batch.commit().await()
            Log.d(TAG, "Successfully upserted ${attendances.size} attendance records.")
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting multiple attendance records: ${e.message}", e)
            throw e // Re-throw to be handled by ViewModel
        }
    }
}