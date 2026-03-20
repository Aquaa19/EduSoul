// File: main/java/com/aquaa/edusoul/viewmodels/ConversationListViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.MessageRepository
import com.aquaa.edusoul.repositories.UserRepository

class ConversationListViewModelFactory(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationListViewModel::class.java)) {
            return ConversationListViewModel(userRepository, messageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}