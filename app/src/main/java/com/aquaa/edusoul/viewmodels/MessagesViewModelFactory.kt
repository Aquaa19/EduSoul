// File: main/java/com/aquaa/edusoul/viewmodels/MessagesViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.MessageRepository
import com.aquaa.edusoul.repositories.UserRepository

class MessagesViewModelFactory(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessagesViewModel::class.java)) {
            return MessagesViewModel(messageRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}