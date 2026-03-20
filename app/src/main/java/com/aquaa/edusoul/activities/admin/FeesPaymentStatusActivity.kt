// File: main/java/com/aquaa/edusoul/activities/admin/FeesPaymentStatusActivity.kt
package com.aquaa.edusoul.activities.admin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aquaa.edusoul.R
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.StudentFeeStatusAdapter
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentFeeStatus
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.FeesPaymentStatusViewModel
import com.aquaa.edusoul.viewmodels.FeesPaymentStatusViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FeesPaymentStatusActivity : BaseActivity() {

    private val TAG = "FeesPaymentStatusAct"

    private lateinit var recyclerViewFeesStatus: RecyclerView
    private lateinit var textViewNoFeesStatus: TextView
    private lateinit var adapter: StudentFeeStatusAdapter
    private lateinit var studentFeeStatusList: java.util.ArrayList<StudentFeeStatus>
    private lateinit var authManager: AuthManager
    private lateinit var currencyFormatter: NumberFormat

    private lateinit var editTextMonth: TextInputEditText
    private lateinit var editTextYear: TextInputEditText
    private lateinit var buttonFilter: MaterialButton
    private lateinit var selectedDateCalendar: Calendar
    private lateinit var yearMonthFormat: SimpleDateFormat

    private lateinit var fabAddFeePayment: FloatingActionButton
    private lateinit var paymentDateCalendar: Calendar

    private lateinit var allStudentsForDialog: java.util.ArrayList<Student>
    // Changed HashMap value type from Long to String
    private lateinit var studentNameToIdMap: java.util.HashMap<String, String>

    private lateinit var feesPaymentStatusViewModel: FeesPaymentStatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fees_payment_status)

        val toolbar = findViewById<Toolbar>(R.id.toolbarFeesPaymentStatus)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val studentRepository = StudentRepository()
        val batchRepository = BatchRepository()
        val feeStructureRepository = FeeStructureRepository()
        val feePaymentRepository = FeePaymentRepository()
        val userRepository = UserRepository()

        val factory = FeesPaymentStatusViewModelFactory(
            studentRepository,
            batchRepository,
            feeStructureRepository,
            feePaymentRepository,
            userRepository
        )
        feesPaymentStatusViewModel = ViewModelProvider(this, factory)[FeesPaymentStatusViewModel::class.java]

        authManager = AuthManager(this)

        recyclerViewFeesStatus = findViewById(R.id.recyclerViewFeesStatus)
        textViewNoFeesStatus = findViewById(R.id.textViewNoFeesStatus)

        studentFeeStatusList = java.util.ArrayList<StudentFeeStatus>()
        adapter = StudentFeeStatusAdapter(this, studentFeeStatusList)
        currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        // Initialized with String as value type
        studentNameToIdMap = java.util.HashMap()
        allStudentsForDialog = java.util.ArrayList()

        recyclerViewFeesStatus.layoutManager = LinearLayoutManager(this)
        recyclerViewFeesStatus.adapter = adapter

        editTextMonth = findViewById(R.id.editTextMonth)
        editTextYear = findViewById(R.id.editTextYear)
        buttonFilter = findViewById(R.id.buttonFilter)

        selectedDateCalendar = Calendar.getInstance()
        yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.US)

        setupMonthYearPickers()
        updateMonthYearDisplay()

        buttonFilter.setOnClickListener { loadFeeStatusData() }

        fabAddFeePayment = findViewById(R.id.fabAddFeePayment)
        fabAddFeePayment.setOnClickListener { showAddFeePaymentDialog() }

        feesPaymentStatusViewModel.feeStatusReport.observe(this) { reportList ->
            adapter.updateData(reportList)
            if (reportList.isEmpty()) {
                textViewNoFeesStatus.visibility = View.VISIBLE
                recyclerViewFeesStatus.visibility = View.GONE
                feesPaymentStatusViewModel.allStudentsForDialog.value?.let { students ->
                    allStudentsForDialog = java.util.ArrayList(students)
                    if (allStudentsForDialog.isEmpty()) {
                        textViewNoFeesStatus.text = "No students registered in the system."
                    } else {
                        textViewNoFeesStatus.text = "No fee payment records found for this period."
                    }
                }
            } else {
                textViewNoFeesStatus.visibility = View.GONE
                recyclerViewFeesStatus.visibility = View.VISIBLE
            }
        }

        feesPaymentStatusViewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                feesPaymentStatusViewModel.clearErrorMessage()
            }
        }

        loadFeeStatusData()
    }

    private fun setupMonthYearPickers() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, _ ->
            selectedDateCalendar.set(Calendar.YEAR, year)
            selectedDateCalendar.set(Calendar.MONTH, monthOfYear)
            updateMonthYearDisplay()
        }

        val datePickerDialog = DatePickerDialog(
            this, dateSetListener,
            selectedDateCalendar.get(Calendar.YEAR),
            selectedDateCalendar.get(Calendar.MONTH),
            selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        )

        editTextMonth.setOnClickListener { datePickerDialog.show() }
        editTextYear.setOnClickListener { datePickerDialog.show() }
    }

    private fun updateMonthYearDisplay() {
        val monthFormat = SimpleDateFormat("MMMM", Locale.US)
        val yearFormat = SimpleDateFormat("yyyy", Locale.US)
        editTextMonth.setText(monthFormat.format(selectedDateCalendar.time))
        editTextYear.setText(yearFormat.format(selectedDateCalendar.time))
        val textViewFeesStatusTitle = findViewById<TextView>(R.id.textViewFeesStatusTitle)
        textViewFeesStatusTitle.text = "Fees Payment Status for ${monthFormat.format(selectedDateCalendar.time)} ${yearFormat.format(selectedDateCalendar.time)}"
    }

    private fun loadFeeStatusData() {
        feesPaymentStatusViewModel.setSelectedPeriod(
            selectedDateCalendar.get(Calendar.YEAR),
            selectedDateCalendar.get(Calendar.MONTH)
        )
    }

    private fun showAddFeePaymentDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_fee_payment, null)
        builder.setView(dialogView)

        val autoCompleteStudent = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompleteStudent)
        val textViewExpectedAmountInDialog = dialogView.findViewById<TextView>(R.id.textViewExpectedAmountInDialog)
        val editTextAmountPaid = dialogView.findViewById<TextInputEditText>(R.id.editTextAmountPaid)
        val editTextPaymentDate = dialogView.findViewById<TextInputEditText>(R.id.editTextPaymentDate)
        val autoCompletePaymentMethod = dialogView.findViewById<AutoCompleteTextView>(R.id.autoCompletePaymentMethod)
        val editTextPaymentPeriod = dialogView.findViewById<TextInputEditText>(R.id.editTextPaymentPeriod)
        val editTextRemarks = dialogView.findViewById<TextInputEditText>(R.id.editTextRemarks)

        paymentDateCalendar = Calendar.getInstance()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        editTextPaymentDate.setText(dateFormatter.format(paymentDateCalendar.time))

        feesPaymentStatusViewModel.allStudentsForDialog.observe(this) { students ->
            allStudentsForDialog.clear()
            allStudentsForDialog.addAll(students)
            studentNameToIdMap.clear()
            val studentDisplayNames = java.util.ArrayList<String>()

            for (student in allStudentsForDialog) {
                val displayName = "${student.fullName} (Adm: ${student.admissionNumber})"
                studentDisplayNames.add(displayName)
                studentNameToIdMap[displayName] = student.id // Store String ID
            }

            val studentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, studentDisplayNames)
            autoCompleteStudent.setAdapter(studentAdapter)

            if (studentDisplayNames.isNotEmpty()) {
                autoCompleteStudent.setText(studentDisplayNames[0], false)
                // Pass String ID to ViewModel
                feesPaymentStatusViewModel.setDialogStudentAndPeriod(
                    studentNameToIdMap[studentDisplayNames[0]],
                    editTextPaymentPeriod.text.toString()
                )
            }
        }

        autoCompleteStudent.setOnItemClickListener { parent, _, position, _ ->
            val selectedDisplayName = parent.getItemAtPosition(position) as String
            val selectedStudentId = studentNameToIdMap[selectedDisplayName] // This is now String?
            // Pass String ID to ViewModel
            feesPaymentStatusViewModel.setDialogStudentAndPeriod(selectedStudentId, editTextPaymentPeriod.text.toString())
        }

        editTextPaymentPeriod.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val selectedStudentId = studentNameToIdMap[autoCompleteStudent.text.toString().trim()]
                // Pass String ID to ViewModel
                feesPaymentStatusViewModel.setDialogStudentAndPeriod(selectedStudentId, s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        feesPaymentStatusViewModel.dialogExpectedAmountInfo.observe(this) { (expectedAmount, totalPaidForPeriod) ->
            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            val displayText = StringBuilder()
            val remainingBalance = expectedAmount - totalPaidForPeriod

            displayText.append(String.format(Locale.US, "Expected Fee: %s", currencyFormatter.format(expectedAmount)))
            if (totalPaidForPeriod > 0) displayText.append(String.format(Locale.US, "\nAlready Paid: %s", currencyFormatter.format(totalPaidForPeriod)))
            if (remainingBalance > 0) displayText.append(String.format(Locale.US, "\nBalance Due: %s", currencyFormatter.format(remainingBalance)))

            editTextAmountPaid.setText(if (remainingBalance > 0) String.format(Locale.US, "%.2f", remainingBalance) else "0.00")
            textViewExpectedAmountInDialog.text = displayText.toString()
            textViewExpectedAmountInDialog.visibility = View.VISIBLE
        }

        val paymentMethods = arrayOf("Cash", "Bank Transfer", "Online Payment", "Cheque")
        val paymentMethodAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentMethods)
        autoCompletePaymentMethod.setAdapter(paymentMethodAdapter)

        editTextPaymentDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, monthOfYear, dayOfMonth ->
                    paymentDateCalendar.set(year, monthOfYear, dayOfMonth)
                    editTextPaymentDate.setText(dateFormatter.format(paymentDateCalendar.time))
                    val periodFormat = SimpleDateFormat("MMMM yyyy", Locale.US) // Corrected format for payment period
                    editTextPaymentPeriod.setText(periodFormat.format(paymentDateCalendar.time))
                    val selectedStudentId = studentNameToIdMap[autoCompleteStudent.text.toString().trim()]
                    // Pass String ID to ViewModel
                    feesPaymentStatusViewModel.setDialogStudentAndPeriod(selectedStudentId, editTextPaymentPeriod.text.toString())
                },
                paymentDateCalendar.get(Calendar.YEAR),
                paymentDateCalendar.get(Calendar.MONTH),
                paymentDateCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val periodFormat = SimpleDateFormat("MMMM yyyy", Locale.US) // Corrected format for payment period on init
        editTextPaymentPeriod.setText(periodFormat.format(paymentDateCalendar.time))

        builder.setTitle("Record New Fee Payment")
        builder.setPositiveButton("Record Payment", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val studentSelection = autoCompleteStudent.text.toString().trim()
            val amountPaidStr = editTextAmountPaid.text.toString().trim()
            val paymentDate = editTextPaymentDate.text.toString().trim()
            val paymentMethod = autoCompletePaymentMethod.text.toString().trim()
            val paymentPeriod = editTextPaymentPeriod.text.toString().trim()
            val remarks = editTextRemarks.text.toString().trim()

            // Corrected: Default studentId to empty string if not found, as it's now String
            val studentId = studentNameToIdMap[studentSelection] ?: ""

            // Corrected: Check if studentId is blank instead of -1L
            if (studentId.isBlank() || amountPaidStr.isEmpty() || paymentDate.isEmpty() || paymentMethod.isEmpty() || paymentPeriod.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountPaid: Double
            try {
                amountPaid = amountPaidStr.toDouble()
                if (amountPaid <= 0) {
                    editTextAmountPaid.error = "Amount must be positive."
                    return@setOnClickListener
                }
            } catch (e: NumberFormatException) {
                editTextAmountPaid.error = "Invalid amount format."
                return@setOnClickListener
            }

            val expectedAmountForPeriod = feesPaymentStatusViewModel.dialogExpectedAmountInfo.value?.first ?: 0.0
            val totalPaidBeforeThisPayment = feesPaymentStatusViewModel.dialogExpectedAmountInfo.value?.second ?: 0.0
            val newTotalPaidForPeriod = totalPaidBeforeThisPayment + amountPaid

            lifecycleScope.launch {
                // currentUserId is String? from AuthManager.getLoggedInUser()
                val currentUserId = authManager.getLoggedInUser()?.id

                if (expectedAmountForPeriod > 0 && newTotalPaidForPeriod > expectedAmountForPeriod) {
                    AlertDialog.Builder(this@FeesPaymentStatusActivity)
                        .setTitle("Overpayment Detected")
                        .setMessage("This payment will result in an overpayment. Do you want to proceed?")
                        .setPositiveButton("Record as Overpaid") { _, _ ->
                            // Pass String IDs to ViewModel
                            feesPaymentStatusViewModel.recordFeePayment(studentId, amountPaid, paymentDate, paymentMethod, paymentPeriod, remarks, currentUserId)
                            alertDialog.dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // Pass String IDs to ViewModel
                    feesPaymentStatusViewModel.recordFeePayment(studentId, amountPaid, paymentDate, paymentMethod, paymentPeriod, remarks, currentUserId)
                    alertDialog.dismiss()
                }
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