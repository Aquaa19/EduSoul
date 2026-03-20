package com.aquaa.edusoul.activities.parent

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.ParentHomeworkAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.ParentHomeworkDetails
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.*
import com.aquaa.edusoul.viewmodels.ParentViewHomeworkViewModel
import com.aquaa.edusoul.viewmodels.ParentViewHomeworkViewModelFactory
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

// NEW IMPORTS FOR DOWNLOAD MANAGER AND MIME TYPE HANDLING
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.ProgressBar


class ParentViewHomeworkActivity : AppCompatActivity(), ParentHomeworkAdapter.OnHomeworkInteractionListener {

    private val TAG = "ParentViewHomework" // Added TAG for logging

    private lateinit var viewModel: ParentViewHomeworkViewModel
    private lateinit var homeworkAdapter: ParentHomeworkAdapter
    private lateinit var authManager: AuthManager
    private var currentUser: User? = null
    private var children: List<Student> = emptyList()

    // Store the studentAssignmentId of the currently selected homework for file attachment
    private var currentHomeworkStudentAssignmentIdForUpload: String? = null
    private var tempSubmissionMimeType: String? = null

    // Using a more robust way to handle ActivityResult to get URI
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                // Ensure currentHomeworkStudentAssignmentIdForUpload is still valid before proceeding
                currentHomeworkStudentAssignmentIdForUpload?.let { studentAssignmentId ->
                    Log.d(TAG, "filePickerLauncher: Calling ViewModel with studentAssignmentId: $studentAssignmentId")
                    val fileName = getFileName(uri) // Use your existing getFileName utility
                    val mimeType = contentResolver.getType(uri) // Get the MIME type from the URI

                    // Pass the URI, studentAssignmentId, filename, AND MIME type to the ViewModel
                    viewModel.uploadHomeworkSubmission(studentAssignmentId, uri, fileName, mimeType)
                } ?: run {
                    Toast.makeText(this, "Error: No homework selected for upload.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // User cancelled file selection
            currentHomeworkStudentAssignmentIdForUpload = null // Clear if cancelled
            // tempSubmissionMimeType is now largely redundant but can be cleared for consistency
            Toast.makeText(this, "File selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_view_homework)

        val toolbar: Toolbar = findViewById(R.id.toolbarParentHomework)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        // Initialize ViewModel outside the launch block for immediate availability
        val studentRepository = StudentRepository()
        val homeworkRepository = HomeworkRepository()
        val studentAssignmentRepository = StudentAssignmentRepository()
        val subjectRepository = SubjectRepository()

        val factory = ParentViewHomeworkViewModelFactory(
            studentRepository,
            homeworkRepository,
            studentAssignmentRepository,
            subjectRepository
        )
        viewModel = ViewModelProvider(this, factory).get(ParentViewHomeworkViewModel::class.java)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewParentHomework)
        // Pass currentUserId from AuthManager here if needed by adapter immediately
        homeworkAdapter = ParentHomeworkAdapter(this, mutableListOf(), this)
        recyclerView.adapter = homeworkAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Only load children and setup observers once in onCreate
        lifecycleScope.launch(Dispatchers.Main) {
            currentUser = authManager.getLoggedInUser()

            if (currentUser == null) {
                Toast.makeText(this@ParentViewHomeworkActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            // Load children only once during creation
            viewModel.loadChildren(currentUser!!.id)
        }

        observeViewModel()
        setupSpinnerListener() // Moved spinner setup to a dedicated function
    }

    override fun onResume() {
        super.onResume()
        // No need to load children again here. ViewModel retains data.
        // This prevents unnecessary reloads and potential cancellations.
        // The viewModel.loadChildren(currentUser!!.id) is already in onCreate.
        // If the activity is recreated, onCreate will handle the initial load.
        // If just resumed, ViewModel holds the data.
    }

    private fun setupSpinnerListener() {
        findViewById<Spinner>(R.id.spinnerSelectChild).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedChildId = children[position].id
                    Log.d(TAG, "Spinner selected child: ${children[position].fullName} (ID: $selectedChildId)")
                    // Trigger homework load for the selected child.
                    // The ViewModel should handle cancelling previous loads.
                    viewModel.loadHomeworkForChild(selectedChildId)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.d(TAG, "Spinner nothing selected.")
                }
            }
    }


    private fun observeViewModel() {
        viewModel.children.observe(this) { studentList ->
            children = studentList
            val childNames = studentList.map { it.fullName }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, childNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            findViewById<Spinner>(R.id.spinnerSelectChild).adapter = adapter

            // If there's a saved instance state, restore spinner selection
            // This needs to be handled carefully in conjunction with ViewModel's state
            if (viewModel.selectedChildId.value != null && studentList.isNotEmpty()) {
                val index = studentList.indexOfFirst { it.id == viewModel.selectedChildId.value }
                if (index != -1) {
                    findViewById<Spinner>(R.id.spinnerSelectChild).setSelection(index)
                }
            } else if (studentList.isNotEmpty()) {
                // Auto-select the first child if no previous selection and list is not empty
                viewModel.loadHomeworkForChild(studentList[0].id)
            }
        }

        viewModel.homeworks.observe(this) { homeworkList ->
            Log.d(TAG, "Observed homeworks update. Count: ${homeworkList.size}")
            homeworkAdapter.updateData(homeworkList)
            updateUIState(viewModel.isLoading.value ?: false, homeworkList.isEmpty())
        }

        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToastMessage() // Clear the message to prevent showing again on config change
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            updateUIState(isLoading, viewModel.homeworks.value?.isEmpty() ?: true)
        }

        // Observe upload status from ViewModel
        viewModel.uploadProgress.observe(this) { progress ->
            val progressBar = findViewById<ProgressBar>(R.id.progressBarParentHomework)
            if (progress != null) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = progress
            } else {
                progressBar.visibility = View.GONE
                progressBar.progress = 0 // Reset progress
            }
        }

