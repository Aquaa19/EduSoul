// File: src/main/java/com/aquaa/edusoul/repositories/UserRepository.kt
package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val TAG = "UserRepository"

    /**
     * Retrieves a specific user from Firestore by their document ID.
     * @param userId The Firestore document ID of the user.
     * @return The User object if found, null otherwise.
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                Log.d(TAG, "Successfully fetched user by ID '$userId'.")
                document.toObject(User::class.java)
            } else {
                Log.w(TAG, "User with ID '$userId' not found.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by ID '$userId': ${e.message}", e)
            null
        }
    }

    /**
     * Retrieves all users from Firestore, ordered by full name ascending.
     * Emits a list of User objects as a Flow.
     */
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val subscription = usersCollection.orderBy("fullName").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all users failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val users = snapshot.toObjects(User::class.java)
                    Log.d(TAG, "Successfully fetched/updated all users. Count: ${users.size}")
                    trySend(users).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to User list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves all parent users from Firestore, ordered by full name ascending.
     * Emits a list of User objects as a Flow.
     */
    fun getAllParentUsers(): Flow<List<User>> = callbackFlow {
        val subscription = usersCollection.whereEqualTo("role", User.ROLE_PARENT).orderBy("fullName").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all parent users failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val parentUsers = snapshot.toObjects(User::class.java)
                    Log.d(TAG, "Successfully fetched/updated all parent users. Count: ${parentUsers.size}")
                    trySend(parentUsers).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to Parent User list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves all teacher users from Firestore, ordered by full name ascending.
     * Emits a list of User objects as a Flow.
     */
    fun getAllTeachers(): Flow<List<User>> = callbackFlow {
        Log.d(TAG, "Flow: getAllTeachers() started.")

        val subscription = usersCollection
            .whereEqualTo("role", User.ROLE_TEACHER)
            .orderBy("fullName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Flow: Listen for all teachers failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val teachers = snapshot.toObjects(User::class.java)
                        Log.d(TAG, "Flow: Fetched ${teachers.size} teachers from Firestore.")
                        teachers.forEach { Log.v(TAG, "Flow: Teacher item: ${it.fullName} (ID: ${it.id}, Role: ${it.role})") }
                        trySend(teachers).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Flow: Error converting snapshot to User list for teachers: ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    Log.d(TAG, "Flow: No teachers found or snapshot is null.")
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose {
            Log.d(TAG, "Flow: Removing listener for all teachers.")
            subscription.remove()
        }
    }

    /**
     * Inserts a new user into Firestore.
     * @param user The User object to insert.
     * @return The Firestore document ID of the newly created user, or an empty string if insertion fails.
     */
    suspend fun insertUser(user: User): String {
        return try {
            // If user.id is already set (e.g., from a migration), use it as the document ID.
            // Otherwise, let Firestore auto-generate.
            val documentId = if (user.id.isNotBlank()) user.id else usersCollection.document().id
            usersCollection.document(documentId).set(user).await()
            Log.d(TAG, "User inserted with ID: $documentId")
            documentId
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user: ${e.message}", e)
            "" // Return empty string to indicate failure
        }
    }

    /**
     * Updates an existing user in Firestore.
     * @param user The User object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateUser(user: User): Int {
        return try {
            if (user.id.isNotBlank()) {
                usersCollection.document(user.id).set(user).await()
                Log.d(TAG, "User updated with ID: ${user.id}")
                1 // Indicate success
            } else {
                Log.e(TAG, "Cannot update user: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Deletes a user from Firestore by their document ID.
     * @param userId The Firestore document ID of the user to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteUserById(userId: String): Int {
        return try {
            usersCollection.document(userId).delete().await()
            Log.d(TAG, "User deleted with ID: $userId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Searches for users in Firestore where the full name matches the query.
     * Emits a list of matching User objects as a Flow.
     * @param query The search string.
     */
    fun searchUsers(query: String): Flow<List<User>> = callbackFlow {
        val firebaseQuery = if (query.isNotBlank()) {
            usersCollection.whereGreaterThanOrEqualTo("fullName", query)
                .whereLessThanOrEqualTo("fullName", query + '\uf8ff')
        } else {
            usersCollection // If query is empty, return all users
        }

        val subscription = firebaseQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for user search failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val searchedUsers = snapshot.toObjects(User::class.java)
                    Log.d(TAG, "Successfully fetched/updated searched users with query '$query'. Found: ${searchedUsers.size}")
                    trySend(searchedUsers).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to User list during search: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }
}