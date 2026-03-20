// File: main/java/com/aquaa/edusoul/viewmodels/EnterScoresViewModel.kt
package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.aquaa.edusoul.models.Exam
import com.aquaa.edusoul.models.Result
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentScoreStatus
import com.aquaa.edusoul.repositories.ExamRepository
import com.aquaa.edusoul.repositories.ResultRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EnterScoresViewModel(
    private val examRepository: ExamRepository,
    private val studentRepository: StudentRepository,
    private val resultRepository: ResultRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "EnterScoresViewModel"

    // Changed _currentTeacherId type from Long to String
    private val _currentTeacherId = MutableLiveData<String>()
    // Changed _selectedExamId type from Long? to String?
    private val _selectedExamId = MutableLiveData<String?>()

    val examsForTeacher: LiveData<List<Exam>> = _currentTeacherId.switchMap { teacherId ->
        // Check for blank String ID instead of > 0
        if (teacherId.isNotBlank()) {
            // Corrected method call from getExamsForTeacher to getExamsByTeacher
            examRepository.getExamsByTeacher(teacherId).asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList())
        }
    }

    val selectedExam: LiveData<Exam?> = _selectedExamId.switchMap { examId ->
        liveData(Dispatchers.IO) {
            // Check for non-null and non-blank String ID
            if (examId != null && examId.isNotBlank()) {
                // examRepository.getExamById already expects String
                emit(examRepository.getExamById(examId))
            } else {
                emit(null)
            }
        }
    }

    val studentScores: LiveData<List<StudentScoreStatus>> = _selectedExamId.switchMap { examId ->
        liveData(Dispatchers.IO) {
            // Check for non-null and non-blank String ID
            if (examId != null && examId.isNotBlank()) {
                // examRepository.getExamById already expects String
                val exam = examRepository.getExamById(examId)
                // Fix: Capture exam.batchId into a local immutable variable
                val currentBatchId = exam?.batchId
                if (currentBatchId != null && currentBatchId.isNotBlank()) { // Line 63 in original
                    // studentRepository.getStudentsByBatch expects String
                    studentRepository.getStudentsByBatch(currentBatchId) // Line 65 in original
                        // resultRepository.getResultsForExam expects String
                        .combine(resultRepository.getResultsForExam(examId)) { students, results ->
                            val existingResultsMap = results.associateBy { it.studentId }
                            students.map { student ->
                                val existingScore = existingResultsMap[student.id]?.marksObtained
                                if (existingScore != null) {
                                    StudentScoreStatus(student, existingScore)
                                } else {
                                    StudentScoreStatus(student, "")
                                }
                            }
                        }.collect { combinedList -> emit(combinedList) }
                } else {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
        }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // Changed teacherId parameter type from Long to String
    fun setCurrentTeacherId(teacherId: String) {
        _currentTeacherId.value = teacherId
    }

    // Changed examId parameter type from Long? to String?
    fun selectExam(examId: String?) {
        _selectedExamId.value = examId
    }

    // Changed examId and enteredByUserId parameter types from Long to String
    fun saveScores(examId: String, scoresData: List<StudentScoreStatus>, enteredByUserId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // examRepository.getExamById already expects String
                val exam = examRepository.getExamById(examId)
                if (exam == null) {
                    withContext(Dispatchers.Main) { _errorMessage.value = "Error: Exam not found." }
                    return@launch
                }

                val resultsToSave = scoresData.mapNotNull { status ->
                    val scoreString = status.score
                    if (scoreString.isNotBlank()) {
                        val marks = scoreString.toDoubleOrNull() // Use toDoubleOrNull() for safe parsing
                        if (marks == null) {
                            withContext(Dispatchers.Main) {
                                _errorMessage.value = "Invalid score format for ${status.student.fullName}."
                            }
                            return@launch // Exit if any score is invalid
                        }

                        if (marks < 0 || marks > exam.maxMarks) {
                            withContext(Dispatchers.Main) {
                                _errorMessage.value = "Invalid score for ${status.student.fullName}. Must be between 0 and ${exam.maxMarks}."
                            }
                            return@launch
                        }
                        Result(
                            studentId = status.student.id, // student.id is already String
                            examId = examId, // examId is now String
                            marksObtained = marks,
                            remarks = null,
                            enteredByUserId = enteredByUserId, // enteredByUserId is now String
                            entryTimestamp = null
                        )
                    } else {
                        null
                    }
                }

                if (resultsToSave.isEmpty()) {
                    withContext(Dispatchers.Main) { _errorMessage.value = "No scores entered to submit." }
                    return@launch
                }

                resultRepository.insertAll(resultsToSave)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "${resultsToSave.size} score(s) submitted successfully!"
                    _selectedExamId.value = _selectedExamId.value // Trigger a refresh of scores
                }
            } catch (e: NumberFormatException) {
                withContext(Dispatchers.Main) { _errorMessage.value = "Invalid score format entered." }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _errorMessage.value = "Error saving scores: ${e.message}" }
                Log.e(TAG, "Error saving scores", e)
            } finally {
                withContext(Dispatchers.Main) { _isLoading.value = false }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
