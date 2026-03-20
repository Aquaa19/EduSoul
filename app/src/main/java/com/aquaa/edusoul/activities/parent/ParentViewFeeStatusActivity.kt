// File: main/java/com/aquaa/edusoul/activities/parent/ParentViewFeeStatusActivity.kt
package com.aquaa.edusoul.activities.parent

import ParentFeePaymentDetails
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.adapters.ParentFeePaymentAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.ParentViewFeeStatusViewModel
import com.aquaa.edusoul.viewmodels.ParentViewFeeStatusViewModelFactory
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch // Import launch
import kotlinx.coroutines.withContext // Import withContext

class ParentViewFeeStatusActivity : AppCompatActivity(), ParentFeePaymentAdapter.OnItemClickListener {

    private val TAG = "ParentViewFeeStatusActivity"

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewFeePayments: RecyclerView
    private lateinit var textViewNoPayments: TextView
    private lateinit var authManager: AuthManager
    private var currentParent: User? = null
    private lateinit var feePaymentAdapter: ParentFeePaymentAdapter
    private lateinit var parentViewFeeStatusViewModel: ParentViewFeeStatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_view_fee_status)

        toolbar = findViewById(R.id.toolbarParentViewFeeStatus)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Fee Status & History"

        authManager = AuthManager(this)

        lifecycleScope.launch(Dispatchers.Main) { // Launch a coroutine on the main dispatcher
            currentParent = authManager.getLoggedInUser() // Call suspend function

            if (currentParent == null || currentParent!!.role != User.ROLE_PARENT) { // Check user role
                Toast.makeText(this@ParentViewFeeStatusActivity, "Access Denied: Only parents can view fee status.", Toast.LENGTH_LONG).show()
                finish()
                return@launch // Exit the coroutine
            }

            initializeViews()
            setupViewModel()
            setupRecyclerView()
        }
    }

    private fun initializeViews() {
        recyclerViewFeePayments = findViewById(R.id.recyclerViewParentFeePayments)
        textViewNoPayments = findViewById(R.id.textViewParentNoPayments)
    }

    private fun setupViewModel() {
        // AppDatabase.getDatabase is not used for DAOs directly
        val studentRepository = StudentRepository()
        val feePaymentRepository = FeePaymentRepository()
        val feeStructureRepository = FeeStructureRepository()
        val userRepository = UserRepository()

        val factory = ParentViewFeeStatusViewModelFactory(studentRepository, feePaymentRepository, feeStructureRepository, userRepository)
        parentViewFeeStatusViewModel = ViewModelProvider(this, factory)[ParentViewFeeStatusViewModel::class.java]

        currentParent?.id?.let { parentId ->
            parentViewFeeStatusViewModel.setParentUserId(parentId) // parentId is String
        }

        parentViewFeeStatusViewModel.parentFeePaymentDetailsList.observe(this) { payments ->
            feePaymentAdapter.updateData(payments)
            textViewNoPayments.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
        }

        parentViewFeeStatusViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                parentViewFeeStatusViewModel.clearErrorMessage()
            }
        }
    }

    private fun setupRecyclerView() {
        feePaymentAdapter = ParentFeePaymentAdapter(this, mutableListOf(), this)
        recyclerViewFeePayments.layoutManager = LinearLayoutManager(this)
        recyclerViewFeePayments.adapter = feePaymentAdapter
    }

    override fun onFeePaymentCardClick(paymentDetails: ParentFeePaymentDetails) {
        Toast.makeText(
            this,
            "Clicked on Payment for: ${paymentDetails.studentName} - Amount: ₹${paymentDetails.amountPaid}",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}