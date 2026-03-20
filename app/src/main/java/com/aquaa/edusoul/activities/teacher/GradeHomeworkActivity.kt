// File: src/main/java/com/aquaa/edusoul/activities/teacher/GradeHomeworkActivity.kt
package com.aquaa.edusoul.activities.teacher

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aquaa.edusoul.activities.BaseActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.StudentSubmissionAdapter
import com.aquaa.edusoul.models.StudentAssignment
import com.aquaa.edusoul.models.StudentAssignmentStatus
import com.aquaa.edusoul.repositories.*
import com.aquaa.edusoul.viewmodels.GradeHomeworkViewModel
import com.aquaa.edusoul.viewmodels.GradeHomeworkViewModelFactory
import com.aquaa.edusoul.viewmodels.MessagesViewModel
import com.aquaa.edusoul.viewmodels.MessagesViewModelFactory
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.io.File
import android.view.View
import android.widget.ProgressBar
import java.util.UUID
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class GradeHomeworkActivity : BaseActivity(), StudentSubmissionAdapter.OnSubmissionActionsListener {

    private lateinit var viewModel: GradeHomeworkViewModel
    private lateinit var messagesViewModel: MessagesViewModel
    private lateinit var submissionAdapter: StudentSubmissionAdapter
    private var homeworkId: String = ""
    private lateinit var studentRepository: StudentRepository
    private lateinit var progressBarGradeHomework: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grade_homework)

        val toolbar: Toolbar = findViewById(R.id.toolbarGradeHomework)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        homeworkId = intent.getStringExtra("HOMEWORK_ID") ?: ""
        if (homeworkId.isBlank()) {
            Toast.makeText(this, "Homework ID not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val homeworkRepository = HomeworkRepository()
        studentRepository = StudentRepository()
        val studentAssignmentRepository = StudentAssignmentRepository()
        val batchAssignmentRepository = BatchAssignmentRepository()
        val messageRepository = MessageRepository()
        val userRepository = UserRepository()


        val factory = GradeHomeworkViewModelFactory(
            homeworkRepository,
            studentRepository,
            studentAssignmentRepository,
            batchAssignmentRepository
        )
        viewModel = ViewModelProvider(this, factory).get(GradeHomeworkViewModel::class.java)

        val messagesFactory = MessagesViewModelFactory(messageRepository, userRepository)
        messagesViewModel = ViewModelProvider(this, messagesFactory).get(MessagesViewModel::class.java)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewSubmissions)
        submissionAdapter = StudentSubmissionAdapter(this, mutableListOf(), this)
        recyclerView.adapter = submissionAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        progressBarGradeHomework = findViewById(R.id.progressBarGradeHomework)


        findViewById<Button>(R.id.buttonSaveChanges).setOnClickListener {
            Toast.makeText(this, "All changes saved.", Toast.LENGTH_SHORT).show()
        }

        viewModel.loadSubmissions(homeworkId)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.homework.observe(this) { hw ->
            findViewById<TextView>(R.id.textViewHomeworkTitle).text = "Grading: ${hw?.title}"
        }

        viewModel.studentSubmissions.observe(this) { submissions ->
            submissionAdapter.updateSubmissions(submissions)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBarGradeHomework.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewSubmission(submission: StudentAssignmentStatus) {
        val submissionPath = submission.submissionPath
        val submissionMimeType = submission.assignment?.submissionMimeType

        if (submissionPath.isNullOrEmpty() || submissionMimeType.isNullOrEmpty()) {
            Toast.makeText(this, "No submission file or type found.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse(submissionPath)

        // Option 1: Handle content URIs (already local/accessible)
        if (uri.scheme == "content") {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, submissionMimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.e("GradeHomeworkActivity", "No app found to directly view local file: ${e.message}")
                Toast.makeText(this, "No app to directly view local file.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("GradeHomeworkActivity", "Error opening local file directly: ${e.message}", e)
                Toast.makeText(this, "Error opening local file directly: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Option 2: Handle remote URLs (e.g., Firebase Storage URLs)
        // Try to open PDF directly from URL if it's a PDF, otherwise download
        if (submissionMimeType == "application/pdf") {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, submissionMimeType) // Use the original remote URL URI
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                Toast.makeText(this, "Attempting to open PDF directly...", Toast.LENGTH_SHORT).show()
                return // If successful, stop here
            } catch (e: ActivityNotFoundException) {
                Log.e("GradeHomeworkActivity", "No app found to open PDF directly from URL, falling back to download: ${e.message}")
                Toast.makeText(this, "No PDF viewer found or cannot open directly from URL. Downloading...", Toast.LENGTH_SHORT).show()
                // Fall through to download logic
            } catch (e: Exception) {
                Log.e("GradeHomeworkActivity", "Error opening PDF directly from URL, falling back to download: ${e.message}", e)
                Toast.makeText(this, "Error opening PDF directly from URL. Downloading...", Toast.LENGTH_SHORT).show()
                // Fall through to download logic
            }
        }

        // Default: Download the file first and then open it via FileProvider
        // This path is taken for non-PDF remote files or if direct PDF open failed
        Toast.makeText(this, "Downloading submission to open...", Toast.LENGTH_SHORT).show()
        downloadAndOpenFile(submissionPath, submission.studentName, submissionMimeType)
    }

    private fun downloadAndOpenFile(url: String, filenamePrefix: String, mimeType: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlUri = Uri.parse(url)
                val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                val uniqueFileName = "${filenamePrefix.replace(" ", "_")}_${UUID.randomUUID()}.${fileExtension ?: "tmp"}"

                val request = DownloadManager.Request(urlUri)
                    .setTitle("Downloading: ${filenamePrefix}")
                    .setDescription("Opening homework submission...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(this@GradeHomeworkActivity, Environment.DIRECTORY_DOWNLOADS, uniqueFileName)

                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)

                var downloading = true
                while (downloading) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    cursor.moveToFirst()
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (columnIndex == -1) {
                        downloading = false
                        continue
                    }
                    val status = cursor.getInt(columnIndex)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false
                        val downloadedUri = downloadManager.getUriForDownloadedFile(downloadId)
                        withContext(Dispatchers.Main) {
                            if (downloadedUri != null) {
                                openDownloadedFile(downloadedUri, mimeType)
                            } else {
                                Toast.makeText(this@GradeHomeworkActivity, "Downloaded file URI not found.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                        val reasonColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = if (reasonColumnIndex != -1) cursor.getInt(reasonColumnIndex) else -1
                        val errorMessage = "Download failed: $reason"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@GradeHomeworkActivity, errorMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                    cursor.close()
                    if (downloading) Thread.sleep(500)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GradeHomeworkActivity, "Failed to download and open file: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("GradeHomeworkActivity", "Error downloading/opening file: ${e.message}", e)
            }
        }
    }

    private fun openDownloadedFile(contentUri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No application found to open this file type. Make sure you have a PDF viewer, image gallery, etc.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching file viewer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRemind(submission: StudentAssignmentStatus) {
        lifecycleScope.launch {
            val student = studentRepository.getStudentById(submission.assignment!!.studentId)
            student?.parentUserId?.let { parentId ->
                val homeworkTitle = viewModel.homework.value?.title ?: "a homework"
                val message = "Reminder: Please remind ${student.fullName} about the homework: '$homeworkTitle'."
                messagesViewModel.setConversationUsers(
                    viewModel.homework.value?.teacherId ?: "",
                    parentId
                )
                messagesViewModel.sendMessage(message)
                Toast.makeText(
                    this@GradeHomeworkActivity,
                    "Reminder sent to parent of ${submission.studentName}",
                    Toast.LENGTH_SHORT
                ).show()
            } ?: Toast.makeText(
                this@GradeHomeworkActivity,
                "Could not send reminder. Parent not found for ${submission.studentName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onGrade(submission: StudentAssignmentStatus) {
        showGradeDialog(submission)
    }

    private fun showGradeDialog(submission: StudentAssignmentStatus) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_grade_submission, null)
        val infoTextView = dialogView.findViewById<TextView>(R.id.textViewGradingInfo)
        val marksEditText = dialogView.findViewById<EditText>(R.id.editTextMarks)
        val remarksEditText = dialogView.findViewById<EditText>(R.id.editTextRemarks)
        val tilMarks = dialogView.findViewById<TextInputLayout>(R.id.tilMarks)
        val viewSubmissionButton = dialogView.findViewById<Button>(R.id.buttonViewSubmissionDialog)

        infoTextView.text = "Grading submission for ${submission.studentName}"
        submission.marksObtained?.let { marksEditText.setText(it.toString()) }
        remarksEditText.setText(submission.remarks)
        tilMarks.helperText = "Max Marks: ${viewModel.homework.value?.maxMarks ?: "N/A"}"

        viewSubmissionButton.setOnClickListener {
            onViewSubmission(submission)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val marks = marksEditText.text.toString().toDoubleOrNull()
                val remarks = remarksEditText.text.toString() // Restored this line

                val maxMarks = viewModel.homework.value?.maxMarks
                if (marks != null && maxMarks != null && marks > maxMarks) {
                    Toast.makeText(this, "Marks cannot exceed maximum marks.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val updatedAssignment = submission.assignment!!.copy(
                    marksObtained = marks,
                    remarks = remarks,
                    status = StudentAssignment.STATUS_GRADED
                )
                viewModel.updateGrade(updatedAssignment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}