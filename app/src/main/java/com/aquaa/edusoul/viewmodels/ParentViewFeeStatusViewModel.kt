package com.aquaa.edusoul.viewmodels

import ParentFeePaymentDetails
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.*
import com.aquaa.edusoul.repositories.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow // Import flow builder for suspending operations
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParentViewFeeStatusViewModel(
    private val studentRepository: StudentRepository,
    private val feePaymentRepository: FeePaymentRepository,
    private val feeStructureRepository: FeeStructureRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val TAG = "ParentViewFeeStatusVM"

    private val _parentUserId = MutableLiveData<String>()

    val parentFeePaymentDetailsList: LiveData<List<ParentFeePaymentDetails>> =
        _parentUserId.switchMap { parentId ->
            if (parentId.isNotBlank()) {
                // FIX 1: Correct method name from getStudentsByParentId to getStudentsByParent
                studentRepository.getStudentsByParent(parentId)
                    // flatMapConcat is suitable here as it will emit a new Flow of payment details
                    // for each list of children. The lambda receives List<Student>.
                    .flatMapConcat { children: List<Student> -> // FIX 2, 3: Explicitly type `children`
                        if (children.isEmpty()) {
                            Log.d(TAG, "No children found for parentId: $parentId, returning empty list.")
                            return@flatMapConcat flowOf(emptyList()) // Emit empty list if no children
                        }
                        Log.d(TAG, "Found ${children.size} children for parentId: $parentId.")

                        // Map each child to a Flow of their payment details
                        val childPaymentFlows = children.map { child: Student -> // FIX 4: Explicitly type `child`
                            feePaymentRepository.getFeePaymentsForStudent(child.id)
                                // Use flatMapConcat here to enter a suspendable context for each list of payments
                                .flatMapConcat { payments ->
                                    // Use `flow { ... }` builder to emit a new flow where suspend functions are allowed
                                    flow {
                                        val paymentDetails = payments.mapNotNull { payment ->
                                            // Suspend calls are now safe inside this flow builder's coroutine
                                            val feeStructure = payment.feeStructureId?.let { id ->
                                                feeStructureRepository.getFeeStructureById(id) // This is a suspend call
                                            }
                                            val recordedByUser = payment.recordedByUserId?.let { id ->
                                                userRepository.getUserById(id) // This is a suspend call
                                            }

                                            ParentFeePaymentDetails(
                                                paymentId = payment.id,
                                                studentId = payment.studentId,
                                                feeStructureId = payment.feeStructureId,
                                                paymentDate = payment.paymentDate,
                                                amountPaid = payment.amountPaid,
                                                paymentMethod = payment.paymentMethod,
                                                paymentPeriod = payment.paymentPeriod,
                                                paymentStatus = payment.status,
                                                paymentRemarks = payment.remarks,
                                                recordedByUserId = payment.recordedByUserId,
                                                studentName = child.fullName, // FIX: child.fullName now resolves
                                                feeStructureTitle = feeStructure?.title,
                                                feeStructureAmount = feeStructure?.amount,
                                                feeStructureFrequency = feeStructure?.duration,
                                                recordedByName = recordedByUser?.fullName // FIX: recordedByUser?.fullName now resolves
                                            )
                                        }
                                        emit(paymentDetails) // Emit the list of ParentFeePaymentDetails
                                    }
                                }
                        }
                        // Combine all individual child payment flows into a single Flow of lists.
                        // The lambda parameter `arrayOfLists` is an Array<List<ParentFeePaymentDetails>>.
                        // FIX 5: Explicitly type `it` in flatMap lambda to resolve ambiguity
                        kotlinx.coroutines.flow.combine(childPaymentFlows) { arrayOfLists ->
                            arrayOfLists.flatMap { it: List<ParentFeePaymentDetails> -> it }
                        }
                    }
                    .asLiveData(Dispatchers.IO)
            } else {
                MutableLiveData(emptyList())
            }
        }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun setParentUserId(userId: String) {
        _parentUserId.value = userId
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}