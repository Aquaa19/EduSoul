package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData // Import asLiveData for Flow conversion
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap // Import switchMap for reactive LiveData
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentBatchLink
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.StudentBatchLinkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // For switching to Main thread for UI updates
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for managing student enrollments within a specific batch.
 * Interacts with StudentRepository and StudentBatchLinkRepository for data operations.
 */
class BatchEnrollmentViewModel(
    private val studentRepository: StudentRepository,
    private val studentBatchLinkRepository: StudentBatchLinkRepository
) : ViewModel() {

    // MutableLiveData to hold the ID of the currently selected batch.
    // Changed type from Long to String.
    private val _batchId = MutableLiveData<String>()
    val batchId: LiveData<String> get() = _batchId

    // LiveData for the list of students currently enrolled in the selected batch.
    // Uses switchMap to react to changes in _batchId and fetch data from StudentRepository.
    val enrolledStudents: LiveData<List<Student>> = _batchId.switchMap { id ->
        // Changed comparison for String ID: check if not blank
        if (id.isNotBlank()) studentRepository.getStudentsByBatch(id).asLiveData(Dispatchers.IO)
        else MutableLiveData(emptyList()) // Emit empty list if no valid batchId
    }

    // LiveData for the list of students available for enrollment in the selected batch.
    // Uses switchMap to react to changes in _batchId and fetch data from StudentRepository.
    val availableStudents: LiveData<List<Student>> = _batchId.switchMap { id ->
        // Changed comparison for String ID: check if not blank
        if (id.isNotBlank()) studentRepository.getAvailableStudentsForBatch(id).asLiveData(Dispatchers.IO)
        else MutableLiveData(emptyList()) // Emit empty list if no valid batchId
    }

    // Loading state indicator for UI feedback (e.g., showing a progress spinner).
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Error/Success message LiveData for displaying Toasts or other UI feedback to the user.
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    /**
     * Sets the batch ID for which enrollment data should be loaded.
     * Calling this will automatically trigger data loading via the switchMap operators.
     * @param id The ID of the batch. Changed parameter type from Long to String.
     */
    fun setBatchId(id: String) {
        _batchId.value = id
    }

    /**
     * Clears the current error/success message, typically after it has been displayed to the user.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Attempts to enroll a student into a specific batch.
     * Performs a check to prevent duplicate enrollments before attempting insertion.
     * @param studentId The ID of the student to enroll. Changed parameter type from Long to String.
     * @param batchId The ID of the batch to enroll into. Changed parameter type from Long to String.
     * @param studentFullName The full name of the student, used for user feedback messages.
     */
    fun enrollStudent(studentId: String, batchId: String, studentFullName: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Log the IDs right before checking for existing links and inserting
                Log.d("BatchEnrollmentVM", "Attempting to enroll studentId: $studentId into batchId: $batchId")

                // Check if the student is already linked to this batch.
                // Repository's countStudentBatchLinks now expects String IDs.
                if (studentBatchLinkRepository.countStudentBatchLinks(studentId, batchId) > 0) {
                    withContext(Dispatchers.Main) { // Switch to Main thread for UI updates
                        _errorMessage.value = "Student '$studentFullName' is already enrolled in this batch."
                    }
                    Log.d("BatchEnrollmentVM", "Student '$studentFullName' already enrolled in batch '$batchId'. Skipping enrollment.")
                    return@launch // Exit the coroutine as no further action is needed
                }

                // Create a new StudentBatchLink object
                val newLink = StudentBatchLink(
                    id = "", // Firebase will generate a String ID
                    studentId = studentId,
                    batchId = batchId,
                    enrollmentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()), // Current date as enrollment date
                    status = "Enrolled" // Default status
                )
                // Insert the new link into the database via the repository.
                // Repository's insertStudentBatchLink now returns String.
                val newRowId = studentBatchLinkRepository.insertStudentBatchLink(newLink)

                // Check if the insertion was successful (Firestore ID is not blank)
                if (newRowId.isNotBlank()) {
                    withContext(Dispatchers.Main) { // Show Toast on main thread
                        _errorMessage.value = "Student '$studentFullName' enrolled successfully!"
                    }
                    Log.d("BatchEnrollmentVM", "Successfully enrolled student '$studentFullName' into batch '$batchId'. New link ID: $newRowId")
                } else {
                    // This else block would typically be hit if insert was ignored, meaning a duplicate was attempted.
                    _errorMessage.postValue("Failed to enroll student '$studentFullName'.")
                    Log.e("BatchEnrollmentVM", "Failed to insert student batch link for student '$studentFullName' and batch '$batchId'. Insert returned blank ID.")
                }
            } catch (e: Exception) {
                // Log and provide a user-friendly error message if an exception occurs
                Log.e("BatchEnrollmentVM", "Error enrolling student: ${e.message}", e)
                _errorMessage.postValue("Error enrolling student '$studentFullName': ${e.message}")
            } finally {
                _isLoading.postValue(false) // Always set loading to false when operation finishes
            }
        }
    }

    /**
     * Attempts to unenroll a student from a specific batch.
     * @param studentId The ID of the student to unenroll. Changed parameter type from Long to String.
     * @param batchId The ID of the batch to unenroll from. Changed parameter type from Long to String.
     * @param studentFullName The full name of the student, used for user feedback messages.
     */
    fun unenrollStudent(studentId: String, batchId: String, studentFullName: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Delete the student-batch link from the database via the repository.
                // Repository's deleteStudentBatchLink now expects String IDs and returns Int.
                val rowsDeleted = studentBatchLinkRepository.deleteStudentBatchLink(studentId, batchId)

                if (rowsDeleted > 0) { // Check if any rows were actually deleted
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "Student '$studentFullName' unenrolled successfully."
                    }
                    Log.d("BatchEnrollmentVM", "Successfully unenrolled student '$studentFullName' from batch '$batchId'.")
                } else {
                    _errorMessage.postValue("Failed to unenroll student '$studentFullName'.")
                    Log.w("BatchEnrollmentVM", "Failed to unenroll student '$studentFullName' from batch '$batchId'. No link found or deleted.")
                }
            } catch (e: Exception) {
                // Log and provide a user-friendly error message if an exception occurs
                Log.e("BatchEnrollmentVM", "Error unenrolling student: ${e.message}", e)
                _errorMessage.postValue("Error unenrolling student '$studentFullName': ${e.message}")
            } finally {
                _isLoading.postValue(false) // Always set loading to false when operation finishes
            }
        }
    }
}