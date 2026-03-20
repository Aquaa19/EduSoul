package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.StudentBatchLinkRepository
import com.aquaa.edusoul.repositories.StudentRepository

class ManageBatchesViewModelFactory(
    private val batchRepository: BatchRepository,
    private val feeStructureRepository: FeeStructureRepository,
    private val studentRepository: StudentRepository,
    private val studentBatchLinkRepository: StudentBatchLinkRepository,
    private val feePaymentRepository: FeePaymentRepository // Add FeePaymentRepository here
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageBatchesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManageBatchesViewModel(
                batchRepository,
                feeStructureRepository,
                studentRepository,
                studentBatchLinkRepository,
                feePaymentRepository // Pass it to the ViewModel
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
