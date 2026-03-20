package com.aquaa.edusoul.models

/*
 * StudentFeeStatus: A helper model data class to encapsulate
 * the fee status for a single student in a specific period.
 * Converted from Java POJO to Kotlin data class.
 */
data class StudentFeeStatus(
    val studentId: String="", // Changed to 'String' for Firestore compatibility
    val studentName: String="",
    val batchName: String="",
    var totalAmountDue: Double=0.0, // This is the expected amount for the period
    var totalAmountPaid: Double=0.0,
    var status: String="",
    var lastPaymentDate: String?=null
)