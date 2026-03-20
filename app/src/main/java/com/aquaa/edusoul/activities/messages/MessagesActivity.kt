package com.aquaa.edusoul.activities.messages

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.app.NotificationManager
import android.content.Context
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Message
import com.aquaa.edusoul.repositories.MessageRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.MessagesViewModel
import com.aquaa.edusoul.viewmodels.MessagesViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aquaa.edusoul.adapters.MessageAdapter

class MessagesActivity : BaseActivity() {

    private val TAG = "MessagesActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessageInput: EditText
    private lateinit var buttonSendMessage: ImageButton

    // private lateinit var authManager: AuthManager // AuthManager is handled by BaseActivity if it extends it
    private lateinit var viewModel: MessagesViewModel
    private lateinit var messageAdapter: MessageAdapter
    private var currentUserId: String = ""
    private var localOtherUserId: String? = null // Renamed to avoid confusion with companion object's otherUserId

    companion object {
        // Static flag to indicate if MessagesActivity is currently active and in foreground
        @Volatile // Make volatile for proper visibility across threads
        var isActivityActive: Boolean = false
        // Store the ID of the user currently chatting with, to prevent self-notifications
        @Volatile // Make volatile for proper visibility across threads
        var activeChatOtherUserId: String? = null // Renamed for clarity and to prevent shadowing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Get user IDs from intent
        currentUserId = intent.getStringExtra("CURRENT_USER_ID") ?: ""
        localOtherUserId = intent.getStringExtra("OTHER_USER_ID")
        val otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "User"

        // Ensure currentUserId and otherUserId are valid before proceeding
        if (currentUserId.isBlank() || localOtherUserId.isNullOrBlank()) {
            Toast.makeText(this, "Error: Conversation details are missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupToolbar(otherUserName)
        setupViewModel()
        setupRecyclerView()
        setupListeners()
        setupObservers()

        // Safely call viewModel methods with non-null user IDs
        val nonNullOtherUserId = localOtherUserId!! // Assert non-null after the check above
        viewModel.setConversationUsers(currentUserId, nonNullOtherUserId)

        // Initial marking of messages as read when activity is created/opened
        viewModel.markMessagesAsRead(currentUserId, nonNullOtherUserId)
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbarMessages)
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        editTextMessageInput = findViewById(R.id.editTextMessageInput)
        buttonSendMessage = findViewById(R.id.buttonSendMessage)
    }

    private fun setupToolbar(otherUserName: String) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = otherUserName
    }

    private fun setupViewModel() {
        val factory = MessagesViewModelFactory(
            MessageRepository(),
            UserRepository()
        )
        viewModel = ViewModelProvider(this, factory)[MessagesViewModel::class.java]
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(currentUserId)
        recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerViewMessages.adapter = messageAdapter
    }

    private fun setupListeners() {
        buttonSendMessage.setOnClickListener {
            val messageContent = editTextMessageInput.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                viewModel.sendMessage(messageContent)
                editTextMessageInput.text.clear()
            }
        }
    }

    private fun setupObservers() {
        viewModel.messages.observe(this) { messages ->
            Log.d(TAG, "Messages received in observer: ${messages.size}")
            messageAdapter.submitList(messages) {
                // Callback after list is submitted, smooth scroll only if not already at bottom
                if (messageAdapter.itemCount > 0 &&
                    (recyclerViewMessages.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() < messageAdapter.itemCount - 1) {
                    recyclerViewMessages.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }
            }
            // Re-added call to mark messages as read when new messages are observed in the chat window.
            // This ensures messages are marked as read as soon as they appear in the active chat.
            val nonNullOtherUserId = localOtherUserId
            if (!currentUserId.isBlank() && !nonNullOtherUserId.isNullOrBlank()) {
                viewModel.markMessagesAsRead(currentUserId, nonNullOtherUserId)
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
        activeChatOtherUserId = localOtherUserId // Update companion object with the current chat's otherUserId

        // Re-mark messages as read in case the activity was paused and resumed
        val nonNullOtherUserId = localOtherUserId
        if (!currentUserId.isBlank() && !nonNullOtherUserId.isNullOrBlank()) {
            viewModel.markMessagesAsRead(currentUserId, nonNullOtherUserId)
        }
    }

    override fun onPause() {
        super.onPause()
        isActivityActive = false
        activeChatOtherUserId = null // Clear the active chat user ID when leaving the activity
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}