// File: main/java/com/aquaa/edusoul/viewmodels/SelectFeeStructureViewModel.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.aquaa.edusoul.models.FeeStructure
import com.aquaa.edusoul.repositories.FeeStructureRepository
import kotlinx.coroutines.Dispatchers

class SelectFeeStructureViewModel(private val repository: FeeStructureRepository) : ViewModel() {

    // LiveData holding the list of all fee structures, observed from the repository's Flow
    val allFeeStructures: LiveData<List<FeeStructure>> = repository.getAllFeeStructures().asLiveData(Dispatchers.IO)

    // You might add loading and error states here if needed for this ViewModel,
    // though for simple data fetching it's often handled at the Activity level
    // based on whether the LiveData is populated or empty.
}