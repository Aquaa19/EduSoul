package com.aquaa.edusoul.activities.teacher

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar // Import ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.LearningResourceAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.LearningResource
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.LearningResourceRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.TeacherSubjectBatchLinkRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ResourceManagementViewModel
import com.aquaa.edusoul.viewmodels.ResourceManagementViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ResourceManagementActivity : BaseActivity(), LearningResourceAdapter.OnLearningResourceActionsListener {

    private val TAG = "ResourceManagementAct"

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewResources: RecyclerView
    private lateinit var fabAddResource: FloatingActionButton
    private lateinit var textViewNoResources: TextView
    private lateinit var progressBar: ProgressBar // Add ProgressBar reference

    private lateinit var resourceAdapter: LearningResourceAdapter
    private lateinit var resourceList: MutableList<LearningResource>

    private lateinit var authManager: AuthManager
    private var currentUser: User? = null // Renamed from currentTeacher to currentUser for broader use
    private var isAdminAccess: Boolean = false // Flag to check if accessed by Admin

    private lateinit var viewModel: ResourceManagementViewModel

    // Data for dialog spinners
    private var allBatches: List<Batch> = emptyList() // Store all batches for admin
    private var allSubjects: List<Subject> = emptyList() // Store all subjects for admin
    // private var teachersList: List<User> = emptyList() // REMOVED: Store all teachers for admin dropdown

    // Keep track of loading states for dialog data
    private var isBatchesLoaded = false
    private var isSubjectsLoaded = false
    // private var isTeachersLoaded = false // REMOVED: No longer need to track teachers loaded state
    private var initialResourceListLoaded = false // To track initial resource list load

    private lateinit var pickFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private var tempFileUri: Uri? = null
    private var tempFileMimeType: String? = null
    private var tempFileName: String? = null
    private var tempImageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resource_management)

        toolbar = findViewById(R.id.toolbarResourceManagement)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        authManager = AuthManager(this)

        // Read the admin access flag from the intent
        isAdminAccess = intent.getBooleanExtra("IS_ADMIN_ACCESS", false)
        Log.d(TAG, "isAdminAccess: $isAdminAccess (from Intent)")

        // Initialize UI views FIRST. This is crucial for lateinit properties.
        initializeViews()

        // Set initial state for UI elements AFTER they are initialized.
        // These lines were causing the crash at line 132 in previous logs.
        fabAddResource.isEnabled = false // Disable FAB until data is loaded
        progressBar.visibility = View.VISIBLE // Show progress bar initially

        val factory = ResourceManagementViewModelFactory(
            LearningResourceRepository(),
            SubjectRepository(),
            BatchRepository(),
            UserRepository(),
            TeacherSubjectBatchLinkRepository()
        )
        viewModel = ViewModelProvider(this, factory)[ResourceManagementViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        setupObservers() // Observers are set up to receive updates

        pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                handleFileAttachment(uri)
            } else {
                Toast.makeText(this, "File selection cancelled.", Toast.LENGTH_SHORT).show()
                clearTempFileDetails()
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    handleFileAttachment(uri)
                }
            } else {
                Toast.makeText(this, "Photo capture cancelled.", Toast.LENGTH_SHORT).show()
                clearTempFileDetails()
            }
        }

        // Fetch current user and set ViewModel mode
        lifecycleScope.launch(Dispatchers.IO) {
            currentUser = authManager.getLoggedInUser()
            withContext(Dispatchers.Main) {
                Log.d(TAG, "Auth: Fetched currentUser: ${currentUser?.id}, Role: ${currentUser?.role}")
                Log.d(TAG, "Auth: isAdminAccess from Intent: $isAdminAccess")

                if (currentUser == null) {
                    Log.e(TAG, "Auth: User is null, finishing activity.")
                    Toast.makeText(this@ResourceManagementActivity, "User not logged in. Please log in.", Toast.LENGTH_LONG).show()
                    finish()
                    return@withContext
                }

                // First, check if the user is an Admin (or Owner)
                if (currentUser?.role == User.ROLE_ADMIN || currentUser?.role == User.ROLE_OWNER) {
                    Log.d(TAG, "User is Admin/Owner. Setting Admin Mode.")
                    viewModel.setAdminMode() // ViewModel handles fetching all data for Admin mode
                }
                // Then, check if the user is a Teacher
                else if (currentUser?.role == User.ROLE_TEACHER) {
                    Log.d(TAG, "User is Teacher. Setting Teacher ID.")
                    currentUser?.id?.let {
                        viewModel.setTeacherId(it)
                    }
                }
                // If not Admin and not Teacher, deny access
                else {
                    Toast.makeText(this@ResourceManagementActivity, "Access Denied: Only admins and teachers can manage resources.", Toast.LENGTH_LONG).show()
                    finish()
                    return@withContext
                }
            }
        }
    }

    private fun handleFileAttachment(uri: Uri) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.e(TAG, "UPLOAD ERROR: FirebaseAuth.getInstance().currentUser is NULL. User is NOT authenticated.")
            Toast.makeText(this, "Authentication required to upload. Please log in again.", Toast.LENGTH_LONG).show()
            clearTempFileDetails()
            return
        } else {
            Log.d(TAG, "UPLOAD DEBUG: FirebaseAuth.getInstance().currentUser UID: ${firebaseUser.uid}")
            if (!isAdminAccess && firebaseUser.uid != currentUser?.id) {
                Log.w(TAG, "UPLOAD WARNING: Authenticated UID does not match currentUser ID for non-admin access. Potential mismatch.")
            }
        }

        val actualFileName = getFileName(uri) ?: "unknown_file"
        val actualMimeType = getMimeTypeFromUri(uri) ?: "application/octet-stream"

        val storageRef = FirebaseStorage.getInstance().reference
        val resourceRef = storageRef.child("resources/${UUID.randomUUID()}_$actualFileName")
        Log.d(TAG, "Attempting to upload to path: ${resourceRef.path}")

        val uploadTask = resourceRef.putFile(uri)

        // Show progress bar during upload
        viewModel.setLoading(true) // Explicitly set loading to true

        uploadTask.addOnSuccessListener {
            Log.d(TAG, "File uploaded successfully to Storage.")
            resourceRef.downloadUrl.addOnSuccessListener { downloadUri ->
                Log.d(TAG, "Download URL obtained: $downloadUri")
                tempFileMimeType = actualMimeType
                tempFileName = actualFileName
                showAddEditResourceDialog(null, downloadUri.toString(), tempFileName)
                viewModel.setLoading(false) // Hide loading after dialog is shown or on success
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Upload failed: ${exception.message}", exception)
            Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            clearTempFileDetails()
            viewModel.setLoading(false) // Hide loading on failure
        }
    }


    private fun initializeViews() {
        recyclerViewResources = findViewById(R.id.recyclerViewResources)
        fabAddResource = findViewById(R.id.fabAddResource)
        textViewNoResources = findViewById(R.id.textViewNoResources)
        progressBar = findViewById(R.id.progressBar) // Initialize ProgressBar here
        resourceList = ArrayList()
    }

    private fun setupRecyclerView() {
        resourceAdapter = LearningResourceAdapter(this, mutableListOf(), this)
        recyclerViewResources.layoutManager = LinearLayoutManager(this)
        recyclerViewResources.adapter = resourceAdapter
    }

    private fun setupListeners() {
        fabAddResource.setOnClickListener {
            // Check if FAB is enabled (meaning data is loaded) before showing dialog
            if (fabAddResource.isEnabled) {
                showResourceTypeDialog()
            } else {
                Toast.makeText(this, "Please wait, loading data...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // This function controls the visibility of the ProgressBar and the enabled state of the FAB.
    private fun updateLoadingAndFabState() {
        // Condition for enabling FAB and hiding main loading ProgressBar
        // For admin/owner, batches, subjects, and initial resources must be loaded.
        // For teacher, only initial resources, batches, and subjects need to be loaded.
        val isReadyForInteraction = initialResourceListLoaded && isBatchesLoaded && isSubjectsLoaded

        if (isReadyForInteraction) {
            progressBar.visibility = View.GONE
            fabAddResource.isEnabled = true
            Log.d(TAG, "All essential data loaded. FAB enabled. ProgressBar hidden.")
        } else {
            // Keep ProgressBar visible and FAB disabled if still loading
            progressBar.visibility = View.VISIBLE
            fabAddResource.isEnabled = false
            Log.d(TAG, "Loading essential data... FAB disabled. ProgressBar visible.")
        }
    }

    private fun setupObservers() {
        viewModel.resourcesForTeacherOrAdmin.observe(this) { resources -> // Observe the combined LiveData
            Log.d(TAG, "Observer: Received ${resources.size} resources. Updating RecyclerView.")
            resourceAdapter.setResources(resources)
            textViewNoResources.visibility = if (resources.isEmpty()) View.VISIBLE else View.GONE
            recyclerViewResources.visibility = if (resources.isEmpty()) View.GONE else View.VISIBLE
            initialResourceListLoaded = true
            updateLoadingAndFabState() // Update state whenever resources are loaded
        }

        viewModel.batchesForDropdown.observe(this) { batches ->
            allBatches = batches
            isBatchesLoaded = true
            Log.d(TAG, "Observer: Fetched ${batches.size} batches for dropdown. isBatchesLoaded: $isBatchesLoaded")
            updateLoadingAndFabState() // Update state whenever batches are loaded
        }

        viewModel.subjectsForDropdown.observe(this) { subjects ->
            allSubjects = subjects
            isSubjectsLoaded = true
            Log.d(TAG, "Observer: Fetched ${subjects.size} subjects for dropdown. isSubjectsLoaded: $isSubjectsLoaded")
            updateLoadingAndFabState() // Update state whenever subjects are loaded
        }

        // REMOVED: viewModel.teachersForDropdown.observe(this) block as spinner is removed and is no longer needed for loading state.
        /*
        viewModel.teachersForDropdown.observe(this) { teachers ->
            teachersList = teachers
            isTeachersLoaded = true // Mark as loaded even if empty, as absence of teachers is also a state
            Log.d(TAG, "Observer: Fetched ${teachers.size} teachers for dropdown. isTeachersLoaded: $isTeachersLoaded")
            teachers.forEachIndexed { index, teacher ->
                Log.d(TAG, "Observer: Teacher $index: ${teacher.fullName} (${teacher.id})")
            }
            updateLoadingAndFabState() // Update state whenever teachers are loaded
        }
        */

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
        // Observe isLoading from ViewModel for general loading (e.g. during upload/delete)
        // This will override the general loading state when specific operations are active.
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
                fabAddResource.isEnabled = false // Disable FAB during any specific operation
            } else {
                // If specific operation finished, revert to the general loading state decision
                updateLoadingAndFabState()
            }
        }
    }

    private fun showResourceTypeDialog() {
        val resourceTypes = arrayOf("Document", "Link", "Photo/Gallery")
        AlertDialog.Builder(this)
            .setTitle("Add a Resource")
            .setItems(resourceTypes) { _, which ->
                when (which) {
                    0 -> showFilePicker() // Document
                    1 -> showAddLinkDialog() // Link
                    2 -> showPhotoGalleryDialog() // Photo/Gallery
                }
            }
            .show()
    }

    private fun showPhotoGalleryDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Add a Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePicture()
                    1 -> pickFromGallery()
                }
            }
            .show()
    }

    private fun takePicture() {
        lifecycleScope.launch {
            getTmpFileUri().let { uri ->
                tempImageUri = uri
                takePictureLauncher.launch(uri)
            }
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickFileLauncher.launch(arrayOf("image/*"))
    }


    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(applicationContext, "${packageName}.provider", tmpFile)
    }


    private fun showAddLinkDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_link, null)
        val editTextUrl = dialogView.findViewById<EditText>(R.id.editTextUrl)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextLinkTitle)

        AlertDialog.Builder(this)
            .setTitle("Add Link")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val url = editTextUrl.text.toString().trim()
                    val title = editTextTitle.text.toString().trim()
                    if (url.isNotEmpty() && title.isNotEmpty()) {
                        val uri = Uri.parse(url)
                        if (uri.scheme == "http" || uri.scheme == "https") {
                            tempFileMimeType = "text/html"
                            showAddEditResourceDialog(null, url, title)
                            dismiss()
                        } else {
                            Toast.makeText(this@ResourceManagementActivity, "Please enter a valid URL starting with http:// or https://", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ResourceManagementActivity, "URL and Title cannot be empty.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }


    private fun showFilePicker() {
        val mimeTypes = arrayOf(
            "application/pdf",
            "image/*",
            "video/*",
            "text/*",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        pickFileLauncher.launch(mimeTypes)
    }

    private fun getMimeTypeFromUri(uri: Uri): String? {
        return contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
    }

    private fun getFileName(uri: Uri): String? {
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
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }

    private fun showAddEditResourceDialog(resourceToEdit: LearningResource?, prefilledUri: String? = null, prefilledFileName: String? = null) {
        // REMOVED: Early exit if teachersList is empty for admin (since we no longer need to pick a teacher)
        /*
        if (isAdminAccess && teachersList.isEmpty()) {
            Toast.makeText(this, "No teachers found. Please add teacher accounts first to assign resources.", Toast.LENGTH_LONG).show()
            return
        }
        */

        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_resource, null)
        builder.setView(dialogView)

        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.tilResourceTitleDialog)
        val editTextTitle = dialogView.findViewById<TextInputEditText>(R.id.editTextResourceTitleDialog)
        val editTextDescription = dialogView.findViewById<TextInputEditText>(R.id.editTextResourceDescriptionDialog)
        val tilFilePathOrUrl = dialogView.findViewById<TextInputLayout>(R.id.tilFilePathOrUrlDialog)
        val editTextFilePathOrUrl = dialogView.findViewById<TextInputEditText>(R.id.editTextFilePathOrUrlDialog)
        val spinnerResourceType = dialogView.findViewById<Spinner>(R.id.spinnerResourceType)

        val checkBoxTargetAll = dialogView.findViewById<CheckBox>(R.id.checkBoxTargetAll)
        val checkBoxTargetSpecificBatch = dialogView.findViewById<CheckBox>(R.id.checkBoxTargetSpecificBatch)
        val spinnerTargetBatch = dialogView.findViewById<Spinner>(R.id.spinnerTargetBatch)
        val checkBoxTargetSpecificSubject = dialogView.findViewById<CheckBox>(R.id.checkBoxTargetSpecificSubject)
        val spinnerTargetSubject = dialogView.findViewById<Spinner>(R.id.spinnerTargetSubject)
        val textViewTargetBatchLabel = dialogView.findViewById<TextView>(R.id.textViewTargetBatchLabel)
        val textViewTargetSubjectLabel = dialogView.findViewById<TextView>(R.id.textViewTargetSubjectLabel)

        // REMOVED: Admin specific UI elements for teacher selection
        // val tilTeacherDropdown = dialogView.findViewById<TextInputLayout>(R.id.tilTeacherResourceDialog)
        // val spinnerTeacher = dialogView.findViewById<Spinner>(R.id.spinnerTeacherResourceDialog)


        // REMOVED: Logic for showing/hiding and populating teacher spinner
        /*
        if (isAdminAccess || currentUser?.role == User.ROLE_ADMIN || currentUser?.role == User.ROLE_OWNER) {
            tilTeacherDropdown.visibility = View.VISIBLE
            Log.d(TAG, "Dialog: Admin Access detected. Populating teacher spinner.")
            Log.d(TAG, "Dialog: teachersList size: ${teachersList.size}")

            val teacherNames = teachersList.map { it.fullName }.toMutableList()
            teacherNames.add(0, "Select Teacher") // Placeholder
            val teacherAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teacherNames)
            teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTeacher.adapter = teacherAdapter

            // If editing, pre-select teacher
            resourceToEdit?.uploadedByTeacherId?.let { teacherId ->
                val pos = teachersList.indexOfFirst { it.id == teacherId }
                if (pos != -1) spinnerTeacher.setSelection(pos + 1)
            }
        } else {
            tilTeacherDropdown.visibility = View.GONE
            Log.d(TAG, "Dialog: Not Admin Access. Hiding teacher spinner.")
        }
        */


        val resourceTypes = arrayOf(
            "Document", "Video", "Image", "Link", "Other"
        )
        val resourceTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resourceTypes)
        resourceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerResourceType.adapter = resourceTypeAdapter


        val batchNames = allBatches.map { it.batchName }.toMutableList()
        batchNames.add(0, "Select Batch") // Placeholder
        val batchAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, batchNames)
        batchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTargetBatch.adapter = batchAdapter

        val subjectNames = allSubjects.map { it.subjectName }.toMutableList()
        subjectNames.add(0, "Select Subject") // Placeholder
        val subjectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subjectNames)
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTargetSubject.adapter = subjectAdapter


        builder.setTitle(if (resourceToEdit == null) "Add New Resource" else "Edit Resource")

        var initialSpinnerSelection = 0 // Default to Document or first item
        if (resourceToEdit != null) {
            editTextTitle.setText(resourceToEdit.title)
            editTextDescription.setText(resourceToEdit.description)
            editTextFilePathOrUrl.setText(resourceToEdit.filePathOrUrl)
            val mimeType = resourceToEdit.fileMimeType
            initialSpinnerSelection = when {
                mimeType.startsWith("image/") -> resourceTypes.indexOf("Image").coerceAtLeast(0)
                mimeType.startsWith("video/") -> resourceTypes.indexOf("Video").coerceAtLeast(0)
                mimeType == "application/pdf" || mimeType == "application/msword" || mimeType.startsWith("application/vnd.openxmlformats-officedocument") -> resourceTypes.indexOf("Document").coerceAtLeast(0)
                mimeType == "text/html" -> resourceTypes.indexOf("Link").coerceAtLeast(0)
                else -> resourceTypes.indexOf("Other").coerceAtLeast(0)
            }

            checkBoxTargetAll.isChecked = resourceToEdit.batchId == null && resourceToEdit.subjectId == null
            checkBoxTargetSpecificBatch.isChecked = resourceToEdit.batchId != null
            checkBoxTargetSpecificSubject.isChecked = resourceToEdit.subjectId != null

            resourceToEdit.batchId?.let { batchId ->
                val pos = allBatches.indexOfFirst { it.id == batchId }
                if (pos != -1) spinnerTargetBatch.setSelection(pos + 1)
            }
            resourceToEdit.subjectId?.let { subjectId ->
                val pos = allSubjects.indexOfFirst { it.id == subjectId }
                if (pos != -1) spinnerTargetSubject.setSelection(pos + 1)
            }

        } else if (prefilledUri != null && prefilledFileName != null) {
            editTextTitle.setText(prefilledFileName.substringBeforeLast("."))
            editTextFilePathOrUrl.setText(prefilledUri)
            val inferredMimeType = tempFileMimeType
            initialSpinnerSelection = when {
                inferredMimeType?.startsWith("image/") == true -> resourceTypes.indexOf("Image").coerceAtLeast(0)
                inferredMimeType?.startsWith("video/") == true -> resourceTypes.indexOf("Video").coerceAtLeast(0)
                inferredMimeType == "application/pdf" || inferredMimeType == "application/msword" || inferredMimeType?.startsWith("application/vnd.openxmlformats-officedocument") == true -> resourceTypes.indexOf("Document").coerceAtLeast(0)
                prefilledUri.startsWith("http://") || prefilledUri.startsWith("https://") -> resourceTypes.indexOf("Link").coerceAtLeast(0)
                else -> resourceTypes.indexOf("Other").coerceAtLeast(0)
            }
        }
        spinnerResourceType.setSelection(initialSpinnerSelection)


        val updateTargetVisibility = {
            textViewTargetBatchLabel.visibility = if (checkBoxTargetSpecificBatch.isChecked) View.VISIBLE else View.GONE
            spinnerTargetBatch.visibility = if (checkBoxTargetSpecificBatch.isChecked) View.VISIBLE else View.GONE
            textViewTargetSubjectLabel.visibility = if (checkBoxTargetSpecificSubject.isChecked) View.VISIBLE else View.GONE
            spinnerTargetSubject.visibility = if (checkBoxTargetSpecificSubject.isChecked) View.VISIBLE else View.GONE
        }

        checkBoxTargetAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxTargetSpecificBatch.isChecked = false
                checkBoxTargetSpecificSubject.isChecked = false
            }
            updateTargetVisibility()
        }
        checkBoxTargetSpecificBatch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxTargetAll.isChecked = false
            }
            updateTargetVisibility()
        }
        checkBoxTargetSpecificSubject.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkBoxTargetAll.isChecked = false
            }
            updateTargetVisibility()
        }
        updateTargetVisibility()

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss(); clearTempFileDetails() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val filePathOrUrl = editTextFilePathOrUrl.text.toString().trim()
            val selectedResourceTypeFromSpinner = spinnerResourceType.selectedItem.toString()

            val mimeTypeToSave: String = when (selectedResourceTypeFromSpinner) {
                "Document" -> tempFileMimeType ?: "application/pdf" // Prefer actual detected, fallback to common doc
                "Video" -> tempFileMimeType ?: "video/*"
                "Image" -> tempFileMimeType ?: "image/*"
                "Link" -> "text/html" // Links always get text/html
                else -> tempFileMimeType ?: "application/octet-stream" // Generic fallback
            }

            // Determine uploadedByTeacherId based on user role
            val uploadedByTeacherId: String? = if (currentUser?.role == User.ROLE_ADMIN || currentUser?.role == User.ROLE_OWNER || currentUser?.role == User.ROLE_TEACHER) {
                currentUser?.id // Always use current logged-in user's ID
            } else {
                null // Should not happen based on earlier access checks, but for safety
            }


            if (uploadedByTeacherId == null) {
                Toast.makeText(this, "Cannot save resource without an assigned user.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetBatchId: String? = if (checkBoxTargetSpecificBatch.isChecked) {
                val selectedBatchPosition = spinnerTargetBatch.selectedItemPosition
                if (selectedBatchPosition > 0) allBatches[selectedBatchPosition - 1].id else {
                    Toast.makeText(this, "Please select a specific batch.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                null
            }

            val targetSubjectId: String? = if (checkBoxTargetSpecificSubject.isChecked) {
                val selectedSubjectPosition = spinnerTargetSubject.selectedItemPosition
                if (selectedSubjectPosition > 0) allSubjects[selectedSubjectPosition - 1].id else {
                    Toast.makeText(this, "Please select a specific subject.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                null
            }

            if (title.isEmpty() || filePathOrUrl.isEmpty()) {
                Toast.makeText(this, "Title and File Path/URL are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newOrUpdatedResource = LearningResource(
                id = resourceToEdit?.id ?: "",
                title = title,
                description = if (description.isEmpty()) null else description,
                filePathOrUrl = filePathOrUrl,
                fileMimeType = mimeTypeToSave,
                subjectId = targetSubjectId,
                batchId = targetBatchId,
                uploadedByTeacherId = uploadedByTeacherId, // This is now correctly handled for admin/teacher
                uploadDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            )

            Log.d(TAG, "Saving LearningResource to Firestore: $newOrUpdatedResource")

            if (resourceToEdit == null) {
                viewModel.addResource(newOrUpdatedResource)
            } else {
                viewModel.updateResource(newOrUpdatedResource)
            }
            alertDialog.dismiss()
            clearTempFileDetails()
        }
    }

    override fun onEditResource(resource: LearningResource, position: Int) {
        showAddEditResourceDialog(resource)
    }

    override fun onDeleteResource(resource: LearningResource, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Resource")
            .setMessage("Are you sure you want to delete '${resource.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteResource(resource.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onViewResource(resource: LearningResource) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(resource.filePathOrUrl), resource.fileMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open resource with"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No application found to open this resource type.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error opening resource: ${e.message}", e)
        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred while trying to open the resource: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "General error opening resource: ${e.message}", e)
        }
    }

    override fun onDownloadResource(resource: LearningResource) {
        val downloadUrl = resource.filePathOrUrl
        val fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(resource.fileMimeType)
        val fileName = getFileNameFromUrl(downloadUrl) ?: "${resource.title.replace(" ", "_")}.${fileExtension ?: "dat"}"

        if (downloadUrl.isEmpty() || (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://"))) {
            Toast.makeText(this, "Invalid download URL: Only HTTP/HTTPS links can be downloaded.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle(resource.title)
                .setDescription("Downloading...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Downloading file: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error initiating download for ${resource.title}: ${e.message}", e)
        }
    }

    private fun getFileNameFromUrl(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val path = uri.path
            if (path != null) {
                val fileNameWithQuery = path.substringAfterLast('/')
                fileNameWithQuery.substringBefore('?')
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun clearTempFileDetails() {
        tempFileUri = null
        tempFileMimeType = null
        tempFileName = null
        tempImageUri = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}