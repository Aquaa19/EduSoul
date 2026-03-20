package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.aquaa.edusoul.models.FeePayment
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class AdminDashboardViewModel(
    studentRepository: StudentRepository,
    userRepository: UserRepository,
    classSessionRepository: ClassSessionRepository,
    feePaymentRepository: FeePaymentRepository
) : ViewModel() {

    private val TAG = "AdminDashboardVM"

    val totalStudents: LiveData<Int> = studentRepository.getAllStudents()
        .map { it.size }
        .catch {
            emit(0)
        }
        .asLiveData(Dispatchers.IO)

    val totalTeachers: LiveData<Int> = userRepository.getAllTeachers()
        .map { it.size }
        .catch {
            emit(0)
        }
        .asLiveData(Dispatchers.IO)

    val upcomingClassesToday: LiveData<Int> = classSessionRepository.getClassSessionsForDate(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ).map { it.size }
        .catch {
            emit(0)
        }
        .asLiveData(Dispatchers.IO)

    val pendingFees: LiveData<Int> = feePaymentRepository.getAllFeePayments()
        .map { payments ->
            Log.d(TAG, "Fetched ${payments.size} fee payments for pending fees calculation.")
            // Filter payments to count only the *latest* payment for each student/period combination
            // and check its status. This assumes one FeePayment document per student per period.
            val latestPayments = payments
                .groupBy { "${it.studentId}-${it.paymentPeriod}-${it.feeStructureId}" } // Group by student, period, and fee structure
                .mapNotNull { (_, feePaymentList) ->
                    // Find the latest payment entry for this group (e.g., by paymentDate or by ID if paymentDate can be same)
                    // For simplicity, let's assume the latest payment is the one with the most recent paymentDate
                    feePaymentList.maxByOrNull { it.paymentDate } // Or a more robust way to find the "latest" status
                }

            val count = latestPayments.count {
                val status = it.status?.trim()?.toLowerCase(Locale.ROOT)
                val isDue = status == FeePayment.STATUS_DUE.toLowerCase(Locale.ROOT)
                val isPartial = status == FeePayment.STATUS_PARTIALLY_PAID.toLowerCase(Locale.ROOT)
                Log.d(TAG, "Processing FeePayment ID: ${it.id}, Raw Status: '${it.status}', Trimmed/Lower Status: '$status', Is Due: $isDue, Is Partial: $isPartial")
                isDue || isPartial
            }
            Log.d(TAG, "Calculated pending fees count: $count")
            count
        }
        .catch { error ->
            Log.e(TAG, "Error calculating pending fees: ${error.message}", error)
            emit(0)
        }
        .asLiveData(Dispatchers.IO)
}
