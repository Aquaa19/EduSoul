// File: main/java/com/aquaa/edusoul/viewmodels/ParentSyllabusViewModel.kt
package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap
import com.aquaa.edusoul.models.*
import com.aquaa.edusoul.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull // Keep this import
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ParentSyllabusVM"

class ParentSyllabusViewModel(
    private val studentRepository: StudentRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val syllabusProgressRepository: SyllabusProgressRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _parentUserId = MutableLiveData<String>()
    private val _selectedChildId = MutableLiveData<String>()

    val childrenOfParent: LiveData<List<Student>> = _parentUserId.switchMap { parentId ->
        if (parentId.isNotBlank()) {
            // FIX: Corrected method name from getStudentsByParentId to getStudentsByParent
            studentRepository.getStudentsByParent(parentId).asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList())
        }
    }

    val syllabusProgressDetails: LiveData<List<ParentSyllabusProgressDetails>> =
        _selectedChildId.switchMap { studentId ->
            liveData(Dispatchers.IO) {
                if (studentId.isNotBlank()) {
                    val currentStudent = studentRepository.getStudentById(studentId)
                    if (currentStudent == null) {
                        Log.w(TAG, "Selected student with ID $studentId not found.")
                        emit(emptyList())
                        return@liveData
                    }

                    val enrolledBatches = batchRepository.getBatchesForStudent(studentId).firstOrNull() ?: emptyList()
                    val enrolledBatchIds = enrolledBatches.map { it.id }.toSet()

                    if (enrolledBatchIds.isEmpty()) {
                        emit(emptyList())
                        return@liveData
                    }

                    val allSubjectsMap = subjectRepository.getAllSubjects().firstOrNull()?.associateBy { it.id } ?: emptyMap()

                    val allProgress = syllabusProgressRepository.getAllSyllabusProgress().firstOrNull()?.filter { it.batchId in enrolledBatchIds } ?: emptyList()

                    val aggregatedProgressMap = allProgress.groupBy { it.syllabusTopicId }.mapValues { (_, progressList) ->
                        progressList.firstOrNull { progress -> progress.isCompleted } ?: progressList.first()
                    }

                    val relevantSubjectIds = enrolledBatches.flatMap { batch ->
                        allSubjectsMap.keys
                    }.toSet()

                    val relevantTopics = syllabusTopicRepository.getAllSyllabusTopics().firstOrNull()?.filter {
                        it.subjectId in relevantSubjectIds
                    } ?: emptyList()

                    val teacherMap = userRepository.getAllTeachers().firstOrNull()?.associateBy { it.id } ?: emptyMap()

                    val resultList = relevantTopics.map { topic ->
                        val progress = aggregatedProgressMap[topic.id]
                        val subject = allSubjectsMap[topic.subjectId]

                        ParentSyllabusProgressDetails(
                            studentId = currentStudent.id,
                            studentName = currentStudent.fullName,
                            syllabusTopicId = topic.id,
                            topicName = topic.topicName,
                            description = topic.description,
                            topicOrder = topic.order ?: 0,
                            syllabusProgressId = progress?.id ?: "",
                            isCompleted = progress?.isCompleted ?: false,
                            completionDate = progress?.completionDate,
                            remarks = progress?.remarks,
                            updatedByTeacherId = progress?.updatedByTeacherId ?: "",
                            teacherName = progress?.updatedByTeacherId?.let { teacherMap[it]?.fullName } ?: "N/A",
                            subjectId = subject?.id ?: "",
                            subjectName = subject?.subjectName ?: "Unknown Subject",
                            batchId = progress?.batchId ?: "",
                            batchName = enrolledBatches.find { it.id == progress?.batchId }?.batchName ?: "N/A"
                        )
                    }

                    emit(resultList.sortedWith(compareBy({ it.subjectName }, { it.topicOrder })))
                } else {
                    emit(emptyList())
                }
            }
        }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setParentUserId(userId: String) {
        _parentUserId.value = userId
    }

    fun selectChild(studentId: String) {
        _selectedChildId.value = studentId
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}