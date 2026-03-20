package com.aquaa.edusoul.activities.teacher

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.HomeworkAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.Homework
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.viewmodels.HomeworkViewModel
import com.aquaa.edusoul.viewmodels.HomeworkViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.aquaa.edusoul.activities.BaseActivity
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.webkit.MimeTypeMap
import android.widget.ProgressBar // Import ProgressBar
import kotlinx.coroutines.runBlocking


class ManageHomeworkActivity : BaseActivity(), HomeworkAdapter.OnHomeworkActionsListener {

    private lateinit var viewModel: HomeworkViewModel
    private lateinit var homeworkAdapter: HomeworkAdapter
    private lateinit var authManager: AuthManager
    private var currentUser: User? = null

    private var subjects: List<Subject> = emptyList()
    private var batches: List<Batch> = emptyList()

    private var attachedFileNameTextView: TextView? = null

    private var uploadedFileUrl: String? = null
    private var uploadedFileMimeType: String? = null
    private lateinit var progressBarUpload: ProgressBar // For upload progress


    private val filePickerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.also { uri ->
                    uploadFileToFirebaseStorage(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_homework)

        val toolbar: Toolbar = findViewById(R.id.toolbarManageHomework)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        progressBarUpload = findViewById(R.id.progressBarUploadHomework) // Initialize ProgressBar here


        lifecycleScope.launch(Dispatchers.Main) {
            currentUser = authManager.getLoggedInUser()

            if (currentUser == null) {
                Toast.makeText(this@ManageHomeworkActivity, "Authentication error", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val homeworkRepository = HomeworkRepository()
            val subjectRepository = SubjectRepository()
            val batchRepository = BatchRepository()
            val factory = HomeworkViewModelFactory(homeworkRepository, subjectRepository)
            viewModel = ViewModelProvider(this@ManageHomeworkActivity, factory).get(HomeworkViewModel::class.java)

            val recyclerView: RecyclerView = findViewById(R.id.recyclerViewHomework)
            homeworkAdapter = HomeworkAdapter(this@ManageHomeworkActivity, mutableListOf(), this@ManageHomeworkActivity) { subjectIdString ->
                runBlocking { subjectRepository.getSubjectById(subjectIdString)?.subjectName ?: "Unknown" }
            }
            recyclerView.adapter = homeworkAdapter
            recyclerView.layoutManager = LinearLayoutManager(this@ManageHomeworkActivity)

            val fab: FloatingActionButton = findViewById(R.id.fabAddHomework)
            fab.setOnClickListener {
                uploadedFileUrl = null
                uploadedFileMimeType = null
                showAddEditHomeworkDialog(null)
            }

            observeViewModel()
            viewModel.setTeacherId(currentUser!!.id)

            subjects = withContext(Dispatchers.IO) { subjectRepository.getAllSubjects().first() }
            batches = withContext(Dispatchers.IO) { batchRepository.getAllBatches().first() }
        }
    }

    private fun observeViewModel() {
        viewModel.homeworksForTeacher.observe(this) { homeworkList ->
            homeworkAdapter.setHomework(homeworkList)
            findViewById<TextView>(R.id.textViewNoHomework).visibility =
                if (homeworkList.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.errorMessage.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun showAddEditHomeworkDialog(homework: Homework?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_homework, null)
        val titleEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextHomeworkTitle)
        val descriptionEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextHomeworkDescription)
        val deadlineEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextHomeworkDeadline)
        val subjectSpinner = dialogView.findViewById<Spinner>(R.id.spinnerHomeworkSubject)
        val maxMarksEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextMaxMarks)
        val attachFileButton = dialogView.findViewById<Button>(R.id.buttonAttachFile)
        attachedFileNameTextView = dialogView.findViewById(R.id.textViewAttachedFileName)

        uploadedFileUrl = homework?.attachmentPath
        uploadedFileMimeType = homework?.attachmentMimeType

        if (!uploadedFileUrl.isNullOrEmpty()) {
            attachedFileNameTextView?.text = "Attached: ${getFileNameFromUrl(uploadedFileUrl!!) ?: "file"}"
            attachedFileNameTextView?.visibility = View.VISIBLE
        } else {
            attachedFileNameTextView?.text = "No file attached"
            attachedFileNameTextView?.visibility = View.GONE
        }


        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjects)
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        subjectSpinner.adapter = subjectAdapter

