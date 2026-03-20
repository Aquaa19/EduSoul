// File: main/java/com/aquaa/edusoul/viewmodels/ParentViewFeeStatusViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.FeeStructureRepository
import com.aquaa.edusoul.repositories.UserRepository

class ParentViewFeeStatusViewModelFactory(
    private val studentRepository: StudentRepository,
    private val feePaymentRepository: FeePaymentRepository,
    private val feeStructureRepository: FeeStructureRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentViewFeeStatusViewModel::class.java)) {
            return ParentViewFeeStatusViewModel(
                studentRepository,
                feePaymentRepository,
                feeStructureRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}