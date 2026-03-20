// File: main/java/com/aquaa/edusoul/activities/admin/ManageBatchesActivity.kt
package com.aquaa.edusoul.activities.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.BatchAdapter
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.StudentBatchLinkRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.viewmodels.ManageBatchesViewModel
import com.aquaa.edusoul.viewmodels.ManageBatchesViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.ArrayList

class ManageBatchesActivity : BaseActivity(), BatchAdapter.OnBatchActionsListener {

    private val TAG = "ManageBatchesActivity"

    private lateinit var recyclerViewBatches: RecyclerView
    private lateinit var fabAddBatch: FloatingActionButton
    private lateinit var textViewNoBatches: TextView
    private lateinit var batchAdapter: BatchAdapter
    private lateinit var batchList: MutableList<Batch>

    private lateinit var selectFeeStructureLauncher: ActivityResultLauncher<Intent>
    private lateinit var textViewSelectedFeeStructureName: TextView

    private lateinit var manageBatchesViewModel: ManageBatchesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_batches)

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageBatches)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val batchRepository = BatchRepository()
        val feeStructureRepository = FeeStructureRepository()
        val studentRepository = StudentRepository()
        val studentBatchLinkRepository = StudentBatchLinkRepository()
        val feePaymentRepository = FeePaymentRepository()
        val factory = ManageBatchesViewModelFactory(
            batchRepository,
            feeStructureRepository,
            studentRepository,
            studentBatchLinkRepository,
            feePaymentRepository
        )
        manageBatchesViewModel = ViewModelProvider(this, factory)[ManageBatchesViewModel::class.java]

        recyclerViewBatches = findViewById(R.id.recyclerViewBatches)
        fabAddBatch = findViewById(R.id.fabAddBatch)
        textViewNoBatches = findViewById(R.id.textViewNoBatches)

        batchList = mutableListOf()
        // Corrected lambda parameter type for feeStructureId to String?
        batchAdapter = BatchAdapter(this, ArrayList(batchList), this) { feeStructureId: String? ->
            if (!feeStructureId.isNullOrBlank()) {
                runBlocking { // runBlocking is used here because the lambda is synchronous for the adapter
                    manageBatchesViewModel.getFeeStructureTitleById(feeStructureId) ?: "N/A"
                }
            } else {
                "N/A"
            }
        }

        recyclerViewBatches.layoutManager = LinearLayoutManager(this)
        recyclerViewBatches.adapter = batchAdapter

        fabAddBatch.setOnClickListener { showAddEditBatchDialog(null) }

        selectFeeStructureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Correctly get String extra
                val selectedFeeStructureId = result.data!!.getStringExtra("SELECTED_FEE_STRUCTURE_ID")
                val selectedFeeStructureTitle = result.data!!.getStringExtra("SELECTED_FEE_STRUCTURE_TITLE")
                Log.d(TAG, "Received Fee ID: $selectedFeeStructureId, Title: $selectedFeeStructureTitle")
                manageBatchesViewModel.setTemporarySelectedFeeStructure(selectedFeeStructureId, selectedFeeStructureTitle)
            } else {
                Log.d(TAG, "Fee structure selection cancelled or failed.")
                manageBatchesViewModel.clearTemporarySelectedFeeStructure()
            }
        }

        manageBatchesViewModel.allBatches.observe(this) { batchesFromDb ->
            batchAdapter.setBatches(batchesFromDb)
            textViewNoBatches.visibility = if (batchesFromDb.isEmpty()) View.VISIBLE else View.GONE
            recyclerViewBatches.visibility = if (batchesFromDb.isEmpty()) View.GONE else View.VISIBLE
        }

        manageBatchesViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                manageBatchesViewModel.clearErrorMessage()
            }
        }
    }

    private fun showAddEditBatchDialog(batchToEdit: Batch?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_batch, null)
        builder.setView(dialogView)

        val tilBatchName = dialogView.findViewById<TextInputLayout>(R.id.tilBatchNameDialog)
        val editTextBatchName = dialogView.findViewById<EditText>(R.id.editTextBatchNameDialog)
        val tilGradeLevel = dialogView.findViewById<TextInputLayout>(R.id.tilGradeLevelDialog)
        val editTextGradeLevel = dialogView.findViewById<EditText>(R.id.editTextGradeLevelDialog)
        val tilAcademicYear = dialogView.findViewById<TextInputLayout>(R.id.tilAcademicYearDialog)
        val editTextAcademicYear = dialogView.findViewById<EditText>(R.id.editTextAcademicYearDialog)
        val tilBatchDescription = dialogView.findViewById<TextInputLayout>(R.id.tilBatchDescriptionDialog)
        val editTextBatchDescription = dialogView.findViewById<EditText>(R.id.editTextBatchDescriptionDialog)
        val checkBoxAutoEnroll = dialogView.findViewById<CheckBox>(R.id.checkBoxAutoEnrollStudents)

        val buttonSetFeeStructure = dialogView.findViewById<MaterialButton>(R.id.buttonSetFeeStructure)
        textViewSelectedFeeStructureName = dialogView.findViewById(R.id.textViewSelectedFeeStructureName)

        var currentFeeStructureId: String? = null // Default for new batch or no selection
        if (batchToEdit != null && !batchToEdit.feeStructureId.isNullOrBlank()) {
            currentFeeStructureId = batchToEdit.feeStructureId
            lifecycleScope.launch { // Launch coroutine to get fee structure title
                val feeTitle = manageBatchesViewModel.getFeeStructureTitleById(currentFeeStructureId)
                textViewSelectedFeeStructureName.text = "Selected Fee: ${feeTitle ?: "N/A"}"
                textViewSelectedFeeStructureName.visibility = View.VISIBLE
                manageBatchesViewModel.setTemporarySelectedFeeStructure(currentFeeStructureId, feeTitle)
            }
        } else {
            // Check ViewModel's temporary selected fee structure if adding a new batch
            val tempFeeId = manageBatchesViewModel.selectedFeeStructureIdForDialog.value
            val tempFeeTitle = manageBatchesViewModel.selectedFeeStructureTitleForDialog.value
            if (!tempFeeId.isNullOrBlank() && !tempFeeTitle.isNullOrBlank()) {
                currentFeeStructureId = tempFeeId
                textViewSelectedFeeStructureName.text = "Selected Fee: $tempFeeTitle"
                textViewSelectedFeeStructureName.visibility = View.VISIBLE
            } else {
                textViewSelectedFeeStructureName.text = "No Fee Structure Selected"
                textViewSelectedFeeStructureName.visibility = View.GONE
                manageBatchesViewModel.clearTemporarySelectedFeeStructure()
            }
        }

        // Observe ViewModel's temporary selected fee structure for dialog updates
        manageBatchesViewModel.selectedFeeStructureTitleForDialog.observe(this) { title ->
            if (title != null) {
                textViewSelectedFeeStructureName.text = "Selected Fee: $title"
                textViewSelectedFeeStructureName.visibility = View.VISIBLE
            } else {
                textViewSelectedFeeStructureName.text = "No Fee Structure Selected"
                textViewSelectedFeeStructureName.visibility = View.GONE
            }
        }

        if (batchToEdit != null) {
            editTextBatchName.setText(batchToEdit.batchName)
            editTextGradeLevel.setText(batchToEdit.gradeLevel)
            editTextAcademicYear.setText(batchToEdit.academicYear)
            editTextBatchDescription.setText(batchToEdit.description)
            checkBoxAutoEnroll.visibility = View.GONE
        } else {
            checkBoxAutoEnroll.visibility = View.VISIBLE
            checkBoxAutoEnroll.isChecked = false
        }

        // Set OnClickListener for the new button
        buttonSetFeeStructure.setOnClickListener {
            val intent = Intent(this, SelectFeeStructureActivity::class.java)
            // Pass String ID as extra (if not null)
            intent.putExtra("CURRENT_SELECTED_FEE_ID", currentFeeStructureId)
            selectFeeStructureLauncher.launch(intent)
        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = editTextBatchName.text.toString().trim()
            val grade = editTextGradeLevel.text.toString().trim()
            val year = editTextAcademicYear.text.toString().trim()
            val description = editTextBatchDescription.text.toString().trim()
            val autoEnroll = checkBoxAutoEnroll.isChecked

            // selectedFeeStructureId is already String? from ViewModel
            val selectedFeeStructureId = manageBatchesViewModel.selectedFeeStructureIdForDialog.value

            tilBatchName.error = null
            if (name.isEmpty()) {
                tilBatchName.error = "Batch name cannot be empty"
                return@setOnClickListener
            }
            if (autoEnroll && TextUtils.isEmpty(grade)) {
                tilGradeLevel.error = "Grade level is required for auto-enrolment"
                Toast.makeText(this, "Grade level is required for auto-enrolment.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val batch = Batch(
                // For new batches, ID will be empty string, Firestore assigns actual ID upon insertion
                id = batchToEdit?.id ?: "", // Ensure ID is String
                batchName = name,
                gradeLevel = if (grade.isBlank()) null else grade,
                academicYear = if (year.isBlank()) null else year,
                description = if (description.isBlank()) null else description,
                feeStructureId = selectedFeeStructureId
            )

            if (batchToEdit == null) { // Adding new batch
                Log.i(TAG, "showAddEditBatchDialog: Attempting to add new batch: \"$name\"")
                manageBatchesViewModel.addBatch(batch, autoEnroll) // Pass autoEnroll flag
            } else { // Editing existing batch
                // batch.id is already set above from batchToEdit.id which is a String
                Log.i(TAG, "showAddEditBatchDialog: Attempting to update batch ID: ${batch.id}, New Name: \"$name\"")
                manageBatchesViewModel.updateBatch(batch)
            }
            alertDialog.dismiss()
        }
    }

    override fun onEditBatch(batch: Batch, position: Int) {
        Log.d(TAG, "onEditBatch: Editing batch: ${batch.batchName}")
        showAddEditBatchDialog(batch)
    }

    override fun onDeleteBatch(batch: Batch, position: Int) {
        Log.d(TAG, "onDeleteBatch: Attempting to delete batch: ${batch.batchName}")
        AlertDialog.Builder(this)
            .setTitle("Delete Batch")
            .setMessage("Are you sure you want to delete '${batch.batchName}'? This will also remove related student enrollments, teacher assignments, and scheduled class sessions for this batch.")
            .setPositiveButton("Delete") { _, _ ->
                manageBatchesViewModel.deleteBatch(batch.id) // Pass String ID to ViewModel
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // New method implementation for the listener
    override fun onManageEnrollments(batch: Batch, position: Int) {
        Log.d(TAG, "onManageEnrollments: Clicked for batch: ${batch.batchName} (ID: ${batch.id})")
        val intent = Intent(this, BatchEnrollmentActivity::class.java)
        intent.putExtra("BATCH_ID", batch.id) // Pass String ID to Intent
        intent.putExtra("BATCH_NAME", batch.batchName) // Pass name for display in the next activity
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