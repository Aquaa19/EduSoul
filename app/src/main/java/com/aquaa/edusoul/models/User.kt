package com.aquaa.edusoul.models

import android.support.v4.media.session.MediaSessionCompat.Token
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    var id: String = "",
    var username: String? = null,
    var passwordHash: String = "",
    var fullName: String = "",
    var email: String? = null,
    var phoneNumber: String? = null,
    var role: String = "",
    var profileImagePath: String? = null,
    var fcmToken: String? = null,
    var registrationDate: String? = null
) {
    override fun toString(): String {
        return fullName
    }

    companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_TEACHER = "TEACHER"
        const val ROLE_PARENT = "PARENT"
        const val ROLE_OWNER = "OWNER"
        const val ROLE_MANAGER = "MANAGER"
    }
}