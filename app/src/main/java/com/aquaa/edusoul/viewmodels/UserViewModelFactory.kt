// File: com/aquaa/edusoul/viewmodels/UserViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.UserRepository // Import UserRepository
import com.aquaa.edusoul.auth.AuthManager

/**
 * Factory for creating instances of UserViewModel, injecting the UserRepository.
 */
class UserViewModelFactory(
    private val userRepository: UserRepository,
    private val authManager: AuthManager // Add AuthManager to the constructor
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST") // Suppress warning for type casting to T
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested ViewModel is UserViewModel
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            // Return a new instance of UserViewModel with the provided UserRepository
            return UserViewModel(userRepository, authManager) as T // Pass authManager
        }
        // If an unknown ViewModel is requested, throw an exception
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}