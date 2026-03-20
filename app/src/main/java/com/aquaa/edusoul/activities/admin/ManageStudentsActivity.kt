// File: EduSoul/app/src/main/java/com/aquaa/edusoul/activities/admin/ManageStudentsActivity.kt
package com.aquaa.edusoul.activities.admin

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
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
import com.aquaa.edusoul.activities.BaseActivity
import com.aquaa.edusoul.adapters.StudentAdapter
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.User
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import com.aquaa.edusoul.viewmodels.StudentViewModel
import com.aquaa.edusoul.viewmodels.StudentViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.ArrayList
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.android.material.button.MaterialButton
import java.io.File
import android.os.Environment

class ManageStudentsActivity : BaseActivity(), StudentAdapter.OnStudentActionsListener {

    private val TAG = "ManageStudentsActivity"
    private lateinit var recyclerViewStudents: RecyclerView
    private lateinit var fabAddStudent: FloatingActionButton
    private lateinit var textViewNoStudents: TextView
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var studentList: MutableList<Student>
    private lateinit var parentUserList: MutableList<User>
    private lateinit var selectedDOBCalendar: Calendar
    private lateinit var studentViewModel: StudentViewModel

    private lateinit var createDocumentLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_students)

        val toolbar = findViewById<Toolbar>(R.id.toolbarManageStudents)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val studentRepository = StudentRepository()
        val userRepository = UserRepository()
        val factory = StudentViewModelFactory(studentRepository, userRepository)
        studentViewModel = ViewModelProvider(this, factory).get(StudentViewModel::class.java)

        recyclerViewStudents = findViewById(R.id.recyclerViewStudents)
        fabAddStudent = findViewById(R.id.fabAddStudent)
        textViewNoStudents = findViewById(R.id.textViewNoStudents)

        studentList = ArrayList()
        parentUserList = ArrayList()
        selectedDOBCalendar = Calendar.getInstance()

        studentAdapter = StudentAdapter(this, studentList, this)
        recyclerViewStudents.layoutManager = LinearLayoutManager(this)
        recyclerViewStudents.adapter = studentAdapter

        fabAddStudent.setOnClickListener { _ ->
            showAddEditStudentDialog(null)
        }

        createDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    writeStudentsToXLSX(uri)
                } else {
                    showToast("Failed to get file location for export.")
                }
            } else {
                showToast("Export cancelled or failed.")
            }
        }

        studentViewModel.allStudents.observe(this) { students ->
            Log.d(TAG, "Students LiveData updated. Size: ${students.size}")
            studentList.clear()
            studentList.addAll(students)
            studentAdapter.notifyDataSetChanged()
            updateEmptyState()
        }

        studentViewModel.parentUsers.observe(this) { parents ->
            Log.d(TAG, "Parent Users LiveData updated. Size: ${parents.size}")
            parentUserList.clear()
            parentUserList.addAll(parents)
        }

        studentViewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading state: $isLoading")
            // Optionally show a progress indicator here if isLoading is true
        }

        studentViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                showToast(it) // Using showToast from BaseActivity
                Log.e(TAG, "Error/Success: $it")
                studentViewModel.clearErrorMessage()
            }
        }
    }

    private fun updateEmptyState() {
        if (studentList.isEmpty()) {
            textViewNoStudents.visibility = View.VISIBLE
            recyclerViewStudents.visibility = View.GONE
        } else {
            textViewNoStudents.visibility = View.GONE
            recyclerViewStudents.visibility = View.VISIBLE
        }
    }

    private fun showAddEditStudentDialog(studentToEdit: Student?) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_edit_student, null)
        builder.setView(dialogView)

        val tilStudentName = dialogView.findViewById<TextInputLayout>(R.id.tilStudentNameDialog)
        val editTextStudentName = dialogView.findViewById<TextInputEditText>(R.id.editTextStudentNameDialog)
        val tilAdmissionNumber = dialogView.findViewById<TextInputLayout>(R.id.tilAdmissionNumberDialog)
        val editTextAdmissionNumber = dialogView.findViewById<TextInputEditText>(R.id.editTextAdmissionNumberDialog)
        val tilGrade = dialogView.findViewById<TextInputLayout>(R.id.tilGradeDialog)
        val editTextGrade = dialogView.findViewById<TextInputEditText>(R.id.editTextGradeDialog)
        val editTextDOB = dialogView.findViewById<TextInputEditText>(R.id.editTextDOBDialog)
        val radioGroupGender = dialogView.findViewById<RadioGroup>(R.id.radioGroupGenderDialog)
        val radioButtonMale = dialogView.findViewById<RadioButton>(R.id.radioButtonMaleDialog)
        val radioButtonFemale = dialogView.findViewById<RadioButton>(R.id.radioButtonFemaleDialog)
        val radioButtonOther = dialogView.findViewById<RadioButton>(R.id.radioButtonOtherDialog)
        val spinnerParent = dialogView.findViewById<Spinner>(R.id.spinnerParentDialog)
        val editTextSchoolName = dialogView.findViewById<TextInputEditText>(R.id.editTextSchoolNameDialog)
        val editTextAddress = dialogView.findViewById<TextInputEditText>(R.id.editTextAddressDialog)
        val editTextNotes = dialogView.findViewById<TextInputEditText>(R.id.editTextNotesDialog)

        // New UI elements for QR code generation
        val qrButtonContainer = dialogView.findViewById<LinearLayout>(R.id.qrButtonContainer)
        val btnGenerateQrCode = dialogView.findViewById<MaterialButton>(R.id.btnGenerateQrCode)
        val btnSaveQrCode = dialogView.findViewById<MaterialButton>(R.id.btnSaveQrCode)
        val imageViewQrCode = dialogView.findViewById<ImageView>(R.id.imageViewQrCode)

        editTextDOB.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (studentToEdit != null && !studentToEdit.dateOfBirth.isNullOrEmpty()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    calendar.time = sdf.parse(studentToEdit.dateOfBirth!!) ?: Calendar.getInstance().time
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing DOB for DatePicker", e)
                }
            } else {
                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 10)
            }
            val datePickerDialog = DatePickerDialog(
                this@ManageStudentsActivity,
                { _, year, monthOfYear, dayOfMonth ->
                    selectedDOBCalendar.set(year, monthOfYear, dayOfMonth)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    editTextDOB.setText(sdf.format(selectedDOBCalendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val parentNamesForSpinner = parentUserList.map { it.fullName }
        val parentSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, parentNamesForSpinner)
        parentSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerParent.adapter = parentSpinnerAdapter

        builder.setTitle(if (studentToEdit == null) "Add New Student" else "Edit Student")
        var parentSelectionIndex = 0

        if (studentToEdit != null) {
            editTextStudentName.setText(studentToEdit.fullName)
            editTextAdmissionNumber.setText(studentToEdit.admissionNumber)
            editTextGrade.setText(studentToEdit.gradeOrClass)
            if (!studentToEdit.dateOfBirth.isNullOrEmpty()) editTextDOB.setText(studentToEdit.dateOfBirth)

            when (studentToEdit.gender) {
                "Male" -> radioButtonMale.isChecked = true
                "Female" -> radioButtonFemale.isChecked = true
                "Other" -> radioButtonOther.isChecked = true
            }

            if (!studentToEdit.parentUserId.isNullOrBlank()) {
                for (i in parentUserList.indices) {
                    if (parentUserList[i].id == studentToEdit.parentUserId) {
                        parentSelectionIndex = i
                        break
                    }
                }
            }
            editTextSchoolName.setText(studentToEdit.schoolName)
            editTextAddress.setText(studentToEdit.address)
            editTextNotes.setText(studentToEdit.notes)

            // Show QR code button container
            qrButtonContainer.visibility = View.VISIBLE

            // Set up Generate QR Code button
            btnGenerateQrCode.setOnClickListener {
                if (studentToEdit.id.isNotBlank()) {
                    val qrBitmap = generateQrCodeBitmap(studentToEdit.id)
                    if (qrBitmap != null) {
                        imageViewQrCode.setImageBitmap(qrBitmap)
                        imageViewQrCode.visibility = View.VISIBLE
                        btnSaveQrCode.visibility = View.VISIBLE // Show save button after generation
                        showToast("QR Code generated for ${studentToEdit.fullName}!")
                    } else {
                        imageViewQrCode.visibility = View.GONE
                        btnSaveQrCode.visibility = View.GONE
                        showToast("Failed to generate QR Code.")
                    }
                } else {
                    showToast("Student ID is not available to generate QR code. Save student first.")
                }
            }

            // Set up Save QR Code button
            btnSaveQrCode.setOnClickListener {
                val qrBitmap = (imageViewQrCode.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (qrBitmap != null) {
                    saveQrCodeToGallery(qrBitmap, studentToEdit.fullName)
                } else {
                    showToast("No QR Code to save. Generate it first.")
                }
            }
        } else {
            // Hide QR code button container for new students
            qrButtonContainer.visibility = View.GONE
            imageViewQrCode.visibility = View.GONE
        }
        spinnerParent.setSelection(parentSelectionIndex)

        builder.setPositiveButton(if (studentToEdit == null) "Add" else "Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val alertDialog = builder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = editTextStudentName.text.toString().trim()
            val admNo = editTextAdmissionNumber.text.toString().trim()
            val grade = editTextGrade.text.toString().trim()
            val dob = editTextDOB.text.toString().trim()
            val school = editTextSchoolName.text.toString().trim()
            val address = editTextAddress.text.toString().trim()
            val notes = editTextNotes.text.toString().trim()

            val gender = when (radioGroupGender.checkedRadioButtonId) {
                R.id.radioButtonMaleDialog -> "Male"
                R.id.radioButtonFemaleDialog -> "Female"
                R.id.radioButtonOtherDialog -> "Other"
                else -> ""
            }

            tilStudentName.error = null
            tilAdmissionNumber.error = null

            if (name.isEmpty()) {
                tilStudentName.error = "Student name cannot be empty"
                return@setOnClickListener
            }
            if (admNo.isEmpty()) {
                tilAdmissionNumber.error = "Admission number cannot be empty"
                return@setOnClickListener
            }

            var selectedParentUserId: String? = null
            val selectedParentPosition = spinnerParent.selectedItemPosition
            if (selectedParentPosition >= 0 && selectedParentPosition < parentUserList.size) {
                val potentialParentId = parentUserList[selectedParentPosition].id
                selectedParentUserId = if (potentialParentId.isBlank()) null else potentialParentId
            }


            if (studentToEdit == null) {
                studentViewModel.addStudent(
                    name,
                    dob.ifEmpty { null },
                    gender.ifEmpty { null },
                    grade.ifEmpty { null },
                    admNo,
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                    selectedParentUserId,
                    school.ifEmpty { null },
                    address.ifEmpty { null },
                    null,
                    notes.ifEmpty { null }
                )
            } else {
                studentViewModel.updateStudent(
                    studentToEdit.id,
                    name,
                    dob.ifEmpty { null },
                    gender.ifEmpty { null },
                    grade.ifEmpty { null },
                    admNo,
                    studentToEdit.admissionDate,
                    selectedParentUserId,
                    school.ifEmpty { null },
                    address.ifEmpty { null },
                    studentToEdit.profileImagePath,
                    notes.ifEmpty { null }
                )
            }
            alertDialog.dismiss()
        }
    }

    // Function to generate QR Code Bitmap
    private fun generateQrCodeBitmap(content: String): Bitmap? {
        val size = 512
        val qrCodeWriter = QRCodeWriter()
        try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(y, x, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bmp
        } catch (e: WriterException) {
            Log.e(TAG, "Error generating QR code", e)
            return null
        }
    }

    // Function to save QR code bitmap to gallery
    private fun saveQrCodeToGallery(bitmap: Bitmap, studentName: String) {
        val filename = "EduSoul_QR_${studentName.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        var imageUri: Uri? = null // Declare imageUri here

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "EduSoul_QR_Codes")
            }

            val insertedUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            insertedUri?.let { uri ->
                imageUri = uri // Assign to imageUri so it can be logged
                contentResolver.openOutputStream(uri)?.use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    showToast("QR Code saved to Gallery (EduSoul_QR_Codes folder)!")
                    Log.i(TAG, "QR Code saved to: $imageUri")
                } ?: run {
                    throw IOException("Failed to get output stream for URI: $uri")
                }
            } ?: run {
                throw IOException("Failed to create new MediaStore record.")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving QR code to gallery", e)
            showToast("Failed to save QR Code: ${e.message}")
        }
    }

    override fun onEditStudent(student: Student, position: Int) {
        showAddEditStudentDialog(student)
    }

    override fun onDeleteStudent(student: Student, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete '${student.fullName}'?")
            .setPositiveButton("Delete") { _, _ ->
                studentViewModel.deleteStudent(student.id)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_delete)
            .show()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.manage_students_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.action_import_students_xlsx) {
            showToast("Import Students from XLSX (To be implemented)")
            return true
        } else if (id == R.id.action_export_students_xlsx) {
            handleExportStudentsToXLSX()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleExportStudentsToXLSX() {
        Log.d(TAG, "handleExportStudentsToXLSX called")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            putExtra(Intent.EXTRA_TITLE, "EduSoul_Students_$timeStamp.xlsx")
        }

        try {
            createDocumentLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching file creation intent for XLSX export", e)
            showToast("Could not initiate export: " + e.message)
        }
    }

    private fun writeStudentsToXLSX(uri: Uri) {
        Log.d(TAG, "writeStudentsToXLSX called with URI: " + uri)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Students")

        val headerRow = sheet.createRow(0)
        val columns = arrayOf("ID", "Full Name", "Admission No", "Grade/Class", "DOB", "Gender",
            "Parent User ID", "School Name", "Address", "Notes", "Admission Date")
        for (i in columns.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(columns[i])
        }

        val studentsToExport = studentList
        if (studentsToExport.isEmpty()) {
            showToast("No student data to export.")
            return
        }

        var rowNum = 1
        for (student in studentsToExport) {
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(student.id)
            row.createCell(1).setCellValue(student.fullName)
            row.createCell(2).setCellValue(student.admissionNumber)
            row.createCell(3).setCellValue(student.gradeOrClass)
            row.createCell(4).setCellValue(student.dateOfBirth)
            row.createCell(5).setCellValue(student.gender)
            row.createCell(6).setCellValue(student.parentUserId)
            row.createCell(7).setCellValue(student.schoolName)
            row.createCell(8).setCellValue(student.address)
            row.createCell(9).setCellValue(student.notes)
            row.createCell(10).setCellValue(student.admissionDate)
        }

        for (i in columns.indices) {
            sheet.autoSizeColumn(i)
        }

        var outputStream: OutputStream? = null
        try {
            outputStream = contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                throw IOException("Failed to get output stream for URI: " + uri)
            }
            workbook.write(outputStream)
            showToast("Students exported successfully to " + uri.path)
            Log.i(TAG, "Students exported successfully to: " + uri)
        } catch (e: IOException) {
            Log.e(TAG, "Error writing XLSX file", e)
            showToast("Export failed: " + e.message)
        } finally {
            try {
                outputStream?.close()
                workbook.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing workbook/stream", e)
            }
        }
    }
}