        homework?.let {
            titleEditText.setText(it.title)
            descriptionEditText.setText(it.description)
            deadlineEditText.setText(it.dueDate)
            subjectSpinner.setSelection(subjects.indexOfFirst { s -> s.id == it.subjectId })
            maxMarksEditText.setText(it.maxMarks.toString())
        }

        deadlineEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                calendar.set(year, month, dayOfMonth)
                deadlineEditText.setText(sdf.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        attachFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            filePickerLauncher.launch(intent)
        }

        AlertDialog.Builder(this)
            .setTitle(if (homework == null) "Add Homework" else "Edit Homework")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val deadline = deadlineEditText.text.toString()
                val selectedSubject = subjectSpinner.selectedItem as Subject
                val maxMarks = maxMarksEditText.text.toString().toDoubleOrNull() ?: 0.0

                val newHomework = Homework(
                    id = homework?.id ?: "",
                    title = title,
                    description = description,
                    dueDate = deadline,
                    subjectId = selectedSubject.id,
                    teacherId = currentUser!!.id,
                    attachmentPath = uploadedFileUrl,
                    attachmentMimeType = uploadedFileMimeType, // Use the uploaded MIME Type
                    maxMarks = maxMarks
                )

                if (homework == null) {
                    viewModel.addHomework(newHomework)
                } else {
                    viewModel.updateHomework(newHomework)
                }
                uploadedFileUrl = null
                uploadedFileMimeType = null
            }
            .setNegativeButton("Cancel") { _, _ ->
                uploadedFileUrl = null
                uploadedFileMimeType = null
            }
            .show()
    }

    private fun uploadFileToFirebaseStorage(uri: Uri) {
        val fileName = getFileName(uri)
        val mimeType = contentResolver.getType(uri)
        val storageRef = FirebaseStorage.getInstance().reference
        val homeworksRef = storageRef.child("homework_attachments/${UUID.randomUUID()}_$fileName")

        progressBarUpload.visibility = View.VISIBLE
        attachedFileNameTextView?.text = "Uploading: $fileName (0%)"

        homeworksRef.putFile(uri)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                progressBarUpload.progress = progress // Update ProgressBar progress
                attachedFileNameTextView?.text = "Uploading: $fileName (${progress}%)"
            }
            .addOnSuccessListener {
                homeworksRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    uploadedFileUrl = downloadUri.toString()
                    uploadedFileMimeType = mimeType
                    attachedFileNameTextView?.text = "Attached: ${fileName}"
                    Toast.makeText(this, "File uploaded successfully!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to get download URL.", Toast.LENGTH_SHORT).show()
                    uploadedFileUrl = null
                    uploadedFileMimeType = null
                    attachedFileNameTextView?.text = "No file attached"
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                uploadedFileUrl = null
                uploadedFileMimeType = null
                attachedFileNameTextView?.text = "No file attached"
            }
            .addOnCompleteListener {
                progressBarUpload.visibility = View.GONE
            }
    }

    // Corrected getFileName function using substringAfterLast for robustness
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
        // If result is still null after checking content scheme or if it's not a content URI
        if (result == null) {
            // Use Kotlin's idiomatic substringAfterLast to safely get the filename
            result = uri.path?.substringAfterLast('/')
        }
        // Provide a default fallback name if no valid name could be extracted
        return result ?: "homework_attachment"
    }

    private fun getFileNameFromUrl(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            uri.lastPathSegment?.substringAfterLast('/')
        } catch (e: Exception) {
            null
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onEdit(homework: Homework) {
        uploadedFileUrl = homework.attachmentPath
        uploadedFileMimeType = homework.attachmentMimeType
        showAddEditHomeworkDialog(homework)
    }

    override fun onDelete(homework: Homework) {
        AlertDialog.Builder(this)
            .setTitle("Delete Homework")
            .setMessage("Are you sure you want to delete this homework? This will also delete all associated student assignments and related data.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteHomework(homework.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onAssign(homework: Homework) {
        val intent = Intent(this, AssignHomeworkActivity::class.java)
        intent.putExtra("HOMEWORK_ID", homework.id)
        startActivity(intent)
    }

    override fun onGrade(homework: Homework) {
        val intent = Intent(this, GradeHomeworkActivity::class.java)
        intent.putExtra("HOMEWORK_ID", homework.id)
        startActivity(intent)
    }
}