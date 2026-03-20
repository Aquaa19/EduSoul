package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.Result
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentResultDetails
import com.aquaa.edusoul.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class ResultRepository {

    private val db = FirebaseFirestore.getInstance()
    private val resultsCollection = db.collection("results")
    private val examsCollection = db.collection("exams") // Needed for exam details
    private val studentsCollection = db.collection("students") // Needed for student details
    private val usersCollection = db.collection("users") // Needed for teacher details
    private val TAG = "ResultRepository"

    /**
     * Retrieves all exams from Firestore in real-time.
     * Emits a list of Exam objects as a Flow whenever the data changes.
     */
    fun getAllExams(): Flow<List<Exam>> = callbackFlow {
        val subscription = examsCollection.orderBy("examDate", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all exams failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val exams = snapshot.toObjects(Exam::class.java)
                    trySend(exams).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Exam list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves results for a specific exam from Firestore in real-time.
     * Emits a list of Result objects as a Flow whenever the data changes.
     * @param examId The ID of the exam.
     */
    fun getResultsForExam(examId: String): Flow<List<Result>> = callbackFlow {
        val subscription = resultsCollection.whereEqualTo("examId", examId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for results for exam '$examId' failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val results = snapshot.toObjects(Result::class.java)
                    trySend(results).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Result list for exam '$examId': ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves results for a specific student from Firestore in real-time.
     * Emits a list of Result objects as a Flow whenever the data changes.
     * @param studentId The ID of the student.
     */
    fun getResultsForStudent(studentId: String): Flow<List<Result>> = callbackFlow {
        val subscription = resultsCollection.whereEqualTo("studentId", studentId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for results for student '$studentId' failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val results = snapshot.toObjects(Result::class.java)
                    trySend(results).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Result list for student '$studentId': ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Inserts a list of results into Firestore using a batch write.
     * This is more efficient for multiple insertions.
     * @param results A list of Result objects to insert.
     */
    suspend fun insertAll(results: List<Result>) {
        val batch = db.batch()
        try {
            for (result in results) {
                val docRef = resultsCollection.document()
                result.id = docRef.id // Corrected: Update the ID of the passed object
                batch.set(docRef, result)
            }
            batch.commit().await()
            Log.d(TAG, "Successfully inserted ${results.size} results.")
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting multiple results: ${e.message}", e)
        }
    }

    /**
     * Updates the score of a specific result in Firestore.
     * @param resultId The Firestore document ID of the result to update.
     * @param newScore The new score to set.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateResultScore(resultId: String, newScore: Double): Int {
        return try {
            resultsCollection.document(resultId).update("marksObtained", newScore).await()
            Log.d(TAG, "Result score updated for ID: $resultId")
            1 // Return 1 for success, similar to Room's update
        } catch (e: Exception) {
            Log.e(TAG, "Error updating result score for ID '$resultId': ${e.message}", e)
            0 // Return 0 for failure
        }
    }

    /**
     * Retrieves a specific result from Firestore by its document ID.
     * @param resultId The Firestore document ID of the result.
     * @return The Result object if found, null otherwise.
     */
    suspend fun getResultById(resultId: String): Result? {
        return try {
            val document = resultsCollection.document(resultId).get().await()
            document.toObject(Result::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting result by ID '$resultId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves detailed results for a specific exam by combining data from multiple collections in real-time.
     * This operation is performed client-side as Firestore does not support direct joins.
     * Emits a list of StudentResultDetails objects as a Flow whenever the data changes.
     * @param examId The ID of the exam.
     */
    fun getDetailedResultsForExam(examId: String): Flow<List<StudentResultDetails>> = callbackFlow {
        val examDocRef = examsCollection.document(examId)
        val resultsQuery = resultsCollection.whereEqualTo("examId", examId)

        // Listen to both exam and results changes
        val examListener = examDocRef.addSnapshotListener { examSnapshot, examError ->
            if (examError != null) {
                Log.e(TAG, "Listen for exam '$examId' failed: ${examError.message}", examError)
                close(examError)
                return@addSnapshotListener
            }

            val exam = examSnapshot?.toObject(Exam::class.java)
            if (exam == null) {
                trySend(emptyList()).isSuccess
                return@addSnapshotListener
            }

            resultsQuery.addSnapshotListener { resultsSnapshot, resultsError ->
                if (resultsError != null) {
                    Log.e(TAG, "Listen for results for exam '$examId' failed: ${resultsError.message}", resultsError)
                    close(resultsError)
                    return@addSnapshotListener
                }

                val results = resultsSnapshot?.toObjects(Result::class.java) ?: emptyList()

                // Fetch related student and teacher data
                val studentIds = results.map { it.studentId }.distinct()
                val teacherIds = results.mapNotNull { it.enteredByUserId }.distinct()

                // Perform one-time fetches for students and teachers
                db.runTransaction { transaction ->
                    val studentsMap = mutableMapOf<String, Student>()
                    val teachersMap = mutableMapOf<String, User>()

                    if (studentIds.isNotEmpty()) {
                        studentIds.chunked(10).forEach { chunk -> // Handle Firestore `in` query limit
                            val studentDocs = studentsCollection.whereIn("id", chunk).get().result
                            studentDocs?.toObjects(Student::class.java)?.forEach { student ->
                                studentsMap[student.id] = student
                            }
                        }
                    }

                    if (teacherIds.isNotEmpty()) {
                        teacherIds.chunked(10).forEach { chunk -> // Handle Firestore `in` query limit
                            val teacherDocs = usersCollection.whereIn("id", chunk).get().result
                            teacherDocs?.toObjects(User::class.java)?.forEach { user ->
                                teachersMap[user.id] = user
                            }
                        }
                    }

                    val detailedResults = results.map { result ->
                        val student = studentsMap[result.studentId]
                        val teacher = teachersMap[result.enteredByUserId]
                        StudentResultDetails(
                            resultId = result.id,
                            studentName = student?.fullName ?: "Unknown Student",
                            admissionNumber = student?.admissionNumber ?: "N/A",
                            marksObtained = result.marksObtained,
                            maxMarks = exam.maxMarks,
                            teacherName = teacher?.fullName ?: "N/A",
                            examId = exam.id,
                            resultRemarks = result.remarks
                        )
                    }.sortedBy { it.studentName }

                    trySend(detailedResults).isSuccess
                    null // Return null for transaction
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Transaction failed for detailed results: ${e.message}", e)
                    close(e)
                }
            }
        }

        awaitClose {
            examListener.remove()
            // The results listener is nested, so it will be implicitly removed when examListener is removed if it's the only path.
            // If it were a separate top-level listener, it would need its own removal.
        }
    }
}