        viewModel.uploadSuccess.observe(this) { isSuccess ->
            if (isSuccess == true) { // Use true specifically to avoid null or false triggers
                Toast.makeText(this, "Submission file attached for upload!", Toast.LENGTH_SHORT).show()
                viewModel.clearUploadStatus() // Clear status after showing toast
            } else if (isSuccess == false) {
                Toast.makeText(this, "Upload failed.", Toast.LENGTH_SHORT).show()
                viewModel.clearUploadStatus() // Clear status after showing toast
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error: $it") // Log errors for debugging
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun updateUIState(isLoading: Boolean, isHomeworkListEmpty: Boolean) {
        val progressBarParentHomework: ProgressBar = findViewById(R.id.progressBarParentHomework)
        val recyclerViewParentHomework: RecyclerView = findViewById(R.id.recyclerViewParentHomework)
        val textViewNoHomework: TextView = findViewById(R.id.textViewNoHomework)

        if (isLoading) {
            progressBarParentHomework.visibility = View.VISIBLE
            recyclerViewParentHomework.visibility = View.GONE
            textViewNoHomework.visibility = View.GONE
        } else {
            progressBarParentHomework.visibility = View.GONE
            if (isHomeworkListEmpty) {
                recyclerViewParentHomework.visibility = View.GONE
                textViewNoHomework.visibility = View.VISIBLE
            } else {
                recyclerViewParentHomework.visibility = View.VISIBLE
                textViewNoHomework.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttachFile(homework: ParentHomeworkDetails) {
        // Store the ID of the homework currently being attached a file to
        Log.d(TAG, "onAttachFile: Preparing to attach for homework ID: ${homework.studentAssignmentId}")
        currentHomeworkStudentAssignmentIdForUpload = homework.studentAssignmentId
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    override fun onConfirmSubmit(homework: ParentHomeworkDetails) {
        // Here, homework.submissionPath should already be updated by the ViewModel
        // after a successful upload in handleFileSelection.
        val submissionPath = homework.submissionPath
        if (submissionPath.isNullOrEmpty()) {
            Toast.makeText(this, "Please attach a file first.", Toast.LENGTH_SHORT).show()
            return
        }
        // Call the ViewModel's submitHomework function. MIME type should ideally be stored in the Homework object
        // or re-fetched if the path itself can imply it, but for simplicity we'll pass the temporary one here.
        // Better: ViewModel should manage the entire submission object.
        viewModel.submitHomework(homework.studentAssignmentId, submissionPath, homework.submissionMimeType) // Assuming submissionMimeType is part of HomeworkDetails now.
        Toast.makeText(this, "Submitting homework...", Toast.LENGTH_SHORT).show()
    }

    override fun onViewSubmission(homework: ParentHomeworkDetails) {
        val submissionPath = homework.submissionPath
        val submissionMimeType = homework.submissionMimeType // Get the MIME type
        if (submissionPath.isNullOrEmpty()) {
            Toast.makeText(this, "No submission found.", Toast.LENGTH_SHORT).show()
            return
        }
        openFile(submissionPath, submissionMimeType) // Pass MIME type
    }

    override fun onDeleteSubmission(homework: ParentHomeworkDetails) {
        AlertDialog.Builder(this)
            .setTitle("Delete Submission")
            .setMessage("Are you sure you want to delete this submission? You will be able to re-submit.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteSubmission(homework.studentAssignmentId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onViewAttachment(homework: ParentHomeworkDetails) {
        val attachmentPath = homework.homeworkAttachmentPath
        val attachmentMimeType = homework.homeworkAttachmentMimeType
        val homeworkTitle = homework.homeworkTitle

        if (attachmentPath.isNullOrEmpty() || attachmentMimeType.isNullOrEmpty()) {
            Toast.makeText(this, "Attachment file or type not available.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(attachmentPath), attachmentMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No application found to view this file type. Attempting to download...", Toast.LENGTH_LONG).show()
            downloadFile(attachmentPath, homeworkTitle, attachmentMimeType)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening attachment: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error attempting to view attachment: ${e.message}", e)
        }
    }

    private fun openFile(url: String, mimeType: String?) {
        val finalMimeType = mimeType ?: contentResolver.getType(Uri.parse(url)) ?: "*/*"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setData(Uri.parse(url))
                setType(finalMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No application found to open this file. You may need a file viewer app.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "No app to open: $url with type $finalMimeType", e)
        } catch (e: Exception) {
            // CORRECTED: Replaced 'e' with Toast.LENGTH_LONG for the duration parameter
            Toast.makeText(this, "Error opening file: $url with type $finalMimeType: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error opening file: $url with type $finalMimeType: ${e.message}", e)
        }
    }

    private fun downloadFile(url: String, title: String, mimeType: String) {
        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val cleanTitle = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val suggestedFileName = "${cleanTitle}.${fileExtension ?: "dat"}"

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Downloading homework attachment: $suggestedFileName")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, suggestedFileName)
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Downloading file: $suggestedFileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error initiating download for $title: ${e.message}", e)
        }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        result = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result ?: "submission_file"
    }

    // handleFileSelection now delegates upload responsibility to ViewModel
    // This function is effectively replaced by the logic in filePickerLauncher's callback
    // and the ViewModel's uploadHomeworkSubmission method.
    // It's left here commented out as a reference to what was moved.
    /*
    private fun handleFileSelection(uri: Uri) {
        val userId = currentUser?.id ?: "unknown_user"
        val selectedFileName = getFileName(uri)
        val selectedMimeType = contentResolver.getType(uri)
        tempSubmissionMimeType = selectedMimeType

        val storageRef = FirebaseStorage.getInstance().reference
        val submissionRef = storageRef.child("submissions/$userId/${UUID.randomUUID()}_${selectedFileName}")
        val uploadTask = submissionRef.putFile(uri)

        val progressBar = findViewById<ProgressBar>(R.id.progressBarParentHomework)
        progressBar.visibility = View.VISIBLE

        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progressBar.progress = progress
        }.addOnSuccessListener {
            submissionRef.downloadUrl.addOnSuccessListener { downloadUri ->
                selectedHomework?.let { hw ->
                    val updatedHomeworks = viewModel.homeworks.value?.map {
                        if (it.studentAssignmentId == hw.studentAssignmentId) {
                            it.copy(submissionPath = downloadUri.toString())
                        } else { it }
                    }
                    updatedHomeworks?.let { homeworkAdapter.updateData(it) }
                    Toast.makeText(this, "Submission file attached for upload!", Toast.LENGTH_SHORT).show()
                }
                progressBar.visibility = View.GONE
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            tempSubmissionMimeType = null
        }
    }
    */
}