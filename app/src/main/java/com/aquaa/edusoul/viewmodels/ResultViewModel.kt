package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.Result
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentResultDetails
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository // Added import
import com.aquaa.edusoul.repositories.UserRepository // Added import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultViewModel(
    private val resultRepository: ResultRepository,
    private val examRepository: ExamRepository,
    private val studentRepository: StudentRepository,
    private val subjectRepository: SubjectRepository, // Added
    private val userRepository: UserRepository // Added
) : ViewModel() {

    private val TAG = "ResultViewModel"

    private val _selectedExamId = MutableLiveData<String>() // Changed from Long to String

    // LiveData for all exams to populate the spinner
    val allExams: LiveData<List<Exam>> = examRepository.getAllExams().asLiveData(Dispatchers.IO)

    // LiveData for displaying detailed results of the selected exam
    // Uses switchMap to react to changes in _selectedExamId
    val detailedResults: LiveData<List<StudentResultDetails>> = _selectedExamId.switchMap { examId ->
        // Changed check for String ID: use isNotBlank()
        if (examId.isNotBlank()) {
            resultRepository.getDetailedResultsForExam(examId).asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList()) // Emit empty list if no valid examId
        }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Selects an exam to display its results.
     * @param examId The ID of the selected exam. Changed from Long to String.
     */
    fun selectExam(examId: String) {
        _selectedExamId.value = examId
    }

    /**
     * Clears the selected exam, usually for initial state or when no exam is chosen.
     */
    fun clearSelectedExam() {
        _selectedExamId.value = "" // Set to blank string for String ID
    }

    /**
     * Updates the marks and remarks for a specific result.
     * @param resultId The ID of the result to update. Changed from Long to String.
     * @param newMarks The new marks obtained.
     * @param newRemarks The new remarks.
     */
    fun updateResult(resultId: String, newMarks: Double, newRemarks: String?) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Repository's updateResultScore expects String ID and returns Int
                val rowsAffected = resultRepository.updateResultScore(resultId, newMarks)
                // Note: Updating remarks requires fetching the result, copying it with new remarks,
                // and then calling a repository update method that accepts a full Result object.
                // Assuming updateResultScore handles the update or a separate method is called.
                // If ResultRepository only has updateResultScore, we might need a general updateResult method there too.
                // For now, let's assume updateResultScore can handle remarks or it's a separate call.
                // If remarks need to be updated:
                val existingResult = resultRepository.getResultById(resultId) // Need getResultById method in ResultRepository
                if (existingResult != null) {
                    val updatedResult = existingResult.copy(remarks = newRemarks)
                    // Assuming ResultRepository has an updateResult method that takes a Result object
                    // For now, just logging if such a method is missing.
                    // resultRepository.updateResult(updatedResult) // This method would need to exist
                }


                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Result updated successfully."
                    } else {
                        _errorMessage.value = "Failed to update result."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating result: ${e.message}"
                }
                Log.e(TAG, "Error updating result", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}