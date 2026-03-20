// main/java/com/aquaa/edusoul/viewmodels/HomeworkViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Homework
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeworkViewModel(
    private val homeworkRepository: HomeworkRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    // Changed _teacherId type from Long to String
    private val _teacherId = MutableLiveData<String>()

    val homeworksForTeacher: LiveData<List<Homework>> = _teacherId.switchMap { teacherId ->
        // homeworkRepository.getHomeworksByTeacher expects String
        homeworkRepository.getHomeworksByTeacher(teacherId).asLiveData(Dispatchers.IO)
    }

    val allSubjects: LiveData<List<Subject>> = subjectRepository.getAllSubjects().asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setTeacherId(teacherId: String) { // Changed teacherId parameter type from Long to String
        if (_teacherId.value != teacherId) {
            _teacherId.value = teacherId
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun addHomework(homework: Homework) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                homeworkRepository.insertHomework(homework)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Homework added successfully."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error adding homework: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateHomework(homework: Homework) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                homeworkRepository.updateHomework(homework)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Homework updated successfully."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating homework: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteHomework(homeworkId: String) { // Changed homeworkId parameter type from Long to String
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                homeworkRepository.deleteHomeworkById(homeworkId)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Homework deleted."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting homework: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}