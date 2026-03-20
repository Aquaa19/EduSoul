package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.aquaa.edusoul.models.*
import com.aquaa.edusoul.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.DecimalFormat

class ParentPerformanceOverviewViewModel(
    private val studentRepository: StudentRepository,
    private val resultRepository: ResultRepository,
    private val examRepository: ExamRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val TAG = "ParentPerformanceVM"

    private val _parentUserId = MutableLiveData<String>()

    val parentExamResultDetailsList: LiveData<List<ParentExamResultDetails>> =
        _parentUserId.switchMap { parentId ->
            liveData(Dispatchers.IO) {
                if (parentId.isBlank()) {
                    emit(emptyList())
                    return@liveData
                }

                try {
                    val allParentExamResults = mutableListOf<ParentExamResultDetails>()
                    // FIX: Corrected method name from getStudentsByParentId to getStudentsByParent
                    // .firstOrNull() is correctly used here to get the List from the Flow.
                    val children = studentRepository.getStudentsByParent(parentId).firstOrNull() ?: emptyList()

                    // The 'iterator()' ambiguity and 'Not enough information' errors resolve
                    // once 'children' is correctly typed as List<Student>.
                    for (child in children) {
                        val resultsForChild = resultRepository.getResultsForStudent(child.id).firstOrNull() ?: emptyList()
                        for (result in resultsForChild) {
                            val exam = examRepository.getExamById(result.examId)
                            val subject = exam?.subjectId?.let { subjectRepository.getSubjectById(it) }

                            allParentExamResults.add(
                                ParentExamResultDetails(
                                    resultId = result.id,
                                    studentId = result.studentId,
                                    examId = result.examId,
                                    marksObtained = result.marksObtained,
                                    resultRemarks = result.remarks,
                                    examName = exam?.examName ?: "Unknown Exam",
                                    examDate = exam?.examDate ?: "N/A",
                                    examMaxMarks = exam?.maxMarks?.toDouble() ?: 0.0,
                                    examDescription = exam?.description,
                                    subjectId = subject?.id ?: "",
                                    subjectName = subject?.subjectName ?: "Unknown Subject"
                                )
                            )
                        }
                    }
                    emit(allParentExamResults)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching performance overview", e)
                    emit(emptyList())
                }
            }
        }

    val overallSummary: LiveData<String> = parentExamResultDetailsList.map { resultsList ->
        generateOverallSummary(resultsList)
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setParentUserId(userId: String) {
        _parentUserId.value = userId
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun generateOverallSummary(resultsList: List<ParentExamResultDetails>): String {
        if (resultsList.isEmpty()) {
            return "No graded exams to calculate overall performance."
        }

        var totalScore = 0.0
        var totalMaxScore = 0.0
        val gradedExams = resultsList.filter { it.marksObtained != null }

        if (gradedExams.isEmpty()) {
            return "No graded exams to calculate overall performance."
        }

        for (result in gradedExams) {
            totalScore += result.marksObtained!!
            totalMaxScore += result.examMaxMarks
        }

        return if (totalMaxScore > 0) {
            val overallPercentage = (totalScore / totalMaxScore) * 100
            val df = DecimalFormat("#.##")
            "Overall Performance: ${df.format(overallPercentage)}% from ${gradedExams.size} graded exams."
        } else {
            "Performance data available, but cannot calculate an overall percentage."
        }
    }
}