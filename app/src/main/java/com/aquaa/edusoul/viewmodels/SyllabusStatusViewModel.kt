package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.liveData
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.SyllabusTopicStatusAdmin
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.repositories.SyllabusProgressRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine // Correct import for combine
import kotlinx.coroutines.flow.firstOrNull // Keep for one-time fetches where appropriate

class SyllabusStatusViewModel(
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val syllabusProgressRepository: SyllabusProgressRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // LiveData for selected filter criteria
    private val _selectedBatchId = MutableLiveData<String?>()
    private val _selectedSubjectId = MutableLiveData<String?>()

    // MediatorLiveData to combine the two filters
    private val _filters = MediatorLiveData<Pair<String?, String?>>().apply {
        var batchId: String? = null
        var subjectId: String? = null

        val updater = {
            val bId = batchId
            val sId = subjectId
            if (bId != null && sId != null) {
                this.value = Pair(bId, sId)
            }
        }

        addSource(_selectedBatchId) {
            batchId = it
            updater()
        }
        addSource(_selectedSubjectId) {
            subjectId = it
            updater()
        }
    }


    // LiveData for all batches for the spinner
    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData(Dispatchers.IO)

    // LiveData for all subjects for the spinner
    val allSubjects: LiveData<List<Subject>> = subjectRepository.getAllSubjects().asLiveData(Dispatchers.IO)

    // LiveData for the syllabus status report, triggered by changes in the combined filters
    val syllabusStatusReport: LiveData<List<SyllabusTopicStatusAdmin>> =
        _filters.switchMap { (batchId, subjectId) ->
            if (batchId != null && batchId.isNotBlank() && subjectId != null && subjectId.isNotBlank()) {
                // Use combine to react to real-time updates from all three flows
                combine(
                    syllabusTopicRepository.getTopicsForSubject(subjectId),
                    syllabusProgressRepository.getSyllabusProgressForBatch(batchId),
                    userRepository.getAllTeachers() // Include teachers for real-time name updates
                ) { topics, progresses, teachers ->
                    val progressMap = progresses.associateBy { it.syllabusTopicId }
                    val teachersMap = teachers.associateBy { it.id }

                    topics.map { topic ->
                        val progress = progressMap[topic.id]
                        val teacherName = progress?.updatedByTeacherId?.let { teachersMap[it]?.fullName }

                        SyllabusTopicStatusAdmin(
                            topicName = topic.topicName,
                            isCompleted = progress?.isCompleted ?: false,
                            completionDate = progress?.completionDate,
                            teacherName = teacherName
                        )
                    }.sortedBy { it.topicName }
                }.asLiveData(Dispatchers.IO) // Convert the combined Flow to LiveData
            } else {
                MutableLiveData(emptyList())
            }
        }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun selectBatch(batchId: String) {
        _selectedBatchId.value = batchId
    }

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
    }
}
