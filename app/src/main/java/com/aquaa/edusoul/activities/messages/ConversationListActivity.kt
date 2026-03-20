package com.aquaa.edusoul.activities.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.ConversationListAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.MessageRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ConversationListViewModel
import com.aquaa.edusoul.viewmodels.ConversationListViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversationListActivity : BaseActivity(), ConversationListAdapter.OnContactClickListener {

    private val TAG = "ConversationListAct"

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewConversations: RecyclerView
    private lateinit var textViewNoConversations: TextView

    private lateinit var authManager: AuthManager
    private var currentUser: User? = null

    private lateinit var viewModel: ConversationListViewModel
    private lateinit var conversationAdapter: ConversationListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_list)

        toolbar = findViewById(R.id.toolbarConversationList)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Messages"

        recyclerViewConversations = findViewById(R.id.recyclerViewConversations)
        textViewNoConversations = findViewById(R.id.textViewNoConversations)

        authManager = AuthManager(this)

        val factory = ConversationListViewModelFactory(
            UserRepository(),
            MessageRepository()
        )
        viewModel = ViewModelProvider(this, factory)[ConversationListViewModel::class.java]

        conversationAdapter = ConversationListAdapter(this, emptyList(), this)
        recyclerViewConversations.layoutManager = LinearLayoutManager(this)
        recyclerViewConversations.adapter = conversationAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            currentUser = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                if (currentUser == null) {
                    Toast.makeText(this@ConversationListActivity, "Error: User not identified.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Initialize the ViewModel with current user details
                    viewModel.loadContacts(currentUser!!.id, currentUser!!.role)
                    setupObservers()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure contacts are reloaded/observed when returning to this activity
        // The ViewModel's LiveData is already observing, so this might not be strictly needed
        // unless you want to re-trigger the filtering logic explicitly.
        // For now, let's rely on the LiveData's real-time nature.
    }

    private fun setupObservers() {
        viewModel.contacts.observe(this) { contacts ->
            conversationAdapter.updateContacts(contacts)
            textViewNoConversations.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
            recyclerViewConversations.visibility = if (contacts.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    override fun onContactClick(contact: User) {
        val intent = Intent(this, MessagesActivity::class.java).apply {
            putExtra("CURRENT_USER_ID", currentUser?.id)
            putExtra("OTHER_USER_ID", contact.id)
            putExtra("OTHER_USER_NAME", contact.fullName)
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
