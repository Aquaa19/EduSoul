// File: main/java/com/aquaa/edusoul/viewmodels/UserViewModel.kt
package com.aquaa.edusoul.viewmodels
import com.aquaa.edusoul.auth.AuthManager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.User // Import User model
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // For switching to Main thread for UI updates

class UserViewModel(
    private val repository: UserRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _manageableUsers = MutableLiveData<List<User>>()
    val manageableUsers: LiveData<List<User>> get() = _manageableUsers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        loadManageableUsers()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // addUser is not typically used for direct ViewModel calls when AuthManager handles registration.
    // Keeping it for completeness if there's an internal-only user creation.
    // If it's used with AuthManager.registerUser, the AuthManager would handle the insertion to repository.
    fun addUser(user: User) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Assuming repository.insertUser now returns String (Firebase ID)
                val newRowId = repository.insertUser(user)
                if (newRowId.isNotBlank()) { // Check for non-blank String ID
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "User added successfully!"
                    }
                } else {
                    _errorMessage.postValue("Failed to add user.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error adding user: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateUser(user: User) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // repository.updateUser already correctly expects User object with String ID
                val rowsAffected = repository.updateUser(user)
                if (rowsAffected > 0) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "User updated successfully!"
                    }
                } else {
                    _errorMessage.postValue("Failed to update user.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error updating user: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Changed userId parameter type from Long to String
    fun deleteUser(userId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // repository.deleteUserById already correctly expects String ID
                val rowsAffected = repository.deleteUserById(userId)
                if (rowsAffected > 0) {
                    withContext(Dispatchers.Main) {
                        _errorMessage.value = "User deleted successfully!"
                    }
                } else {
                    _errorMessage.postValue("Failed to delete user.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error deleting user: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadManageableUsers() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // repository.getAllUsers() returns Flow<List<User>> where User.id is String
                repository.getAllUsers().collect { users ->
                    val filteredUsers = users.filter {
                        it.role == User.ROLE_TEACHER || it.role == User.ROLE_PARENT || it.role == "manager"
                    }
                    _manageableUsers.postValue(filteredUsers)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error loading users: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateUserProfile(
        userId: String, // userId is already String
        fullName: String,
        email: String?,
        phoneNumber: String?,
        profileImagePath: String?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // repository.getUserById already correctly expects String ID
                val existingUser = repository.getUserById(userId)
                if (existingUser == null) {
                    _errorMessage.postValue("Error: User not found.")
                    return@launch
                }

                val updatedUser = existingUser.copy(
                    fullName = fullName,
                    email = email ?:"",
                    phoneNumber = phoneNumber,
                    profileImagePath = profileImagePath
                )

                // repository.updateUser already correctly expects User object with String ID
                val rowsAffected = repository.updateUser(updatedUser)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Profile updated successfully!"
                    } else {
                        _errorMessage.value = "Failed to update profile. No changes detected or user not found."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error updating profile: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun changePassword(newPassword: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // authManager.updatePassword already correctly expects newPassword String
                val success = authManager.updatePassword(newPassword)
                withContext(Dispatchers.Main) {
                    if (success) {
                        _errorMessage.value = "Password changed successfully!"
                    } else {
                        _errorMessage.value = "Failed to change password. Please re-authenticate and try again."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error changing password: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}