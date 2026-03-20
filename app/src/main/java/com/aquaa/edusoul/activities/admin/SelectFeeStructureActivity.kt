package com.aquaa.edusoul.activities.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
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
import com.aquaa.edusoul.viewmodels.SelectFeeStructureViewModel
import com.aquaa.edusoul.viewmodels.SelectFeeStructureViewModelFactory
import java.util.ArrayList

/*
 * SelectFeeStructureActivity: Allows an admin to select a fee structure.
 * Migrated to Kotlin and MVVM.
 */
class SelectFeeStructureActivity : BaseActivity(), FeeStructureAdapter.OnItemClickListener { // Corrected interface

    private val TAG = "SelectFeeStructureAct"

    private lateinit var recyclerViewFeeStructuresSelection: RecyclerView
    private lateinit var textViewNoFeeStructures: TextView
    private lateinit var feeStructureAdapter: FeeStructureAdapter
    private lateinit var feeStructureList: MutableList<FeeStructure>

    private lateinit var selectFeeStructureViewModel: SelectFeeStructureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_fee_structure)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSelectFeeStructure)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        val feeStructureRepository = FeeStructureRepository()
        val factory = SelectFeeStructureViewModelFactory(feeStructureRepository)
        selectFeeStructureViewModel = ViewModelProvider(this, factory)[SelectFeeStructureViewModel::class.java]

        recyclerViewFeeStructuresSelection = findViewById(R.id.recyclerViewFeeStructuresSelection)
        textViewNoFeeStructures = findViewById(R.id.textViewNoFeeStructures)

        feeStructureList = ArrayList()
        // Pass 'this' as the listener, as this activity handles item clicks for selection
        feeStructureAdapter = FeeStructureAdapter(this, feeStructureList, this, isSelectionMode = true)


        recyclerViewFeeStructuresSelection.layoutManager = LinearLayoutManager(this)
        recyclerViewFeeStructuresSelection.adapter = feeStructureAdapter

        selectFeeStructureViewModel.allFeeStructures.observe(this) { feeStructures ->
            feeStructureList.clear()
            if (feeStructures != null && feeStructures.isNotEmpty()) {
                feeStructureList.addAll(feeStructures)
                textViewNoFeeStructures.visibility = View.GONE
                recyclerViewFeeStructuresSelection.visibility = View.VISIBLE
                Log.d(TAG, "Loaded ${feeStructureList.size} fee structures.")
            } else {
                textViewNoFeeStructures.visibility = View.VISIBLE
                recyclerViewFeeStructuresSelection.visibility = View.GONE
                Log.d(TAG, "No fee structures found.")
            }
            feeStructureAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClick(feeStructure: FeeStructure) { // Renamed method to match interface
        // When a fee structure is selected, send its ID back to ManageBatchesActivity
        Log.d(TAG, "Fee Structure selected: ${feeStructure.title} (ID: ${feeStructure.id})")
        val resultIntent = Intent()
        resultIntent.putExtra("SELECTED_FEE_STRUCTURE_ID", feeStructure.id)
        resultIntent.putExtra("SELECTED_FEE_STRUCTURE_TITLE", feeStructure.title)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onEditClick(feeStructure: FeeStructure) { // Renamed method
        // Not used in this activity as it's for selection, not editing
    }

    override fun onDeleteClick(feeStructure: FeeStructure) { // Renamed method
        // Not used in this activity as it's for selection, not deletion
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}