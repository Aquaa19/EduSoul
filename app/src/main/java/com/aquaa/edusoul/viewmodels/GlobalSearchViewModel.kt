// File: main/java/com/aquaa/edusoul/viewmodels/GlobalSearchViewModel.kt
package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.SearchItem
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.BatchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf // Import flowOf
import kotlinx.coroutines.flow.map // Import map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class GlobalSearchViewModel(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository
    // Add other repositories here for global search
) : ViewModel() {

    private val TAG = "GlobalSearchViewModel"

    // MutableStateFlow to hold the current search query and selected categories
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategories = MutableStateFlow(listOf("All")) // Default to "All"

    // Combine query and categories, debounce for performance, and flatMapLatest to handle new queries
    val searchResults: LiveData<List<SearchItem>> =
        combine(_searchQuery.debounce(300), _selectedCategories) { query, categories ->
            Pair(query, categories)
        }
            .distinctUntilChanged()
            .flatMapLatest { (query, categories) ->
                if (query.isBlank() && categories.contains("All") && categories.size == 1) { // No active search, show empty list
                    flowOf(emptyList()) // Use flowOf
                } else if (query.isBlank() && categories.isEmpty()) { // No query and no categories selected
                    flowOf(emptyList()) // Use flowOf
                }
                else {
                    performCombinedSearch(query, categories)
                }
            }.asLiveData(Dispatchers.IO)


    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategories(categories: List<String>) {
        _selectedCategories.value = categories
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun performCombinedSearch(query: String, categories: List<String>): kotlinx.coroutines.flow.Flow<List<SearchItem>> {
        _isLoading.postValue(true) // FIX: Use postValue as it's on a background thread
        Log.d(TAG, "Performing search for query: '$query' in categories: $categories")

        return combine(
            if (categories.contains("All") || categories.contains("Students")) {
                studentRepository.searchStudents(query).map { students: List<com.aquaa.edusoul.models.Student> ->
                    students.map { student: com.aquaa.edusoul.models.Student -> SearchItem.StudentItem(student) }
                }
            } else {
                flowOf(emptyList<SearchItem>())
            },
            if (categories.contains("All") || categories.contains("Users")) {
                userRepository.searchUsers(query).map { users: List<com.aquaa.edusoul.models.User> ->
                    users.map { user: com.aquaa.edusoul.models.User -> SearchItem.UserItem(user) }
                }
            } else {
                flowOf(emptyList<SearchItem>())
            },
            if (categories.contains("All") || categories.contains("Subjects")) {
                subjectRepository.searchSubjects(query).map { subjects: List<com.aquaa.edusoul.models.Subject> ->
                    subjects.map { subject: com.aquaa.edusoul.models.Subject -> SearchItem.SubjectItem(subject) }
                }
            } else {
                flowOf(emptyList<SearchItem>())
            },
            if (categories.contains("All") || categories.contains("Batches")) {
                batchRepository.searchBatches(query).map { batches: List<com.aquaa.edusoul.models.Batch> ->
                    batches.map { batch: com.aquaa.edusoul.models.Batch -> SearchItem.BatchItem(batch) }
                }
            } else {
                flowOf(emptyList<SearchItem>())
            }
        ) { studentResults: List<SearchItem>, userResults: List<SearchItem>, subjectResults: List<SearchItem>, batchResults: List<SearchItem> ->
            _isLoading.postValue(false) // FIX: Use postValue as it's on a background thread
            studentResults + userResults + subjectResults + batchResults
        }
    }
}