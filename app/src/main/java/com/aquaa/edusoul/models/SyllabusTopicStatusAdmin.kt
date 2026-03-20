package com.aquaa.edusoul.models

/*
 * Helper model data class to hold the combined data for the Admin's syllabus progress report.
 * Converted from Java POJO to Kotlin data class.
 */
data class SyllabusTopicStatusAdmin(
    var topicName: String="",
    var isCompleted: Boolean=false,
    var completionDate: String?=null, // Nullable string for date
    var teacherName: String?=null // Nullable string for teacher name
)