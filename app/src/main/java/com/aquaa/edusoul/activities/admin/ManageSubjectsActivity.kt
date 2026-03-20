// File: main/java/com/aquaa/edusoul/activities/admin/ManageSubjectsActivity.kt
package com.aquaa.edusoul.activities.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.SubjectAdapter
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.viewmodels.SubjectViewModel
import com.aquaa.edusoul.viewmodels.SubjectViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import java.util.ArrayList
import android.content.Intent // Import Intent

/*
 * ManageSubjectsActivity: Allows an admin to manage subjects.
 * Migrated to Kotlin and MVVM.
 */
class ManageSubjectsActivity : BaseActivity(), SubjectAdapter.OnSubjectActionsListener {

    private val TAG = "ManageSubjectsActivity"

    private lateinit var recyclerViewSubjects: RecyclerView
    private lateinit var fabAddSubject: FloatingActionButton
    private lateinit var textViewNoSubjects: TextView
    private lateinit var subjectAdapter: SubjectAdapter
    private lateinit var subjectList: MutableList<Subject>

    private lateinit var subjectViewModel: SubjectViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_subjects)

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageSubjects)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val subjectRepository = SubjectRepository()
        val factory = SubjectViewModelFactory(subjectRepository)
        subjectViewModel = ViewModelProvider(this, factory)[SubjectViewModel::class.java]

        recyclerViewSubjects = findViewById(R.id.recyclerViewSubjects)
        fabAddSubject = findViewById(R.id.fabAddSubject)
        textViewNoSubjects = findViewById(R.id.textViewNoSubjects)

        subjectList = ArrayList()
        subjectAdapter = SubjectAdapter(this, ArrayList(subjectList), this)
        recyclerViewSubjects.layoutManager = LinearLayoutManager(this)
        recyclerViewSubjects.adapter = subjectAdapter

        fabAddSubject.setOnClickListener { showAddEditSubjectDialog(null) }

        setupObservers()
    }

    private fun setupObservers() {
        subjectViewModel.allSubjects.observe(this) { subjectsFromDb ->
            Log.d(TAG, "Subjects LiveData updated. Size: ${subjectsFromDb.size}")
            subjectAdapter.setSubjects(subjectsFromDb)
            updateEmptyState()
        }

        subjectViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error/Success message: $it")
                subjectViewModel.clearErrorMessage()
            }
        }
    }

    private fun updateEmptyState() {
        if (subjectAdapter.itemCount == 0) {
            textViewNoSubjects.visibility = View.VISIBLE
            recyclerViewSubjects.visibility = View.GONE
        } else {
            textViewNoSubjects.visibility = View.GONE
            recyclerViewSubjects.visibility = View.VISIBLE
        }
    }

    private fun showAddEditSubjectDialog(subjectToEdit: Subject?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_subject, null)
        builder.setView(dialogView)

        val tilSubjectName = dialogView.findViewById<TextInputLayout>(R.id.tilSubjectNameDialog)
        val editTextSubjectName = dialogView.findViewById<EditText>(R.id.editTextSubjectNameDialog)
        val tilSubjectCode = dialogView.findViewById<TextInputLayout>(R.id.tilSubjectCodeDialog)
        val editTextSubjectCode = dialogView.findViewById<EditText>(R.id.editTextSubjectCodeDialog)
        val tilSubjectDescription = dialogView.findViewById<TextInputLayout>(R.id.tilSubjectDescriptionDialog)
        val editTextSubjectDescription = dialogView.findViewById<EditText>(R.id.editTextSubjectDescriptionDialog)

        builder.setTitle(if (subjectToEdit == null) "Add New Subject" else "Edit Subject")

        if (subjectToEdit != null) {
            editTextSubjectName.setText(subjectToEdit.subjectName)
            editTextSubjectCode.setText(subjectToEdit.subjectCode)
            editTextSubjectDescription.setText(subjectToEdit.description)
        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = editTextSubjectName.text.toString().trim()
            val code = editTextSubjectCode.text.toString().trim()
            val description = editTextSubjectDescription.text.toString().trim()

            tilSubjectName.error = null
            tilSubjectCode.error = null

            if (name.isEmpty()) {
                tilSubjectName.error = "Subject name cannot be empty"
                return@setOnClickListener
            }

            val subject = Subject(
                id = subjectToEdit?.id ?: "", // Corrected: Default ID to empty string for String type
                subjectName = name,
                subjectCode = code.ifEmpty { null },
                description = description.ifEmpty { null }
            )

            if (subjectToEdit == null) { // Adding new subject
                Log.i(TAG, "showAddEditSubjectDialog: Attempting to add new subject: \"$name\"")
                subjectViewModel.addSubject(subject)
            } else { // Editing existing subject
                Log.i(TAG, "showAddEditSubjectDialog: Attempting to update subject ID: ${subject.id}, New Name: \"$name\"")
                subjectViewModel.updateSubject(subject)
            }
            alertDialog.dismiss()
        }
    }

    override fun onEditSubject(subject: Subject, position: Int) {
        Log.d(TAG, "onEditSubject: Editing subject: ${subject.subjectName}")
        showAddEditSubjectDialog(subject)
    }

    override fun onDeleteSubject(subject: Subject, position: Int) {
        Log.d(TAG, "onDeleteSubject: Attempting to delete subject: ${subject.subjectName}")
        AlertDialog.Builder(this)
            .setTitle("Delete Subject")
            .setMessage("Are you sure you want to delete '${subject.subjectName}'? This will also remove related topics, class sessions, exams, and homeworks related to this subject.")
            .setPositiveButton("Delete") { _, _ ->
                subjectViewModel.deleteSubject(subject.id) // Corrected: subject.id is String
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // NEW: Implement onManageTopics to open ManageSyllabusActivity
    override fun onManageTopics(subject: Subject, position: Int) {
        Log.d(TAG, "onManageTopics: Managing topics for subject: ${subject.subjectName} (ID: ${subject.id})")
        val intent = Intent(this, ManageSyllabusActivity::class.java).apply {
            putExtra("SUBJECT_ID", subject.id) // Pass subject ID
            putExtra("SUBJECT_NAME", subject.subjectName) // Pass subject name for toolbar title
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