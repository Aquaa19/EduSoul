package com.aquaa.edusoul.models

/*
 * Enrollment: A simple helper model data class to represent a student's enrollment
 * in a specific batch. This is often used for displaying enrollment statuses
 * or for simplified data transfer.
 * Converted from Java POJO to Kotlin data class.
 */
data class Enrollment(
    val studentId: String="",
    val batchId: String="",
    val enrollmentDate: String?=null,
    val status: String?=null
)