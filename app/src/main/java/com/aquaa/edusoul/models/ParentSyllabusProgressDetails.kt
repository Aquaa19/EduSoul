package com.aquaa.edusoul.models

data class ParentSyllabusProgressDetails(
    val syllabusTopicId: String = "",
    val topicName: String = "",
    val isCompleted: Boolean = false, // Added default value
    val completionDate: String? = null,
    val remarks: String? = null,
    val updatedByTeacherId: String = "", // Added default value
    val studentId: String = "", // Added default value
    val studentName: String = "", // Added default value
    val subjectName: String = "", // Added default value
    val teacherName: String? = null,
    val batchId: String = "", // Added default value
    val batchName: String = "", // Added default value
    val description: String? = null,
    val topicOrder: Int = 0, // Added default value

    // ADDED the missing subjectId property
    val subjectId: String = "", // Added default value
    var syllabusProgressId: String = ""
)
