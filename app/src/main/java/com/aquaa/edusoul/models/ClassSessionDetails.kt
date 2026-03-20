package com.aquaa.edusoul.models

/**
 * A helper data class to hold combined details for a ClassSession
 * including subject name, batch name, and teacher name.
 * Used for display purposes in the Teacher Dashboard's "Next Class" card.
 */
data class ClassSessionDetails(
    val classSession: ClassSession,
    val subjectName: String="",
    val batchName: String="",
    val teacherName: String=""
)