// File: main/java/com/aquaa/edusoul/viewmodels/SyllabusStatusViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.repositories.SyllabusProgressRepository
import com.aquaa.edusoul.repositories.UserRepository

class SyllabusStatusViewModelFactory(
    private val batchRepository: BatchRepository,
    private val subjectRepository: SubjectRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val syllabusProgressRepository: SyllabusProgressRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyllabusStatusViewModel::class.java)) {
            return SyllabusStatusViewModel(
                batchRepository,
                subjectRepository,
                syllabusTopicRepository,
                syllabusProgressRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}