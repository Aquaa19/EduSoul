import java.io.Serializable

/**
 * Model data class to encapsulate detailed fee payment information for a parent's view.
 * It combines data from FeePayment, Student, FeeStructure, and User entities.
 * Converted from Java POJO to Kotlin data class.
 */
data class ParentFeePaymentDetails(
    // From FeePayment
    var paymentId: String = "",
    val studentId: String = "", // Added default value
    val feeStructureId: String? = null,
    val paymentDate: String = "", // Added default value
    val amountPaid: Double = 0.0, // Added default value
    val paymentMethod: String? = null,
    val paymentPeriod: String? = null,
    val paymentStatus: String? = null,
    val paymentRemarks: String? = null,
    val recordedByUserId: String? = null,

    // From Student (joined data)
    val studentName: String = "", // Added default value

    // From FeeStructure (if linked, joined data)
    val feeStructureTitle: String? = null,
    val feeStructureAmount: Double? = null, // Default for nullable Double is null
    val feeStructureFrequency: String? = null,

    // From User (who recorded payment, if available, joined data)
    val recordedByName: String? = null
) : Serializable