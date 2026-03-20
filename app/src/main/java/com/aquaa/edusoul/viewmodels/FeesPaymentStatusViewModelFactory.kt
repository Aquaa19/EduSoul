// File: main/java/com/aquaa/edusoul/viewmodels/FeesPaymentStatusViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository

class FeesPaymentStatusViewModelFactory(
    private val studentRepository: StudentRepository,
    private val batchRepository: BatchRepository,
    private val feeStructureRepository: FeeStructureRepository,
    private val feePaymentRepository: FeePaymentRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeesPaymentStatusViewModel::class.java)) {
            return FeesPaymentStatusViewModel(
                studentRepository,
                batchRepository,
                feeStructureRepository,
                feePaymentRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}