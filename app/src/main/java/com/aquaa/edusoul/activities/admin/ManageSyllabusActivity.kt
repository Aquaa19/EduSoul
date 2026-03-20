// File: main/java/com/aquaa/edusoul/activities/admin/ManageSyllabusActivity.kt
package com.aquaa.edusoul.activities.admin

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu // Import Menu
import android.view.MenuInflater // Import MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.SyllabusTopicAdapter
import com.aquaa.edusoul.models.SyllabusTopic
import com.aquaa.edusoul.repositories.SyllabusProgressRepository
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.viewmodels.SyllabusTopicViewModel
import com.aquaa.edusoul.viewmodels.SyllabusTopicViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout

class ManageSyllabusActivity : BaseActivity(), SyllabusTopicAdapter.OnTopicActionsListener {

    private val TAG = "ManageSyllabusActivity"

    private lateinit var recyclerViewTopics: RecyclerView
    private lateinit var fabAddTopic: FloatingActionButton
    private lateinit var textViewNoTopics: TextView
    private lateinit var topicAdapter: SyllabusTopicAdapter
    private val topicList = ArrayList<SyllabusTopic>()
    private lateinit var syllabusTopicViewModel: SyllabusTopicViewModel

    private var subjectId: String = ""
    private var subjectName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_syllabus)

        if (intent.extras != null) {
            subjectId = intent.getStringExtra("SUBJECT_ID") ?: ""
            subjectName = intent.getStringExtra("SUBJECT_NAME")
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageSyllabus)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Syllabus: " + (subjectName ?: "")
        }

        if (subjectId.isBlank()) {
            Toast.makeText(this, "Error: Subject not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val syllabusTopicRepository = SyllabusTopicRepository()
        val syllabusProgressRepository = SyllabusProgressRepository()
        val factory = SyllabusTopicViewModelFactory(syllabusTopicRepository, syllabusProgressRepository)
        syllabusTopicViewModel = ViewModelProvider(this, factory)[SyllabusTopicViewModel::class.java]

        recyclerViewTopics = findViewById(R.id.recyclerViewSyllabusTopics)
        fabAddTopic = findViewById(R.id.fabAddSyllabusTopic)
        textViewNoTopics = findViewById(R.id.textViewNoSyllabusTopics)

        topicAdapter = SyllabusTopicAdapter(this, topicList, this)
        recyclerViewTopics.layoutManager = LinearLayoutManager(this)
        recyclerViewTopics.adapter = topicAdapter

        fabAddTopic.setOnClickListener { _ -> showAddEditTopicDialog(null) }

        syllabusTopicViewModel.syllabusTopicsWithStatus.observe(this) { topicsWithStatus ->
            val topics = topicsWithStatus.map { it.first }
            topicAdapter.setTopics(topics)
            if (topics.isEmpty()) {
                textViewNoTopics.visibility = View.VISIBLE
            } else {
                textViewNoTopics.visibility = View.GONE
            }
        }

        syllabusTopicViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error/Success: $it")
                syllabusTopicViewModel.clearErrorMessage()
            }
        }

        syllabusTopicViewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading state: $isLoading")
        }

        syllabusTopicViewModel.selectSubjectAndBatch(subjectId, null)
    }

    override fun onResume() {
        super.onResume()
        syllabusTopicViewModel.selectSubjectAndBatch(subjectId, null)
    }

    private fun showAddEditTopicDialog(topicToEdit: SyllabusTopic?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_topic, null)
        builder.setView(dialogView)

        val tilTopicName = dialogView.findViewById<TextInputLayout>(R.id.tilTopicNameDialog)
        val editTextTopicName = dialogView.findViewById<EditText>(R.id.editTextTopicNameDialog)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextTopicDescriptionDialog)
        // No longer referencing editTextTopicOrder because it's removed from layout for auto-generation

        builder.setTitle(if (topicToEdit == null) "Add New Topic" else "Edit Topic")

        if (topicToEdit != null) {
            editTextTopicName.setText(topicToEdit.topicName)
            editTextDescription.setText(topicToEdit.description)
            // No longer setting order for editing as it's auto-generated for new, and managed by reorder for existing.
        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { _ ->
            val topicName = editTextTopicName.text.toString().trim()
            val description = editTextDescription.text.toString().trim()

            tilTopicName.error = null

            if (TextUtils.isEmpty(topicName)) {
                tilTopicName.error = "Topic name cannot be empty."
                return@setOnClickListener
            }

            if (topicToEdit == null) {
                // Call addSyllabusTopic without the order parameter (it's auto-generated now)
                syllabusTopicViewModel.addSyllabusTopic(topicName, subjectId, description.ifEmpty { null })
            } else {
                val updatedTopic = topicToEdit.copy(
                    topicName = topicName,
                    description = if (description.isEmpty()) null else description,
                    order = topicToEdit.order // Keep existing order as it's not edited manually from this dialog anymore for existing topics
                )
                syllabusTopicViewModel.updateSyllabusTopic(updatedTopic)
            }
            alertDialog.dismiss()
        }
    }

    override fun onEditTopic(topic: SyllabusTopic, position: Int) {
        showAddEditTopicDialog(topic)
    }

    override fun onDeleteTopic(topic: SyllabusTopic, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Topic")
            .setMessage("Are you sure you want to delete topic: '${topic.topicName}'?")
            .setPositiveButton("Delete") { _, _ ->
                syllabusTopicViewModel.deleteSyllabusTopic(topic.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // NEW: Inflate the menu for this activity
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.manage_syllabus_menu, menu)
        return true
    }

    // NEW: Handle menu item clicks
    override fun onOptionsItemSelected(@NonNull item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_reorder_topics -> {
                // Call the ViewModel function to reorder topics
                if (subjectId.isNotBlank()) {
                    syllabusTopicViewModel.reindexSyllabusTopicsForSubject(subjectId)
                } else {
                    Toast.makeText(this, "Please select a subject first.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}