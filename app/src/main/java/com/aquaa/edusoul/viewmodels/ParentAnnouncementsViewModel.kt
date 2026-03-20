package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.liveData
import com.aquaa.edusoul.models.* // Import all necessary models
import com.aquaa.edusoul.repositories.* // Import all necessary repositories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull // Keep this import
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ParentAnnouncementsViewModel(
    private val announcementRepository: AnnouncementRepository,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val batchRepository: BatchRepository
) : ViewModel() {

    private val TAG = "ParentAnnouncementsVM"

    private val _parentUserId = MutableLiveData<String>()

    val announcements: LiveData<List<Announcement>> = _parentUserId.switchMap { parentId ->
        liveData(Dispatchers.IO) {
            if (parentId.isBlank()) {
                emit(emptyList())
                return@liveData
            }

            try {
                // FIX: Corrected method name from getStudentsByParentId to getStudentsByParent
                // .firstOrNull() is correctly used here to get the List from the Flow.
                val children = studentRepository.getStudentsByParent(parentId).firstOrNull() ?: emptyList()
                val childBatchIds = mutableSetOf<String>()

                // The 'iterator()' ambiguity and 'Not enough information' errors resolve
                // once 'children' is correctly typed as List<Student>.
                for (child in children) {
                    val batchesForChild = batchRepository.getBatchesForStudent(child.id).firstOrNull() ?: emptyList()
                    childBatchIds.addAll(batchesForChild.map { it.id })
                }

                val allAnnouncements = announcementRepository.getAllAnnouncements().firstOrNull() ?: emptyList()

                // Fetch all users to get author names in one go if needed
                val allUsersMap = userRepository.getAllUsers().firstOrNull()?.associateBy { it.id } ?: emptyMap()

                val relevantAnnouncements = allAnnouncements.filter { announcement ->
                    val isGeneral = announcement.targetAudience == Announcement.AUDIENCE_ALL ||
                            announcement.targetAudience == Announcement.AUDIENCE_PARENTS

                    val isBatchSpecific = announcement.targetAudience == Announcement.AUDIENCE_BATCH &&
                            announcement.batchId != null && announcement.batchId in childBatchIds

                    isGeneral || isBatchSpecific
                }.map { announcement ->
                    // Populate the 'authorName' field that was added to the Announcement model
                    val authorUser = announcement.authorUserId?.let { allUsersMap[it] }
                    announcement.copy(authorName = authorUser?.fullName ?: "Unknown")
                }

                emit(relevantAnnouncements.sortedByDescending { it.publishDate })

            } catch (e: Exception) {
                Log.e(TAG, "Error loading parent announcements: ${e.message}", e)
                emit(emptyList())
            }
        }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun loadAnnouncementsForParent(parentId: String) {
        _parentUserId.value = parentId
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}