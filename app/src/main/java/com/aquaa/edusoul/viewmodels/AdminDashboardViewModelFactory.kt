package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.ClassSessionRepository
import com.aquaa.edusoul.repositories.FeePaymentRepository
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.UserRepository

class AdminDashboardViewModelFactory(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val classSessionRepository: ClassSessionRepository,
    private val feePaymentRepository: FeePaymentRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            return AdminDashboardViewModel(
                studentRepository,
                userRepository,
                classSessionRepository,
                feePaymentRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}