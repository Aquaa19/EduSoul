// File: EduSoul/app/src/main/java/com/aquaa/edusoul/viewmodels/ConversationListViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.MessageRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.asFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A helper data class to hold user and unread count for display.
 */
data class UserWithUnreadCount(
    val user: User,
    val unreadCount: Int,
    val lastMessageTimestamp: String? = null,
    val lastMessageContent: String? = null
)

class ConversationListViewModel(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val TAG = "ConversationListVM"

    private val _userRoleFilter = MutableLiveData<String>()
    private val _currentUserId = MutableLiveData<String>()

    val contacts: LiveData<List<UserWithUnreadCount>> = combine(
        _currentUserId.asFlow().distinctUntilChanged(),
        _userRoleFilter.asFlow().distinctUntilChanged()
    ) { currentUserId, userRole ->
        Pair(currentUserId, userRole)
    }.flatMapLatest { (currentUserId, userRole) ->
        if (currentUserId.isBlank()) {
            return@flatMapLatest flowOf(emptyList())
        }

        userRepository.getAllUsers().map { allUsers ->
            val contactsList = mutableListOf<User>()

            when (userRole) {
                User.ROLE_TEACHER -> {
                    // Teachers can message Parents and Admin/Owner.
                    contactsList.addAll(allUsers.filter { it.role == User.ROLE_PARENT })
                    allUsers.firstOrNull { it.role == User.ROLE_ADMIN || it.role == User.ROLE_OWNER }?.let { admin ->
                        contactsList.add(admin)
                    }
                }
                User.ROLE_PARENT -> {
                    // Parents can message Teachers and Admin/Owner.
                    contactsList.addAll(allUsers.filter { it.role == User.ROLE_TEACHER })
                    allUsers.firstOrNull { it.role == User.ROLE_ADMIN || it.role == User.ROLE_OWNER }?.let { admin ->
                        contactsList.add(admin)
                    }
                }
                User.ROLE_ADMIN, User.ROLE_OWNER -> {
                    // Admins/Owners can message all Teachers and all Parents.
                    contactsList.addAll(allUsers.filter { it.role == User.ROLE_TEACHER || it.role == User.ROLE_PARENT })
                }
                User.ROLE_MANAGER -> { // New: Add filtering logic for Manager role
                    // Managers can message Parents and Teachers, and Admin/Owner.
                    contactsList.addAll(allUsers.filter { it.role == User.ROLE_PARENT || it.role == User.ROLE_TEACHER })
                    allUsers.firstOrNull { it.role == User.ROLE_ADMIN || it.role == User.ROLE_OWNER }?.let { admin ->
                        contactsList.add(admin)
                    }
                }
                else -> {
                    Log.w(TAG, "Unhandled user role for contact filtering: $userRole")
                }
            }

            val filteredContacts = contactsList.filter { it.id != currentUserId }

            if (filteredContacts.isEmpty()) {
                flowOf(emptyList<UserWithUnreadCount>())
            } else {
                val conversationFlows = filteredContacts.map { contact ->
                    messageRepository.getMessagesForConversation(currentUserId, contact.id)
                        .map { messages ->
                            val unreadCount = messages.count { it.receiverId == currentUserId && it.status != com.aquaa.edusoul.models.Message.STATUS_READ }
                            val lastMessage = messages.maxByOrNull { it.timestamp }
                            val lastMessageTimestamp = lastMessage?.timestamp
                            val lastMessageContent = lastMessage?.messageContent
                            UserWithUnreadCount(contact, unreadCount, lastMessageTimestamp, lastMessageContent)
                        }
                }
                combine(conversationFlows) { userWithUnreadCountsArray ->
                    userWithUnreadCountsArray.toList().sortedByDescending { it.lastMessageTimestamp ?: "" }
                }
            }
        }.flatMapLatest { innerFlow -> innerFlow }
    }.asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun loadContacts(currentUserId: String, userRole: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _currentUserId.value = currentUserId
            _userRoleFilter.value = userRole
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}