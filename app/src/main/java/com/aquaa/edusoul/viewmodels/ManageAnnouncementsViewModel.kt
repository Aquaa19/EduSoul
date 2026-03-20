package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Announcement
import com.aquaa.edusoul.repositories.AnnouncementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageAnnouncementsViewModel(private val repository: AnnouncementRepository) : ViewModel() {

    // LiveData for all announcements to display in the RecyclerView
    val allAnnouncements: LiveData<List<Announcement>> = repository.getAllAnnouncements().asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Changed 'announcement: Announcement' type to reflect String IDs in model
    fun addAnnouncement(announcement: Announcement) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The repository's insert method correctly returns String
                val newRowId = repository.insertAnnouncement(announcement)
                withContext(Dispatchers.Main) {
                    if (newRowId.isNotBlank()) { // Check if String ID is not blank
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

    // Changed 'announcement: Announcement' type to reflect String IDs in model
    fun updateAnnouncement(announcement: Announcement) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The repository's update method correctly returns Int (rows affected)
                val rowsAffected = repository.updateAnnouncement(announcement)
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

    // Changed parameter type from Long to String
    fun deleteAnnouncement(announcementId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The repository's delete method correctly returns Int (rows affected)
                val rowsAffected = repository.deleteAnnouncementById(announcementId)
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