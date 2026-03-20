// File: main/java/com/aquaa/edusoul/activities/GlobalSearchActivity.kt
package com.aquaa.edusoul.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.GlobalSearchAdapter
// Removed unused import: import com.aquaa.edusoul.database.AppDatabase
import com.aquaa.edusoul.models.SearchItem
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.GlobalSearchViewModel
import com.aquaa.edusoul.viewmodels.GlobalSearchViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.MutableStateFlow

class GlobalSearchActivity : BaseActivity(), GlobalSearchAdapter.OnItemClickListener {

    private val TAG = "GlobalSearchActivity"

    private lateinit var editTextGlobalSearch: TextInputEditText
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var buttonAdvancedFilter: MaterialButton
    private lateinit var recyclerViewSearchResults: RecyclerView
    private lateinit var textViewSearchResultsTitle: TextView
    private lateinit var textViewNoSearchResults: TextView
    private lateinit var progressBarSearch: ProgressBar

    private lateinit var globalSearchViewModel: GlobalSearchViewModel
    private lateinit var globalSearchAdapter: GlobalSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_search)

        val toolbar: Toolbar = findViewById(R.id.toolbarGlobalSearch)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeViews()
        setupViewModel()
        setupRecyclerView()
        setupEventListeners()

    }

    private fun initializeViews() {
        editTextGlobalSearch = findViewById(R.id.editTextGlobalSearch)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        buttonAdvancedFilter = findViewById(R.id.buttonAdvancedFilter)
        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults)
        textViewSearchResultsTitle = findViewById(R.id.textViewSearchResultsTitle)
        textViewNoSearchResults = findViewById(R.id.textViewNoSearchResults)
        progressBarSearch = findViewById(R.id.progressBarSearch)
    }

    private fun setupViewModel() {
        val studentRepository = StudentRepository()
        val userRepository = UserRepository()
        val subjectRepository = SubjectRepository()
        val batchRepository = BatchRepository()

        val factory = GlobalSearchViewModelFactory(
            studentRepository,
            userRepository,
            subjectRepository,
            batchRepository
        )
        globalSearchViewModel = ViewModelProvider(this, factory)[GlobalSearchViewModel::class.java]

        globalSearchViewModel.searchResults.observe(this) { results ->
            globalSearchAdapter.updateData(results)
            if (results.isEmpty()) {
                val currentQuery = editTextGlobalSearch.text.toString().trim()
                val selectedCategories = getSelectedFilterCategories()
                if (currentQuery.isBlank() && selectedCategories.contains("All") && selectedCategories.size == 1) {
                    textViewNoSearchResults.text = "Type to search or select categories."
                } else if (currentQuery.isBlank() && selectedCategories.isEmpty()) {
                    textViewNoSearchResults.text = "Type to search or select categories."
                }
                else {
                    textViewNoSearchResults.text = "No results found for \"$currentQuery\" in ${getSelectedFilterCategories().joinToString()}."
                }
                textViewNoSearchResults.visibility = View.VISIBLE
                recyclerViewSearchResults.visibility = View.GONE
                textViewSearchResultsTitle.visibility = View.GONE
            } else {
                textViewNoSearchResults.visibility = View.GONE
                recyclerViewSearchResults.visibility = View.VISIBLE
                textViewSearchResultsTitle.visibility = View.VISIBLE
            }
        }

        globalSearchViewModel.isLoading.observe(this) { isLoading ->
            progressBarSearch.visibility = if (isLoading) View.VISIBLE else View.GONE
            recyclerViewSearchResults.visibility = if (isLoading) View.GONE else recyclerViewSearchResults.visibility
            textViewNoSearchResults.visibility = if (isLoading) View.GONE else textViewNoSearchResults.visibility
            textViewSearchResultsTitle.visibility = if (isLoading) View.GONE else textViewSearchResultsTitle.visibility
        }

        globalSearchViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                globalSearchViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupRecyclerView() {
        globalSearchAdapter = GlobalSearchAdapter(this, mutableListOf(), this)
        recyclerViewSearchResults.layoutManager = LinearLayoutManager(this)
        recyclerViewSearchResults.adapter = globalSearchAdapter
    }

    private fun setupEventListeners() {
        editTextGlobalSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                globalSearchViewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedCategories = getSelectedFilterCategories()
            globalSearchViewModel.setSelectedCategories(selectedCategories)
        }

        buttonAdvancedFilter.setOnClickListener {
            Toast.makeText(this, "Advanced filter options coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedFilterCategories(): List<String> {
        val selectedCategories = mutableListOf<String>()
        val checkedChipIds = chipGroupCategories.checkedChipIds
        val allChip = findViewById<com.google.android.material.chip.Chip>(R.id.chipAll)
        if (allChip != null && allChip.isChecked) {
            return listOf("All")
        }

        for (id in checkedChipIds) {
            val chip = findViewById<com.google.android.material.chip.Chip>(id)
            chip?.let {
                if (it.id != R.id.chipAll) {
                    selectedCategories.add(it.text.toString())
                }
            }
        }
        return selectedCategories
    }

    override fun onSearchResultClick(item: SearchItem) {
        when (item) {
            is SearchItem.StudentItem -> {
                Toast.makeText(this, "Clicked Student: ${item.student.fullName}", Toast.LENGTH_SHORT).show()
            }
            is SearchItem.UserItem -> {
                Toast.makeText(this, "Clicked User: ${item.user.fullName} (${item.user.role})", Toast.LENGTH_SHORT).show()
            }
            is SearchItem.SubjectItem -> {
                Toast.makeText(this, "Clicked Subject: ${item.subject.subjectName}", Toast.LENGTH_SHORT).show()
            }
            is SearchItem.BatchItem -> {
                Toast.makeText(this, "Clicked Batch: ${item.batch.batchName}", Toast.LENGTH_SHORT).show()
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
}