package com.aquaa.edusoul.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Student(
    @DocumentId
    var id: String = "",
    var fullName: String = "",
    var dateOfBirth: String? = null,
    var gender: String? = null,
    var gradeOrClass: String? = null,
    var admissionNumber: String? = null,
    var admissionDate: String? = null,
    var parentUserId: String? = null,
    var schoolName: String? = null,
    var address: String? = null,
    var profileImagePath: String? = null,
    var notes: String? = null,
    var stability: Int = 0
) : Parcelable {
    override fun toString(): String {
        return fullName + " (ID: " + id + ")"
    }
}