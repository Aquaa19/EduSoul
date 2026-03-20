package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.StudentAssignment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class StudentAssignmentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val assignmentsCollection = db.collection("student_assignments")
    private val TAG = "StudentAssignmentRepo"

    suspend fun insertStudentAssignment(studentAssignment: StudentAssignment): String {
        return try {
            val documentReference = assignmentsCollection.add(studentAssignment).await()
            // Changed to Log.d for success
            Log.d(TAG, "Student assignment inserted with ID: ${documentReference.id}")
            studentAssignment.id = documentReference.id // Corrected: Update the ID of the passed object
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting student assignment: ${e.message}", e)
            ""
        }
    }

    suspend fun updateStudentAssignment(studentAssignment: StudentAssignment): Int {
        Log.d(TAG, "updateStudentAssignment: Received studentAssignment.id: ${studentAssignment.id}")
        return try {
            if (studentAssignment.id.isNotBlank()) {
                // Added debug log to confirm invocation
                Log.d(TAG, "Attempting to update student assignment with ID: ${studentAssignment.id}")
                assignmentsCollection.document(studentAssignment.id).set(studentAssignment).await()
                // Changed to Log.d for success
                Log.d(TAG, "Student assignment updated with ID: ${studentAssignment.id}")
                1
            } else {
                Log.e(TAG, "Cannot update student assignment: ID is blank.")
                0
            }
        } catch (e: Exception) {
            // This catch block should ideally log "PERMISSION_DENIED" if it's a rules issue
            Log.e(TAG, "Error updating student assignment: ${e.message}", e)
            0
        }
    }

    suspend fun getStudentAssignmentById(assignmentId: String): StudentAssignment? {
        return try {
            val document = assignmentsCollection.document(assignmentId).get().await()
            document.toObject(StudentAssignment::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student assignment by ID '$assignmentId': ${e.message}", e)
            null
        }
    }

    fun getStudentAssignmentsByHomework(homeworkId: String): Flow<List<StudentAssignment>> = callbackFlow {
        // Changed to Log.d for clarity
        Log.d(TAG, "Querying student assignments for homeworkId: $homeworkId")
        try {
            val subscription = assignmentsCollection.whereEqualTo("homeworkId", homeworkId).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for student assignments by homework ID '$homeworkId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val assignments = snapshot.toObjects(StudentAssignment::class.java)
                        // Changed to Log.d for clarity
                        Log.d(TAG, "Fetched ${assignments.size} student assignments for homeworkId: $homeworkId")
                        trySend(assignments).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to StudentAssignment list for homework ID '$homeworkId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    // Changed to Log.d for clarity
                    Log.d(TAG, "No student assignments found for homework ID '$homeworkId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up student assignments by homework listener: ${e.message}", e)
            close(e)
        }
    }

    fun getStudentAssignmentsByStudent(studentId: String): Flow<List<StudentAssignment>> = callbackFlow {
        // Changed to Log.d for clarity
        Log.d(TAG, "Querying student assignments for studentId: $studentId")
        try {
            val subscription = assignmentsCollection.whereEqualTo("studentId", studentId).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for student assignments by student ID '$studentId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val assignments = snapshot.toObjects(StudentAssignment::class.java)
                        // Changed to Log.d for clarity
                        Log.d(TAG, "Fetched ${assignments.size} student assignments for studentId: $studentId")
                        trySend(assignments).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to StudentAssignment list for student ID '$studentId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    // Changed to Log.d for clarity
                    Log.d(TAG, "No student assignments found for student ID '$studentId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up student assignments by student listener: ${e.message}", e)
            close(e)
        }
    }

    fun getAllStudentAssignments(): Flow<List<StudentAssignment>> = callbackFlow {
        // Changed to Log.d for clarity
        Log.d(TAG, "Querying all student assignments")
        try {
            val subscription = assignmentsCollection.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for all student assignments failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val assignments = snapshot.toObjects(StudentAssignment::class.java)
                        // Changed to Log.d for clarity
                        Log.d(TAG, "Fetched ${assignments.size} total student assignments.")
                        trySend(assignments).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to StudentAssignment list: ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    // Changed to Log.d for clarity
                    Log.d(TAG, "No student assignments found or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up all student assignments listener: ${e.message}", e)
            close(e)
        }
    }

    suspend fun deleteStudentAssignmentById(studentAssignmentId: String): Int {
        return try {
            assignmentsCollection.document(studentAssignmentId).delete().await()
            // Changed to Log.d for clarity
            Log.d(TAG, "Student assignment deleted with ID: $studentAssignmentId")
            1
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting student assignment by ID: ${e.message}", e)
            0
        }
    }

    suspend fun getStudentAssignment(homeworkId: String, studentId: String): StudentAssignment? {
        return try {
            val snapshot = assignmentsCollection
                .whereEqualTo("homeworkId", homeworkId)
                .whereEqualTo("studentId", studentId)
                .limit(1)
                .get()
                .await()
            val assignment = snapshot.documents.firstOrNull()?.toObject(StudentAssignment::class.java)
            if (assignment != null) {
                // Changed to Log.d for clarity
                Log.d(TAG, "Found student assignment for homework '$homeworkId' and student '$studentId'.")
            } else {
                // Changed to Log.d for clarity
                Log.d(TAG, "No student assignment found for homework '$homeworkId' and student '$studentId'.")
            }
            assignment
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student assignment for homework '$homeworkId' and student '$studentId': ${e.message}", e)
            null
        }
    }
}