package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentBatchLink
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

class StudentRepository {

    private val db = FirebaseFirestore.getInstance()
    private val studentsCollection = db.collection("students")
    private val TAG = "StudentRepo"

    /**
     * Inserts a new student into Firestore.
     * @param student The Student object to insert.
     * @return The Firestore document ID of the newly created student, or an empty string if insertion fails.
     */
    suspend fun insertStudent(student: Student): String {
        return try {
            val documentReference = studentsCollection.add(student).await()
            Log.d(TAG, "Student inserted with ID: ${documentReference.id}")
            student.id = documentReference.id // This correctly updates the in-memory object
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting student: ${e.message}", e)
            ""
        }
    }

    /**
     * Updates an existing student in Firestore.
     * @param student The Student object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateStudent(student: Student): Int {
        return try {
            if (student.id.isNotBlank()) {
                studentsCollection.document(student.id).set(student).await()
                Log.d(TAG, "Student updated with ID: ${student.id}")
                1
            } else {
                Log.w(TAG, "Cannot update student: ID is blank.")
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating student: ${e.message}", e)
            0
        }
    }

    /**
     * Deletes a student from Firestore by their document ID,
     * and also deletes all associated student-batch links, attendance records, result records,
     * and student assignments for this student.
     * @param studentId The Firestore document ID of the student to delete.
     * @return The number of documents affected (1 for student + N for related data for success, 0 for failure).
     */
    suspend fun deleteStudentById(studentId: String): Int {
        return try {
            val firestoreBatch = db.batch()
            var deletedCount = 0

            // 1. Delete the student document itself
            val studentDocRef = studentsCollection.document(studentId)
            firestoreBatch.delete(studentDocRef)
            deletedCount++
            Log.d(TAG, "Scheduled deletion of student with ID: $studentId")

            // 2. Find and delete all associated StudentBatchLink documents
            val studentBatchLinksSnapshot = db.collection("student_enrollments")
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            for (document in studentBatchLinksSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of student enrollment link with ID: ${document.id} for student: $studentId")
            }

            // 3. Find and delete all associated Attendance documents
            val attendanceSnapshot = db.collection("attendance")
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            for (document in attendanceSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of attendance record with ID: ${document.id} for student: $studentId")
            }

            // 4. Find and delete all associated Result documents
            val resultsSnapshot = db.collection("results")
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            for (document in resultsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of result record with ID: ${document.id} for student: $studentId")
            }

            // 5. Find and delete all associated StudentAssignment documents
            val studentAssignmentsSnapshot = db.collection("student_assignments")
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            for (document in studentAssignmentsSnapshot.documents) {
                firestoreBatch.delete(document.reference)
                deletedCount++
                Log.d(TAG, "Scheduled deletion of student assignment with ID: ${document.id} for student: $studentId")
            }

            // Commit the batch write
            firestoreBatch.commit().await()
            Log.d(TAG, "Student and ${deletedCount - 1} associated data deleted successfully for ID: $studentId. Total documents deleted: $deletedCount")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting student by ID $studentId and its associated data: ${e.message}", e)
            0
        }
    }

