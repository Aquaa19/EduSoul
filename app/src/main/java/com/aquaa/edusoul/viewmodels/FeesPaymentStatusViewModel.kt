package com.aquaa.edusoul.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.aquaa.edusoul.models.FeePayment
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentFeeStatus
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.catch

class FeesPaymentStatusViewModel(
    private val studentRepository: StudentRepository,
    private val batchRepository: BatchRepository,
    private val feeStructureRepository: FeeStructureRepository,
    private val feePaymentRepository: FeePaymentRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "FeesPaymentStatusVM"

    private val _selectedPeriod = MutableLiveData<String>()

    val feeStatusReport: LiveData<List<StudentFeeStatus>> = _selectedPeriod.switchMap { period ->
        if (period.isNotBlank()) {
            studentRepository.getAllStudents()
                .catch { Log.e(TAG, "Error fetching all students for fee status: ${it.message}", it); emit(emptyList()) }
                .combine(
                    feePaymentRepository.getFeePaymentsForPeriod(period)
                        .catch { Log.e(TAG, "Error fetching fee payments for period: ${it.message}", it); emit(emptyList()) }
                ) { allStudents, periodPayments ->
                    processFeeStatusData(allStudents, periodPayments, period)
                }.asLiveData(Dispatchers.IO)
        } else {
            MutableLiveData(emptyList())
        }
    }

    val allStudentsForDialog: LiveData<List<Student>> = studentRepository.getAllStudents()
        .catch { Log.e(TAG, "Error fetching all students for dialog: ${it.message}", it); emit(emptyList()) }
        .asLiveData(Dispatchers.IO)

    private val _dialogStudentId = MutableLiveData<String?>()
    private val _dialogPaymentPeriod = MutableLiveData<String?>()

    private val _dialogInputTrigger = MediatorLiveData<Pair<String?, String?>>().apply {
        var studentId: String? = null
        var paymentPeriod: String? = null

        val updater = {
            val localStudentId = studentId
            val localPaymentPeriod = paymentPeriod
            if (localStudentId != null && localPaymentPeriod != null) {
                value = Pair(localStudentId, localPaymentPeriod)
            }
        }
        addSource(_dialogStudentId) { studentId = it; updater() }
        addSource(_dialogPaymentPeriod) { paymentPeriod = it; updater() }
    }

    val dialogExpectedAmountInfo: LiveData<Pair<Double, Double>> =
        _dialogInputTrigger.switchMap { (studentId, paymentPeriod) ->
            liveData(Dispatchers.IO) {
                if (studentId != null && studentId.isNotBlank() && !paymentPeriod.isNullOrBlank()) {
                    val expectedFee = calculateExpectedFee(studentId)
                    val totalPaid = calculateTotalPaidForPeriod(studentId, paymentPeriod)
                    emit(Pair(expectedFee, totalPaid))
                } else {
                    emit(Pair(0.0, 0.0))
                }
            }
        }


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setSelectedPeriod(year: Int, month: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        // Use "yyyy-MM" format for internal period representation
        val period = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
        if (_selectedPeriod.value != period) {
            _selectedPeriod.value = period
        }
    }

    fun setDialogStudentAndPeriod(studentId: String?, paymentPeriod: String?) {
        // Ensure paymentPeriod passed to dialog is converted to "yyyy-MM" if it's "MMMM yyyy"
        val formattedPaymentPeriod = if (!paymentPeriod.isNullOrBlank()) {
            try {
                val date = SimpleDateFormat("MMMM yyyy", Locale.US).parse(paymentPeriod)
                SimpleDateFormat("yyyy-MM", Locale.US).format(date)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing payment period from MMMM yyyy to yyyy-MM: ${e.message}")
                paymentPeriod // Fallback to original if parsing fails
            }
        } else {
            null
        }
        _dialogStudentId.value = studentId
        _dialogPaymentPeriod.value = formattedPaymentPeriod
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private suspend fun processFeeStatusData(
        allStudents: List<Student>,
        periodPayments: List<FeePayment>,
        selectedPeriod: String
    ): List<StudentFeeStatus> {
        val studentStatusMap = mutableMapOf<String, StudentFeeStatus>()

        if (allStudents.isEmpty()) {
            return emptyList()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US) // Assuming paymentDate is in "yyyy-MM-dd" format

        for (student in allStudents) {
            var batchName = "N/A"
            var studentFeeStructureAmount = 0.0

            val studentBatches = batchRepository.getBatchesForStudent(student.id).firstOrNull()
            val primaryBatch = studentBatches?.firstOrNull()

            if (primaryBatch != null) {
                batchName = primaryBatch.batchName
                primaryBatch.feeStructureId?.let { feeStructureId ->
                    if (feeStructureId.isNotBlank()) {
                        val feeStructure = feeStructureRepository.getFeeStructureById(feeStructureId)
                        if (feeStructure != null) {
                            if (feeStructure.duration?.equals("Monthly", ignoreCase = true) == true) {
                                studentFeeStructureAmount = feeStructure.amount ?: 0.0
                            }
                        }
                    }
                }
            }

            studentStatusMap[student.id] = StudentFeeStatus(
                studentId = student.id,
                studentName = student.fullName,
                batchName = batchName,
                totalAmountDue = studentFeeStructureAmount,
                totalAmountPaid = 0.0,
                status = FeePayment.STATUS_DUE,
                lastPaymentDate = null
            )
        }

        // Updated logic to correctly aggregate payments and determine status
        val aggregatedPayments = mutableMapOf<String, FeePayment>()

        for (payment in periodPayments) {
            val key = "${payment.studentId}-${payment.paymentPeriod}-${payment.feeStructureId}"
            val existingAggregatedPayment = aggregatedPayments[key]

            if (existingAggregatedPayment == null) {
                // If no payment for this student/period/fee structure yet, add it
                aggregatedPayments[key] = payment
            } else {
                // If a payment already exists, aggregate the amountPaid and take the latest paymentDate
                val updatedAmountPaid = existingAggregatedPayment.amountPaid + payment.amountPaid
                val latestPaymentDate = try {
                    val existingDate = dateFormat.parse(existingAggregatedPayment.paymentDate)
                    val newDate = dateFormat.parse(payment.paymentDate)
                    if (newDate != null && existingDate != null && newDate.after(existingDate)) {
                        payment.paymentDate
                    } else {
                        existingAggregatedPayment.paymentDate
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date for aggregation: ${e.message}")
                    existingAggregatedPayment.paymentDate
                }

                aggregatedPayments[key] = existingAggregatedPayment.copy(
                    amountPaid = updatedAmountPaid,
                    paymentDate = latestPaymentDate // Update to the latest recorded payment date
                )
            }
        }

        // Now, iterate through the aggregated payments to update student statuses
        for (payment in aggregatedPayments.values) {
            val studentStatus = studentStatusMap[payment.studentId]
            if (studentStatus != null) {
                studentStatus.totalAmountPaid = payment.amountPaid // Use the aggregated amount
                studentStatus.lastPaymentDate = payment.paymentDate // Use the latest payment date from aggregation

                val amountDue = studentStatus.totalAmountDue
                val amountPaid = studentStatus.totalAmountPaid

                if (amountDue > 0) {
                    studentStatus.status = when {
                        amountPaid >= amountDue -> if (amountPaid > amountDue) FeePayment.STATUS_OVERPAID else FeePayment.STATUS_PAID
                        amountPaid > 0 -> FeePayment.STATUS_PARTIALLY_PAID
                        else -> FeePayment.STATUS_DUE
                    }
                } else {
                    studentStatus.status = if (amountPaid > 0) FeePayment.STATUS_PAID else "N/A"
                }
            }
        }

        return studentStatusMap.values.filter { it.totalAmountDue > 0 || it.totalAmountPaid > 0 }
            .sortedBy { it.studentName }
    }

    private suspend fun calculateExpectedFee(studentId: String): Double {
        val studentBatches = batchRepository.getBatchesForStudent(studentId).firstOrNull()
        val primaryBatch = studentBatches?.firstOrNull()

        if (primaryBatch != null) {
            primaryBatch.feeStructureId?.let { feeStructureId ->
                if (feeStructureId.isNotBlank()) {
                    val feeStructure = feeStructureRepository.getFeeStructureById(feeStructureId)
                    if (feeStructure != null && feeStructure.duration?.equals("Monthly", ignoreCase = true) == true) {
                        return feeStructure.amount ?: 0.0
                    }
                }
            }
        }
        return 0.0
    }

    private suspend fun calculateTotalPaidForPeriod(studentId: String, paymentPeriod: String): Double {
        // Ensure paymentPeriod is in "yyyy-MM" format for filtering
        val paymentsForStudent = feePaymentRepository.getFeePaymentsForStudent(studentId)
            .catch { Log.e(TAG, "Error fetching fee payments for student $studentId for period $paymentPeriod: ${it.message}", it); emit(emptyList()) }
            .firstOrNull()

        return paymentsForStudent?.filter {
            it.paymentPeriod?.equals(paymentPeriod, ignoreCase = true) == true
        }?.sumOf { it.amountPaid } ?: 0.0
    }

    fun recordFeePayment(
        studentId: String,
        amountPaid: Double,
        paymentDate: String,
        paymentMethod: String?,
        paymentPeriod: String?, // This is now expected to be in "MMMM yyyy" format from UI
        remarks: String?,
        recordedByUserId: String?
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val studentBatches = batchRepository.getBatchesForStudent(studentId).firstOrNull()
                val primaryBatch = studentBatches?.firstOrNull()
                val feeStructureId = primaryBatch?.feeStructureId

                // Convert paymentPeriod from "MMMM yyyy" (UI) to "yyyy-MM" (Firestore)
                val formattedPaymentPeriodForFirestore = if (!paymentPeriod.isNullOrBlank()) {
                    try {
                        val date = SimpleDateFormat("MMMM yyyy", Locale.US).parse(paymentPeriod)
                        SimpleDateFormat("yyyy-MM", Locale.US).format(date)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing payment period from MMMM yyyy to yyyy-MM for Firestore: ${e.message}")
                        paymentPeriod // Fallback to original if parsing fails
                    }
                } else {
                    null
                }

                // Find existing fee payment for this student and period (using yyyy-MM format)
                val existingPayment = feePaymentRepository.getFeePaymentsForStudent(studentId)
                    .firstOrNull()
                    ?.find {
                        it.paymentPeriod == formattedPaymentPeriodForFirestore && it.feeStructureId == feeStructureId
                    }

                val expectedAmountForPeriod = calculateExpectedFee(studentId)
                val totalPaidBeforeThisPayment = existingPayment?.amountPaid ?: 0.0
                val newTotalPaidForPeriod = totalPaidBeforeThisPayment + amountPaid

                val status = when {
                    expectedAmountForPeriod > 0 && newTotalPaidForPeriod >= expectedAmountForPeriod ->
                        if (newTotalPaidForPeriod > expectedAmountForPeriod) FeePayment.STATUS_OVERPAID else FeePayment.STATUS_PAID
                    expectedAmountForPeriod > 0 && newTotalPaidForPeriod > 0 ->
                        FeePayment.STATUS_PARTIALLY_PAID
                    else -> FeePayment.STATUS_DUE
                }

                val feePaymentToSave: FeePayment
                val isUpdate: Boolean

                if (existingPayment != null) {
                    // Update existing payment
                    feePaymentToSave = existingPayment.copy(
                        amountPaid = newTotalPaidForPeriod,
                        paymentDate = paymentDate, // Keep as is, it's the date of this transaction
                        paymentMethod = paymentMethod,
                        paymentPeriod = formattedPaymentPeriodForFirestore, // Ensure consistency
                        status = status,
                        remarks = remarks,
                        recordedByUserId = recordedByUserId
                    )
                    isUpdate = true
                } else {
                    // Create new payment (should only happen for the very first payment for a period)
                    feePaymentToSave = FeePayment(
                        id = "", // Firestore will generate
                        studentId = studentId,
                        feeStructureId = feeStructureId,
                        paymentDate = paymentDate,
                        amountPaid = newTotalPaidForPeriod,
                        paymentMethod = paymentMethod,
                        paymentPeriod = formattedPaymentPeriodForFirestore, // Ensure consistency
                        status = status,
                        remarks = remarks,
                        recordedByUserId = recordedByUserId
                    )
                    isUpdate = false
                }

                val resultSuccess = if (isUpdate) {
                    feePaymentRepository.updateFeePayment(feePaymentToSave) > 0 // Check if update affected rows
                } else {
                    feePaymentRepository.insertFeePayment(feePaymentToSave).isNotBlank() // Check if insert returned a non-blank ID
                }

                withContext(Dispatchers.Main) {
                    if (resultSuccess) {
                        _errorMessage.value = "Payment recorded successfully!"
                        _selectedPeriod.value = _selectedPeriod.value // Re-trigger refresh
                    } else {
                        _errorMessage.value = "Failed to record payment."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Error recording payment: ${e.message}"
                }
                Log.e(TAG, "Error recording fee payment", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}