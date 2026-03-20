// File: main/java/com/aquaa/edusoul/viewmodels/ManageFeeStructuresViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.FeeStructureRepository

class ManageFeeStructuresViewModelFactory(private val repository: FeeStructureRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageFeeStructuresViewModel::class.java)) {
            return ManageFeeStructuresViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}