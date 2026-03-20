package com.aquaa.edusoul.models

data class TeacherAssignmentDetails(
    val assignmentId: String="",
    val teacherId: String="", // Ensure this parameter is present
    val subjectId: String="", // Ensure this parameter is present
    val batchId: String="",   // Ensure this parameter is present
    val subjectName: String="",
    val batchName: String=""
)