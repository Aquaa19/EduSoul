package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Announcement
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User // Assuming User model is in this package
import com.aquaa.edusoul.repositories.AnnouncementRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.isNullOrBlank // Import isNullOrBlank

class TeacherAnnouncementsManagementViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val userRepository: UserRepository,
    private val teacherSubjectBatchLinkRepository: TeacherSubjectBatchLinkRepository
) : ViewModel() {

    private val _currentTeacherId = MutableLiveData<String>()

    // Flow representing batches for the current teacher
    private val batchesForTeacherFlow: Flow<List<Batch>> = _currentTeacherId.asFlow().flatMapLatest { teacherId ->
        if (teacherId.isBlank()) {
            flowOf(emptyList())
        } else {
            batchRepository.getBatchesForTeacher(teacherId)
        }
    }

    // Flow representing subjects for the current teacher
    private val subjectsForTeacherFlow: Flow<List<Subject>> = _currentTeacherId.asFlow().flatMapLatest { teacherId ->
        if (teacherId.isBlank()) {
            flowOf(emptyList())
        } else {
            teacherSubjectBatchLinkRepository.getTeacherSubjectBatchLinksForTeacher(teacherId)
                .flatMapLatest { links ->
                    if (links.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        subjectRepository.getAllSubjects().map { allSubjects ->
                            val assignedSubjectIds = links.map { it.subjectId }.toSet()
                            allSubjects.filter { it.id in assignedSubjectIds }
                        }
                    }
                }
        }
    }

    val announcementsForTeacher: LiveData<List<Announcement>> = _currentTeacherId.switchMap { teacherId ->
        if (teacherId.isBlank()) {
            MutableLiveData(emptyList())
        } else {
            combine(
                announcementRepository.getAnnouncementsByAudience(Announcement.AUDIENCE_ALL),
                announcementRepository.getAnnouncementsByAudience(Announcement.AUDIENCE_TEACHERS),
                batchesForTeacherFlow.flatMapLatest { batches -> // Use the Flow version here
                    if (batches.isEmpty()) flowOf(emptyList<Announcement>()) // Specify type for emptyList
                    else {
                        val batchAnnouncementFlows = batches.map { batch ->
                            announcementRepository.getAnnouncementsByBatch(batch.id)
                        }
                        if (batchAnnouncementFlows.isEmpty()) flowOf(emptyList<Announcement>()) // Handle empty list of flows
                        else combine(batchAnnouncementFlows) { arrayOfLists ->
                            arrayOfLists.flatMap { it }
                        }
                    }
                },
                subjectsForTeacherFlow.flatMapLatest { subjects -> // Use the Flow version here
                    if (subjects.isEmpty()) flowOf(emptyList<Announcement>()) // Specify type for emptyList
                    else {
                        val subjectAnnouncementFlows = subjects.map { subject ->
                            announcementRepository.getAnnouncementsBySubject(subject.id)
                        }
                        if (subjectAnnouncementFlows.isEmpty()) flowOf(emptyList<Announcement>()) // Handle empty list of flows
                        else combine(subjectAnnouncementFlows) { arrayOfLists ->
                            arrayOfLists.flatMap { it }
                        }
                    }
                },
                userRepository.getAllUsers()
            ) { allAudienceAnnouncements, teachersAudienceAnnouncements, batchSpecificAnnouncements, subjectSpecificAnnouncements, allUsers ->
                val combinedAnnouncements = mutableListOf<Announcement>()
                combinedAnnouncements.addAll(allAudienceAnnouncements)
                combinedAnnouncements.addAll(teachersAudienceAnnouncements)
                combinedAnnouncements.addAll(batchSpecificAnnouncements)
                combinedAnnouncements.addAll(subjectSpecificAnnouncements)

                val authorMap = allUsers.associateBy { it.id }

                combinedAnnouncements
                    .distinctBy { it.id }
                    .map { announcement ->
                        announcement.copy(authorName = authorMap[announcement.authorUserId]?.fullName ?: "Unknown")
                    }
                    .sortedByDescending { it.publishDate }
            }.asLiveData(Dispatchers.IO)
        }
    }

    // LiveData exposed to the UI, derived from the Flow
    val batchesForTeacher: LiveData<List<Batch>> = batchesForTeacherFlow.asLiveData(Dispatchers.IO)

    // LiveData exposed to the UI, derived from the Flow
    val subjectsForTeacher: LiveData<List<Subject>> = subjectsForTeacherFlow.asLiveData(Dispatchers.IO)


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setCurrentTeacherId(teacherId: String) {
        if (_currentTeacherId.value != teacherId) { // Avoid unnecessary updates
            _currentTeacherId.value = teacherId
        }
    }

    // This method is no longer needed for fetching announcements as combine handles it
    // but might be used by the dialog for setting initial audience.
    // Keeping it for now but note its role is diminished for fetching.
    fun setAnnouncementFilter(audience: String, targetId: String?) {
        // The combined flow in announcementsForTeacher will fetch all relevant announcements.
        // This function can still be used if you want to implement additional client-side filtering
        // based on these parameters, but it won't drive the primary data fetch anymore.
    }

    fun addAnnouncement(announcement: Announcement) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure authorUserId is set if it's derived from currentTeacherId
                val announcementToInsert = if (announcement.authorUserId.isNullOrBlank() && !_currentTeacherId.value.isNullOrBlank()) {
                    announcement.copy(authorUserId = _currentTeacherId.value!!)
                } else {
                    announcement
                }
                val newRowId = announcementRepository.insertAnnouncement(announcementToInsert)
                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) { // Assuming isNotBlank indicates success for your ID
                        _errorMessage.value = "Announcement published successfully."
                    } else {
                        _errorMessage.value = "Failed to publish announcement."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error publishing announcement: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateAnnouncement(announcement: Announcement) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = announcementRepository.updateAnnouncement(announcement)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Announcement updated successfully."
                    } else {
                        _errorMessage.value = "Failed to update announcement."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating announcement: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteAnnouncement(announcementId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = announcementRepository.deleteAnnouncementById(announcementId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Announcement deleted."
                    } else {
                        _errorMessage.value = "Failed to delete announcement."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting announcement: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}