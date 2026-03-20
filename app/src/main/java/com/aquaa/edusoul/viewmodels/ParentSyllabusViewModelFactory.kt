package com.aquaa.edusoul.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aquaa.edusoul.repositories.StudentRepository
import com.aquaa.edusoul.repositories.SubjectRepository
import com.aquaa.edusoul.repositories.BatchRepository
import com.aquaa.edusoul.repositories.SyllabusTopicRepository
import com.aquaa.edusoul.repositories.SyllabusProgressRepository
import com.aquaa.edusoul.repositories.UserRepository // Assuming UserRepository is needed for teacher names if fetching reports

class ParentSyllabusViewModelFactory(
    private val studentRepository: StudentRepository,
    private val subjectRepository: SubjectRepository,
    private val batchRepository: BatchRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val syllabusProgressRepository: SyllabusProgressRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParentSyllabusViewModel::class.java)) {
            return ParentSyllabusViewModel(
                studentRepository,
                subjectRepository,
                batchRepository,
                syllabusTopicRepository,
                syllabusProgressRepository,
                userRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}