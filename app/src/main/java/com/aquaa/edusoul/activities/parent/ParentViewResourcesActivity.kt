package com.aquaa.edusoul.activities.parent

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.LearningResourceAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.LearningResource
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.LearningResourceRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.viewmodels.ParentResourceViewModel
import com.aquaa.edusoul.viewmodels.ParentResourceViewModelFactory
import java.util.ArrayList
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext
import android.app.DownloadManager // Import DownloadManager
import android.content.Context // Import Context for getSystemService
import android.os.Environment // Import Environment
import android.webkit.MimeTypeMap // Import MimeTypeMap

// Corrected: Implemented LearningResourceAdapter.OnLearningResourceActionsListener
class ParentViewResourcesActivity : AppCompatActivity(), LearningResourceAdapter.OnLearningResourceActionsListener {

    private val TAG = "ParentViewResources"

    private lateinit var toolbar: Toolbar
    private lateinit var spinnerSelectChild: Spinner
    private lateinit var recyclerViewResources: RecyclerView
    private lateinit var textViewNoResources: TextView

    private lateinit var authManager: AuthManager
    private var currentUser: User? = null

    private lateinit var children: List<Student>
    private val childNameToIdMap: MutableMap<String, String> = mutableMapOf()

    private lateinit var parentResourceViewModel: ParentResourceViewModel
    private lateinit var resourceAdapter: LearningResourceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_view_resources)

        toolbar = findViewById(R.id.toolbarParentViewResources)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Learning Resources"

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) {
            currentUser = authManager.getLoggedInUser()

            if (currentUser == null || currentUser!!.role != User.ROLE_PARENT) {
                Toast.makeText(this@ParentViewResourcesActivity, "Access Denied: Only parents can view resources.", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            initializeViews()
            setupViewModel()
            setupSpinners()
            setupRecyclerView()
        }
    }

    private fun initializeViews() {
        spinnerSelectChild = findViewById(R.id.spinnerSelectChildResources)
        recyclerViewResources = findViewById(R.id.recyclerViewParentResources)
        textViewNoResources = findViewById(R.id.textViewNoResources)

        children = emptyList()
    }

    private fun setupViewModel() {
        val studentRepository = StudentRepository()
        val learningResourceRepository = LearningResourceRepository()
        val subjectRepository = SubjectRepository()
        val batchRepository = BatchRepository()

        val factory = ParentResourceViewModelFactory(
            studentRepository, learningResourceRepository, subjectRepository, batchRepository
        )
        parentResourceViewModel = ViewModelProvider(this, factory)[ParentResourceViewModel::class.java]

        currentUser?.id?.let { parentId ->
            parentResourceViewModel.setParentUserId(parentId)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        parentResourceViewModel.childrenOfParent.observe(this) { childrenList ->
            children = childrenList
            childNameToIdMap.clear()
            val childNames = mutableListOf<String>()

            if (children.isNotEmpty()) {
                children.forEach { student ->
                    val displayName = "${student.fullName} (Adm: ${student.admissionNumber})"
                    childNames.add(displayName)
                    childNameToIdMap[displayName] = student.id
                }
                textViewNoResources.visibility = View.GONE
                spinnerSelectChild.visibility = View.VISIBLE
                findViewById<TextView>(R.id.textViewSelectChildLabel).visibility = View.VISIBLE

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, childNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSelectChild.adapter = adapter

                if (children.isNotEmpty()) {
                    spinnerSelectChild.setSelection(0)
                    parentResourceViewModel.selectChildAndBatch(children[0].id, null)
                }

            } else {
                Log.d(TAG, "No children found for parent: ${currentUser?.fullName}")
                textViewNoResources.text = "No children linked to your account."
                textViewNoResources.visibility = View.VISIBLE
                spinnerSelectChild.visibility = View.GONE
                findViewById<TextView>(R.id.textViewSelectChildLabel).visibility = View.GONE
            }
        }

        parentResourceViewModel.filteredResources.observe(this) { resources ->
            resourceAdapter.setResources(resources)
            textViewNoResources.visibility = if (resources.isEmpty()) View.VISIBLE else View.GONE
            recyclerViewResources.visibility = if (resources.isEmpty()) View.GONE else View.VISIBLE
        }

        parentResourceViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                parentResourceViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupSpinners() {
        spinnerSelectChild.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedChildName = parent.getItemAtPosition(position) as String
                val selectedChild = children.find { "${it.fullName} (Adm: ${it.admissionNumber})" == selectedChildName }
                val studentId = selectedChild?.id

                parentResourceViewModel.selectChildAndBatch(studentId, null)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupRecyclerView() {
        resourceAdapter = LearningResourceAdapter(this, mutableListOf(), this)
        recyclerViewResources.layoutManager = LinearLayoutManager(this)
        recyclerViewResources.adapter = resourceAdapter
    }

    override fun onViewResource(resource: LearningResource) {
        val resourceUrl = resource.filePathOrUrl
        val resourceMimeType = resource.fileMimeType
        if (resourceUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Resource path is not available.", Toast.LENGTH_SHORT).show()
            return
        }
        openFile(resourceUrl, resourceMimeType)
    }

    private fun openFile(url: String, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No application found to open this file type or URL.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error opening URL: $url with MIME type $mimeType", e)
        }
    }

    override fun onEditResource(resource: LearningResource, position: Int) {
        Toast.makeText(this, "Edit not available in parent view.", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteResource(resource: LearningResource, position: Int) {
        Toast.makeText(this, "Delete not available in parent view.", Toast.LENGTH_SHORT).show()
    }

    // NEW: Implement onDownloadResource for ParentViewResourcesActivity
    override fun onDownloadResource(resource: LearningResource) {
        val downloadUrl = resource.filePathOrUrl
        // Fallback filename if URL doesn't provide one, and use MIME type extension if available
        val fileName = getFileNameFromUrl(downloadUrl) ?: (resource.title.replace(" ", "_") + "." + MimeTypeMap.getFileExtensionFromUrl(downloadUrl))

        if (downloadUrl.isEmpty()) {
            Toast.makeText(this, "Download URL is not available.", Toast.LENGTH_SHORT).show()
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

    // Helper function to extract filename from URL, or null if not possible
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
}