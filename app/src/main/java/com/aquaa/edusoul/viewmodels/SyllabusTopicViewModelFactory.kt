package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.repositories.SyllabusProgressRepository

class SyllabusTopicViewModelFactory(
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val syllabusProgressRepository: SyllabusProgressRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SyllabusTopicViewModel::class.java)) {
            return SyllabusTopicViewModel(syllabusTopicRepository, syllabusProgressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}