package com.aquaa.edusoul.models

import com.google.firebase.firestore.DocumentId // Ensure this import is present
import com.google.firebase.firestore.PropertyName // Keep if you use it for other fields or if needed

/**
 * Represents a Fee Structure.
 */
data class FeeStructure(
    @DocumentId // <--- Crucial for Firestore ID mapping
    var id: String = "",
    var title: String = "",
    var description: String? = null,
    var amount: Double? = null,
    var duration: String? = null
) {
    override fun toString(): String {
        return title + (if (amount != null) " - $amount" else "")
    }
}