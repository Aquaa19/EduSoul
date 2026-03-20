package com.aquaa.edusoul.activities.admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.FeeStructureAdapter
import com.aquaa.edusoul.models.FeeStructure
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.viewmodels.ManageFeeStructuresViewModel
import com.aquaa.edusoul.viewmodels.ManageFeeStructuresViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Arrays
import java.util.Locale

class ManageFeeStructuresActivity : BaseActivity(), FeeStructureAdapter.OnItemClickListener { // Corrected interface

    private val TAG = "ManageFeeStructuresAct"

    private lateinit var recyclerViewFeeStructures: RecyclerView
    private lateinit var fabAddFeeStructure: FloatingActionButton
    private lateinit var textViewNoFeeStructures: TextView
    private lateinit var feeStructureAdapter: FeeStructureAdapter
    private lateinit var feeStructureList: MutableList<FeeStructure>

    private lateinit var manageFeeStructuresViewModel: ManageFeeStructuresViewModel

    private val PAYMENT_FREQUENCIES = arrayOf("Monthly", "Quarterly", "Annually", "One-time")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_fee_structures)

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageFeeStructures)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val feeStructureRepository = FeeStructureRepository()
        val factory = ManageFeeStructuresViewModelFactory(feeStructureRepository)
        manageFeeStructuresViewModel = ViewModelProvider(this, factory)[ManageFeeStructuresViewModel::class.java]

        recyclerViewFeeStructures = findViewById(R.id.recyclerViewFeeStructures)
        fabAddFeeStructure = findViewById(R.id.fabAddFeeStructure)
        textViewNoFeeStructures = findViewById(R.id.textViewNoFeeStructures)

        feeStructureList = mutableListOf()
        feeStructureAdapter = FeeStructureAdapter(this, feeStructureList, this) // 'this' is now a valid listener

        recyclerViewFeeStructures.layoutManager = LinearLayoutManager(this)
        recyclerViewFeeStructures.adapter = feeStructureAdapter

        fabAddFeeStructure.setOnClickListener { showAddEditFeeStructureDialog(null) }

        manageFeeStructuresViewModel.allFeeStructures.observe(this) { feesFromDb ->
            feeStructureAdapter.setFeeStructures(feesFromDb)
            textViewNoFeeStructures.visibility = if (feesFromDb.isEmpty()) View.VISIBLE else View.GONE
        }

        manageFeeStructuresViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                manageFeeStructuresViewModel.clearErrorMessage()
            }
        }
    }

    private fun showAddEditFeeStructureDialog(feeStructureToEdit: FeeStructure?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_fee_structure, null)
        builder.setView(dialogView)

        val tilFeeTitle = dialogView.findViewById<TextInputLayout>(R.id.tilFeeTitleDialog)
        val editTextFeeTitle = dialogView.findViewById<TextInputEditText>(R.id.editTextFeeTitleDialog)
        val editTextFeeDescription = dialogView.findViewById<TextInputEditText>(R.id.editTextFeeDescriptionDialog)
        val tilFeeAmount = dialogView.findViewById<TextInputLayout>(R.id.tilFeeAmountDialog)
        val editTextFeeAmount = dialogView.findViewById<TextInputEditText>(R.id.editTextFeeAmountDialog)
        val tilFeeFrequency = dialogView.findViewById<TextInputLayout>(R.id.tilFeeFrequencyDialog)
        val autoCompleteFeeFrequency = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteFeeFrequency)

        val frequencyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, PAYMENT_FREQUENCIES)
        autoCompleteFeeFrequency.setAdapter(frequencyAdapter)

        builder.setTitle(if (feeStructureToEdit == null) "Add New Fee Structure" else "Edit Fee Structure")

        if (feeStructureToEdit != null) {
            editTextFeeTitle.setText(feeStructureToEdit.title)
            editTextFeeDescription.setText(feeStructureToEdit.description)
            editTextFeeAmount.setText(String.format(Locale.US, "%.2f", feeStructureToEdit.amount))
            autoCompleteFeeFrequency.setText(feeStructureToEdit.duration, false) // Corrected to 'duration'
        }

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = editTextFeeTitle.text.toString().trim()
            val description = editTextFeeDescription.text.toString().trim()
            val amountStr = editTextFeeAmount.text.toString().trim()
            val duration = autoCompleteFeeFrequency.text.toString().trim() // Corrected to 'duration'

            if (title.isEmpty() || amountStr.isEmpty() || duration.isEmpty()) { // Corrected to 'duration'
                Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount: Double
            try {
                amount = amountStr.toDouble()
            } catch (e: NumberFormatException) {
                tilFeeAmount.error = "Invalid amount"
                return@setOnClickListener
            }

            val feeStructure = FeeStructure(
                id = feeStructureToEdit?.id ?: "",
                title = title,
                description = if (description.isEmpty()) null else description,
                amount = amount,
                duration = duration // Corrected to 'duration'
            )

            if (feeStructureToEdit == null) {
                manageFeeStructuresViewModel.addFeeStructure(feeStructure)
            } else {
                manageFeeStructuresViewModel.updateFeeStructure(feeStructure)
            }
            alertDialog.dismiss()
        }
    }

    override fun onItemClick(feeStructure: FeeStructure) {
        // Not used in this activity as it's for managing, not selection
        // Log.d(TAG, "Item clicked: ${feeStructure.title}")
    }

    override fun onEditClick(feeStructure: FeeStructure) { // Renamed method
        showAddEditFeeStructureDialog(feeStructure)
    }

    override fun onDeleteClick(feeStructure: FeeStructure) { // Renamed method
        AlertDialog.Builder(this)
            .setTitle("Delete Fee Structure")
            .setMessage("Are you sure you want to delete '${feeStructure.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                manageFeeStructuresViewModel.deleteFeeStructure(feeStructure.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}