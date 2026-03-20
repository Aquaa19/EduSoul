// File: EduSoul/app/src/main/java/com/aquaa/edusoul/repositories/InstituteAttendanceRepository.kt
package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.InstituteAttendance
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

class InstituteAttendanceRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val INSTITUTE_ATTENDANCE_COLLECTION = "instituteAttendance"
    private val TAG = "InstituteAttendanceRepo"

    /**
     * Inserts a new institute attendance record into Firestore.
     * @param attendance The InstituteAttendance object to insert.
     * @return True if the operation was successful, false otherwise.
     */
    suspend fun insertInstituteAttendance(attendance: InstituteAttendance): Boolean {
        return try {
            if (attendance.id.isBlank()) {
                val documentRef = firestore.collection(INSTITUTE_ATTENDANCE_COLLECTION).document()
                attendance.id = documentRef.id
                documentRef.set(attendance).await()
            } else {
                firestore.collection(INSTITUTE_ATTENDANCE_COLLECTION).document(attendance.id).set(attendance).await()
            }
            Log.d(TAG, "Institute attendance record added/updated successfully: ${attendance.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding/updating institute attendance record", e)
            false
        }
    }

    /**
     * Retrieves institute attendance records for a specific student on a given date.
     * @param studentId The ID of the student.
     * @param date The timestamp (in milliseconds) for the date to query.
     * @return A list of InstituteAttendance objects, or an empty list if none found or an error occurs.
     */
    suspend fun getInstituteAttendanceForStudent(studentId: String, date: Long): List<InstituteAttendance> {
        return try {
            val startOfDay = getStartOfDay(date)
            val endOfDay = getEndOfDay(date)

            val querySnapshot = firestore.collection(INSTITUTE_ATTENDANCE_COLLECTION)
                .whereEqualTo("studentId", studentId)
                .whereGreaterThanOrEqualTo("markedAt", startOfDay)
                .whereLessThanOrEqualTo("markedAt", endOfDay)
                .get()
                .await()

            querySnapshot.documents.mapNotNull { it.toObject(InstituteAttendance::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting institute attendance for student $studentId on date $date", e)
            emptyList()
        }
    }

    /**
     * Helper function to get the start of the day (midnight) for a given timestamp.
     */
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Helper function to get the end of the day (just before midnight) for a given timestamp.
     */
    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    // You can add more CRUD operations here as needed, e.g., update, delete, get all for a day etc.
}