// File: main/java/com/aquaa/edusoul/auth/AuthManager.kt
package com.aquaa.edusoul.auth

import android.content.Context
import android.util.Log
import com.aquaa.edusoul.models.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthManager(private val context: Context) {

    companion object {
        private const val TAG = "AuthManagerFirebase"
        private const val USERS_COLLECTION = "users"
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(email: String, password: String, fullName: String, username: String?, role: String): User? {
        if (email.isBlank() || password.isBlank() || fullName.isBlank() || role.isBlank()) {
            Log.e(TAG, "Email, password, full name, and role cannot be empty for registration.")
            return null
        }

        try {
            val signInMethods = auth.fetchSignInMethodsForEmail(email).await().signInMethods
            if (signInMethods != null && signInMethods.isNotEmpty()) {
                Log.e(TAG, "Registration failed: Email address '$email' is already in use.")
                return null
            }

            if (!username.isNullOrBlank() && isUsernameExists(username)) {
                Log.e(TAG, "Username '$username' already exists.")
                return null
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return null

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val fcmToken = FirebaseMessaging.getInstance().token.await()

            val user = User(
                id = firebaseUser.uid,
                username = username ?: "",
                fullName = fullName,
                email = email,
                phoneNumber = null,
                role = role,
                profileImagePath = null,
                fcmToken = fcmToken,
                registrationDate = getCurrentDateTime()
            )

            firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).set(user).await()
            Log.i(TAG, "User registered successfully with UID: ${firebaseUser.uid}")
            return user

        } catch (e: FirebaseAuthUserCollisionException) {
            Log.e(TAG, "Registration failed: Email address is already in use (FirebaseAuthUserCollisionException).", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error during user registration: ${e.message}", e)
            return null
        }
    }

    suspend fun loginUser(email: String, password: String): FirebaseUser? {
        if (email.isBlank() || password.isBlank()) {
            Log.e(TAG, "Email or password cannot be empty for login.")
            return null
        }
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            Log.i(TAG, "User '$email' logged in successfully.")

            authResult.user?.let { firebaseUser ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val token = task.result
                        firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).update("fcmToken", token)
                            .addOnSuccessListener { Log.d(TAG, "FCM token updated in Firestore for user ${firebaseUser.uid}") }
                            .addOnFailureListener { e -> Log.w(TAG, "Error updating FCM token", e) }
                    }
                }
            }
            return authResult.user
        } catch (e: Exception) {
            Log.e(TAG, "Error during login: ${e.message}", e)
            return null
        }
    }

    suspend fun isUsernameExists(username: String): Boolean {
        if (username.isBlank()) return false
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if username exists: ${e.message}", e)
            false
        }
    }

    suspend fun getUserByUsername(username: String): User? {
        if (username.isBlank()) return null
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            querySnapshot.documents.firstOrNull()?.toObject(User::class.java)?.apply {
                this.id = querySnapshot.documents.first().id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by username '$username': ${e.message}", e)
            null
        }
    }

    suspend fun isEmailExists(email: String): Boolean {
        return try {
            val signInMethods = auth.fetchSignInMethodsForEmail(email).await().signInMethods
            signInMethods != null && signInMethods.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if email exists in Firebase Auth: ${e.message}", e)
            false
        }
    }

    suspend fun updatePassword(newPassword: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.updatePassword(newPassword).await()
            Log.i(TAG, "Password updated successfully for user: ${user.uid}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating password", e)
            false
        }
    }

    fun logoutUser() {
        auth.signOut()
        Log.i(TAG, "User logged out.")
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun getLoggedInUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return try {
            val documentSnapshot = firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).get().await()
            documentSnapshot.toObject(User::class.java)?.apply {
                this.id = documentSnapshot.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile from Firestore: ${e.message}", e)
            null
        }
    }

    // Helper to get current Firebase User ID directly
    fun getCurrentFirebaseUserId(): String? {
        return auth.currentUser?.uid
    }

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}