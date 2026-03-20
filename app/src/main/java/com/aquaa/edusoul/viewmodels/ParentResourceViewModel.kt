package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.LearningResource
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.LearningResourceRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class ParentResourceViewModel(
    private val studentRepository: StudentRepository,
    private val learningResourceRepository: LearningResourceRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository
) : ViewModel() {

    private val TAG = "ParentResourceVM"

    private val _parentUserId = MutableLiveData<String>()

    // Triple stores (studentId: String?, subjectId: String?, batchId: String?)
    private val _filterCriteria = MutableLiveData<Triple<String?, String?, String?>>()

    val childrenOfParent: LiveData<List<Student>> = _parentUserId.switchMap { parentId ->
        if (parentId.isNotBlank()) {
            // FIX: Corrected method name from getStudentsByParentId to getStudentsByParent
            studentRepository.getStudentsByParent(parentId).asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList())
        }
    }

    val availableSubjects: LiveData<List<Subject>> = subjectRepository.getAllSubjects().asLiveData(Dispatchers.IO)

    val availableBatchesForStudent: LiveData<List<Batch>> = _filterCriteria.map { it.first }.switchMap { studentId ->
        if (studentId != null && studentId.isNotBlank()) {
            batchRepository.getBatchesForStudent(studentId).asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList())
        }
    }

    val filteredResources: LiveData<List<LearningResource>> =
        _filterCriteria.switchMap { (studentId, subjectId, batchId) ->
            liveData(Dispatchers.IO) {
                val effectiveBatchId = if (studentId != null && studentId.isNotBlank() && batchId == null) {
                    val studentBatches = batchRepository.getBatchesForStudent(studentId).firstOrNull()
                    studentBatches?.firstOrNull()?.id
                } else {
                    batchId
                }

                learningResourceRepository.getAllLearningResources().collect { allResources ->
                    val filteredList = allResources.filter { resource ->
                        val matchSubject = subjectId.isNullOrBlank() || resource.subjectId == subjectId
                        val matchBatch = effectiveBatchId.isNullOrBlank() || resource.batchId == effectiveBatchId
                        matchSubject && matchBatch
                    }
                    emit(filteredList)
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

    fun selectChildAndBatch(studentId: String?, batchId: String?) {
        val currentSubjectFilter = _filterCriteria.value?.second
        _filterCriteria.value = Triple(studentId, currentSubjectFilter, batchId)
    }

    fun applyFilters(studentId: String?, subjectId: String?, batchId: String?) {
        _filterCriteria.value = Triple(studentId, subjectId, batchId)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}