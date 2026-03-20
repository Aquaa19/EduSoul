package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.FeePayment
import com.aquaa.edusoul.models.StudentBatchLink
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.StudentBatchLinkRepository
import com.aquaa.edusoul.repositories.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageBatchesViewModel(
    private val batchRepository: BatchRepository,
    private val feeStructureRepository: FeeStructureRepository,
    private val studentRepository: StudentRepository,
    private val studentBatchLinkRepository: StudentBatchLinkRepository,
    private val feePaymentRepository: FeePaymentRepository // Inject FeePaymentRepository
) : ViewModel() {

    private val TAG = "ManageBatchesVM"

    // LiveData for all batches to display in the RecyclerView
    val allBatches: LiveData<List<Batch>> = batchRepository.getAllBatches().asLiveData(Dispatchers.IO)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _selectedFeeStructureIdForDialog = MutableLiveData<String?>()
    val selectedFeeStructureIdForDialog: LiveData<String?> get() = _selectedFeeStructureIdForDialog

    private val _selectedFeeStructureTitleForDialog = MutableLiveData<String?>()
    val selectedFeeStructureTitleForDialog: LiveData<String?> get() = _selectedFeeStructureTitleForDialog

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setTemporarySelectedFeeStructure(feeStructureId: String?, feeStructureTitle: String?) {
        _selectedFeeStructureIdForDialog.value = feeStructureId
        _selectedFeeStructureTitleForDialog.value = feeStructureTitle
    }

    fun clearTemporarySelectedFeeStructure() {
        _selectedFeeStructureIdForDialog.value = null
        _selectedFeeStructureTitleForDialog.value = null
    }

    fun addBatch(batch: Batch, autoEnrollStudents: Boolean) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newBatchId = batchRepository.insertBatch(batch)
                withContext(Dispatchers.Main) {
                    if (newBatchId.isNotBlank()) {
                        _errorMessage.value = "Batch added successfully!"
                        // Ensure batch object has the new ID for subsequent operations
                        batch.id = newBatchId

                        if (autoEnrollStudents && !batch.gradeLevel.isNullOrBlank()) {
                            // Auto-enroll students
                            autoEnrollStudentsInBatch(batch.gradeLevel!!, newBatchId)
                        }

                        // After batch is added and potentially students auto-enrolled,
                        // ensure initial fee payments are created if a fee structure is set.
                        if (!batch.feeStructureId.isNullOrBlank()) {
                            ensureInitialFeePayments(batch.id, batch.feeStructureId!!)
                        }
                    } else {
                        _errorMessage.value = "Failed to add batch (name might exist or other error)."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error adding batch: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateBatch(batch: Batch) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch the old batch to compare feeStructureId
                val oldBatch = batchRepository.getBatchById(batch.id)

                val rowsAffected = batchRepository.updateBatch(batch)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Batch updated successfully!"

                        // If fee structure was added or changed, ensure fee payments are generated
                        val oldFeeStructureId = oldBatch?.feeStructureId
                        val newFeeStructureId = batch.feeStructureId

                        if (newFeeStructureId != null && newFeeStructureId != oldFeeStructureId) {
                            Log.d(TAG, "Fee structure changed for batch ${batch.id}. Ensuring initial fee payments.")
                            ensureInitialFeePayments(batch.id, newFeeStructureId)
                        }
                    } else {
                        _errorMessage.value = "Failed to update batch."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error updating batch: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun deleteBatch(batchId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowsAffected = batchRepository.deleteBatchById(batchId)
                withContext(Dispatchers.Main) {
                    if (rowsAffected > 0) {
                        _errorMessage.value = "Batch deleted successfully."
                    } else {
                        _errorMessage.value = "Failed to delete batch."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error deleting batch: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    suspend fun getFeeStructureTitleById(feeStructureId: String?): String? {
        if (feeStructureId.isNullOrBlank()) return null
        return feeStructureRepository.getFeeStructureById(feeStructureId)?.title
    }

    private suspend fun autoEnrollStudentsInBatch(gradeLevel: String, batchId: String) {
        if (gradeLevel.isBlank() || batchId.isBlank()) {
            Log.w(TAG, "autoEnrollStudents: Invalid gradeLevel or batchId provided.")
            withContext(Dispatchers.Main) {
                _errorMessage.value = "Auto-enrollment failed: invalid grade or batch ID."
            }
            return
        }
        var enrolledCount = 0
        try {
            val studentsInGrade = studentRepository.getAllStudents().firstOrNull()?.filter {
                it.gradeOrClass?.equals(gradeLevel, ignoreCase = true) == true
            } ?: emptyList()

            if (studentsInGrade.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "No students found in grade '$gradeLevel' for auto-enrollment."
                }
                return
            }

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            for (student in studentsInGrade) {
                val existingEnrollment = studentBatchLinkRepository.getStudentBatchLink(student.id, batchId)
                if (existingEnrollment == null) {
                    val newEnrollment = StudentBatchLink(
                        id = "", // Firestore generates ID
                        studentId = student.id,
                        batchId = batchId,
                        enrollmentDate = currentDate,
                        status = "Enrolled"
                    )
                    val id = studentBatchLinkRepository.insertStudentBatchLink(newEnrollment)
                    if (id.isNotBlank()) {
                        enrolledCount++
                    }
                } else {
                    enrolledCount++ // Already enrolled, count as "ensured"
                }
            }
            withContext(Dispatchers.Main) {
                _errorMessage.value = "$enrolledCount student(s) auto-enrolled into the batch."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto-enrollment: ${e.message}", e)
            withContext(Dispatchers.Main) {
                _errorMessage.value = "Auto-enrollment failed: ${e.message}"
            }
        }
    }

    /**
     * Ensures initial fee payment documents are created for all students in a given batch
     * if a fee structure is assigned and no payment exists for the current period.
     * This function is idempotent: it won't create duplicates if a payment already exists.
     * @param batchId The ID of the batch.
     * @param feeStructureId The ID of the fee structure assigned to the batch.
     */
    private suspend fun ensureInitialFeePayments(batchId: String, feeStructureId: String) {
        if (batchId.isBlank() || feeStructureId.isBlank()) {
            Log.w(TAG, "ensureInitialFeePayments: Invalid batchId or feeStructureId provided.")
            return
        }

        try {
            // 1. Get all students currently enrolled in this batch
            val enrolledStudents = studentRepository.getStudentsByBatch(batchId).firstOrNull() ?: emptyList()
            if (enrolledStudents.isEmpty()) {
                Log.d(TAG, "No students enrolled in batch $batchId. Skipping initial fee payment creation.")
                return
            }

            val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            for (student in enrolledStudents) {
                // 2. Check if a FeePayment for this student, fee structure, and current period already exists
                // This is a simplified check. For more complex scenarios (e.g., annual fees vs monthly),
                // you might need to query for payments related to the feeStructureId directly.
                val existingPaymentsForStudent = feePaymentRepository.getFeePaymentsForStudent(student.id).firstOrNull() ?: emptyList()

                val paymentExistsForCurrentPeriod = existingPaymentsForStudent.any {
                    it.feeStructureId == feeStructureId && it.paymentPeriod == currentMonthYear
                }

                if (!paymentExistsForCurrentPeriod) {
                    // 3. If no payment exists for the current period and fee structure, create a "Due" payment
                    val newFeePayment = FeePayment(
                        studentId = student.id,
                        feeStructureId = feeStructureId,
                        paymentDate = currentDate, // Use current date for when the fee became "due" in the system
                        amountPaid = 0.0, // Initially 0 as it's due
                        paymentMethod = null,
                        paymentPeriod = currentMonthYear,
                        status = FeePayment.STATUS_DUE,
                        remarks = "Initial fee due for ${currentMonthYear}",
                        recordedByUserId = null // Can be set to admin user ID if available
                    )
                    val paymentId = feePaymentRepository.insertFeePayment(newFeePayment)
                    if (paymentId.isNotBlank()) {
                        Log.d(TAG, "Created initial 'Due' fee payment for student ${student.fullName} (ID: ${student.id}) in batch $batchId. Payment ID: $paymentId")
                    } else {
                        Log.e(TAG, "Failed to create initial 'Due' fee payment for student ${student.fullName} (ID: ${student.id}) in batch $batchId.")
                    }
                } else {
                    Log.d(TAG, "Fee payment for student ${student.fullName} (ID: ${student.id}) and fee structure $feeStructureId already exists for $currentMonthYear. Skipping.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring initial fee payments for batch $batchId: ${e.message}", e)
            withContext(Dispatchers.Main) {
                _errorMessage.value = "Error generating initial fee payments: ${e.message}"
            }
        }
    }
}
