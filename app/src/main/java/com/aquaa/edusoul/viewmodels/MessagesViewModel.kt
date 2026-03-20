package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Message
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.MessageRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull // Keep this import
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesViewModel(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "MessagesViewModel"

    private val _currentUserId = MutableLiveData<String>()
    private val _otherUserId = MutableLiveData<String>()

    private val _conversationUsers = MutableLiveData<Pair<String, String>>()

    // NEW: LiveData to track if the associated Activity is active/in foreground (not directly used for this fix but good to keep)
    private val _isActivityActive = MutableLiveData<Boolean>(false)

    val messages: LiveData<List<Message>> = _conversationUsers.switchMap { (user1Id, user2Id) ->
        // Directly convert the Flow from repository to LiveData for continuous updates
        messageRepository.getMessagesForConversation(user1Id, user2Id).asLiveData(Dispatchers.IO)
    }

    val otherUser: LiveData<User?> = _otherUserId.switchMap { otherId ->
        liveData(Dispatchers.IO) {
            emit(userRepository.getUserById(otherId))
        }
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setConversationUsers(currentUserId: String, otherUserId: String) {
        _currentUserId.value = currentUserId
        _otherUserId.value = otherUserId
        _conversationUsers.value = Pair(currentUserId, otherUserId)
    }

    fun setActivityActive(isActive: Boolean) {
        _isActivityActive.value = isActive
    }

    fun sendMessage(messageContent: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val senderId = _currentUserId.value
                val receiverId = _otherUserId.value

                if (senderId.isNullOrBlank() || receiverId.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { _errorMessage.value = "Sender or receiver ID missing." }
                    return@launch
                }

                val message = Message(
                    senderId = senderId,
                    receiverId = receiverId,
                    messageContent = messageContent,
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    status = Message.STATUS_SENT
                )
                messageRepository.insertMessage(message)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Message sent." // This message will be displayed as a Toast
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Failed to send message: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun markMessagesAsRead(currentUserId: String, otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            messageRepository.markMessagesAsRead(currentUserId, otherUserId)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
