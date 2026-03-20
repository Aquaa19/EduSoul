// File: main/java/com/aquaa/edusoul/viewmodels/SelectFeeStructureViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.FeeStructureRepository

class SelectFeeStructureViewModelFactory(private val repository: FeeStructureRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectFeeStructureViewModel::class.java)) {
            return SelectFeeStructureViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}