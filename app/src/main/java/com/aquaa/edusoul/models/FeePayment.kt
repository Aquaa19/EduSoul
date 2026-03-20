package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId

/**
 * Data class for FeePayment.
 * Represents a record of a fee payment made by a student.
 */
data class FeePayment(
    @DocumentId
    var id: String = "",
    val studentId: String = "", // Added default value
    var feeStructureId: String? = null,
    val paymentDate: String = "", // Added default value
    val amountPaid: Double = 0.0, // Added default value
    val paymentMethod: String? = null,
    val paymentPeriod: String? = null,
    var status: String? = null,
    val remarks: String? = null,
    val recordedByUserId: String? = null
) {
    companion object {
        const val STATUS_PAID = "Paid"
        const val STATUS_PARTIALLY_PAID = "Partial"
        const val STATUS_DUE = "Due"
        const val STATUS_OVERPAID = "Overpaid"
        const val STATUS_OVERDUE = "Overdue"
    }
}