    /**
     * Retrieves a specific student from Firestore by their document ID.
     * @param studentId The Firestore document ID of the student.
     * @return The Student object if found, null otherwise.
     */
    suspend fun getStudentById(studentId: String): Student? {
        return try {
            val document = studentsCollection.document(studentId).get().await()
            document.toObject(Student::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting student by ID '$studentId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves all students from Firestore in real-time.
     * Emits a list of Student objects as a Flow whenever the data changes.
     */
    fun getAllStudents(): Flow<List<Student>> = callbackFlow {
        val subscription = studentsCollection.orderBy("fullName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for all students failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val students = snapshot.toObjects(Student::class.java)
                        Log.d(TAG, "Fetched ${students.size} all students.")
                        trySend(students).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to Student list: ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    Log.d(TAG, "No students found or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves students assigned to a specific batch from Firestore in real-time.
     * This now listens to changes in studentBatchLinks and then fetches corresponding student details.
     * This function handles the Firestore 'in' query limit of 10 by chunking student IDs.
     * @param batchId The ID of the batch.
     * @return A Flow of list of Student objects.
     */
    fun getStudentsByBatch(batchId: String): Flow<List<Student>> = callbackFlow {
        Log.d(TAG, "Listening for students for batchId: $batchId")
        val studentBatchLinksCollection = db.collection("student_enrollments")

        val linksSubscription = studentBatchLinksCollection
            .whereEqualTo("batchId", batchId)
            .addSnapshotListener { linkSnapshot, linkError ->
                if (linkError != null) {
                    Log.e(TAG, "Listen for student batch links failed: ${linkError.message}", linkError)
                    close(linkError)
                    return@addSnapshotListener
                }

                launch {
                    if (linkSnapshot != null && !linkSnapshot.isEmpty) {
                        val studentLinks = linkSnapshot.toObjects(StudentBatchLink::class.java)
                        val studentIds = studentLinks.map { it.studentId }.filter { it.isNotBlank() }

                        if (studentIds.isEmpty()) {
                            Log.d(TAG, "No valid student IDs found for batch: $batchId")
                            trySend(emptyList()).isSuccess
                            return@launch
                        }

                        val allEnrolledStudents = mutableListOf<Student>()
                        // Firestore 'in' query limit is 10, so chunk the student IDs
                        studentIds.chunked(10).forEach { chunkedIds ->
                            try {
                                val studentsInChunk = studentsCollection
                                    .whereIn(FieldPath.documentId(), chunkedIds)
                                    .get()
                                    .await()
                                    .toObjects(Student::class.java)
                                allEnrolledStudents.addAll(studentsInChunk)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error fetching student details for a chunk in batch: ${e.message}", e)
                                // Continue to next chunk even if one fails
                            }
                        }
                        Log.d(TAG, "Fetched ${allEnrolledStudents.size} students for batch: $batchId via links (chunked queries).")
                        trySend(allEnrolledStudents).isSuccess
                    } else {
                        Log.d(TAG, "No student links found for batch: $batchId or link snapshot is empty.")
                        trySend(emptyList()).isSuccess
                    }
                }
            }
        awaitClose { linksSubscription.remove() }
    }


    /**
     * Retrieves students by their parent's user ID from Firestore in real-time.
     * @param parentUserId The ID of the parent user.
     * @return A Flow emitting a list of Student objects.
     */
    fun getStudentsByParent(parentUserId: String): Flow<List<Student>> = callbackFlow {
        val subscription = studentsCollection.whereEqualTo("parentUserId", parentUserId)
            .orderBy("fullName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for students by parent ID '$parentUserId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    try {
                        val students = snapshot.toObjects(Student::class.java)
                        Log.d(TAG, "Fetched ${students.size} students for parent ID '$parentUserId'.")
                        trySend(students).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to Student list for parent ID '$parentUserId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    Log.d(TAG, "No students found for parent ID '$parentUserId' or snapshot is empty.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves a Flow of students who are NOT yet assigned to a specific batch.
     * Combines all students and students already in the batch, then filters.
     * @param batchId The ID of the batch.
     * @return A Flow emitting a list of Student objects available for enrollment.
     */
    fun getAvailableStudentsForBatch(batchId: String): Flow<List<Student>> {
        return combine(
            getAllStudents(),
            getStudentsByBatch(batchId) // This now correctly handles more than 10 enrolled students
        ) { allStudents, enrolledStudents ->
            val enrolledStudentIds = enrolledStudents.map { it.id }.toSet()
            allStudents.filter { student ->
                !enrolledStudentIds.contains(student.id)
            }
        }
    }

    /**
     * Searches for students by their full name in real-time.
     * Performs a prefix search, with a client-side filter for better "contains" and case-insensitivity.
     * @param query The search string.
     * @return A Flow emitting a list of matching Student objects.
     */
    fun searchStudents(query: String): Flow<List<Student>> = callbackFlow {
        val queryLower = query.toLowerCase(Locale.ROOT)
        var firebaseQuery: Query = studentsCollection.orderBy("fullName")

        if (query.isNotBlank()) {
            firebaseQuery = studentsCollection
                .orderBy("fullName")
                .startAt(query)
                .endAt(query + "\uf8ff")
        }

        val subscription = firebaseQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for student search failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val students = snapshot.toObjects(Student::class.java)
                    val filteredStudents = if (query.isNotBlank()) {
                        students.filter { student ->
                            student.fullName.toLowerCase(Locale.ROOT).contains(queryLower)
                        }
                    } else {
                        students
                    }
                    Log.d(TAG, "Fetched ${filteredStudents.size} students for query '$query'.")
                    trySend(filteredStudents).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Student list during search: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                Log.d(TAG, "No students found for query '$query' or snapshot is empty.")
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }
}
