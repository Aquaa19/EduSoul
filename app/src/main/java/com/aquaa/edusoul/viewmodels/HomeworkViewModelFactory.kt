// main/java/com/aquaa/edusoul/viewmodels/HomeworkViewModelFactory.kt
package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.HomeworkRepository
import com.aquaa.edusoul.repositories.SubjectRepository

class HomeworkViewModelFactory(
    private val homeworkRepository: HomeworkRepository,
    private val subjectRepository: SubjectRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeworkViewModel::class.java)) {
            return HomeworkViewModel(homeworkRepository, subjectRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}