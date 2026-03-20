package com.aquaa.edusoul.repositories

import android.util.Log
import com.aquaa.edusoul.models.FeePayment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class FeePaymentRepository {

    // IMPORTANT: Ensure this matches the actual Firestore collection name.
    // Based on your latest report, it seems "feePayments" (camelCase) is being used.
    private val db = FirebaseFirestore.getInstance()
    private val feePaymentsCollection = db.collection("feePayments") // Confirmed: Using camelCase
    private val TAG = "FeePaymentRepository"

    /**
     * Inserts a new fee payment into Firestore.
     * If the message has an ID, it attempts to set it. If not, Firestore generates one.
     * @param feePayment The FeePayment object to insert.
     * @return The ID of the inserted message, or an empty string on failure.
     */
    suspend fun insertFeePayment(feePayment: FeePayment): String {
        return try {
            val docRef = if (feePayment.id.isNotBlank()) feePaymentsCollection.document(feePayment.id) else feePaymentsCollection.document()
            feePayment.id = docRef.id // Ensure the feePayment object has the Firestore ID
            docRef.set(feePayment).await()
            Log.d(TAG, "Fee payment inserted with ID: ${feePayment.id}")
            feePayment.id
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting fee payment: ${e.message}", e)
            "" // Return empty string on failure
        }
    }

    /**
     * Updates an existing fee payment in Firestore.
     * @param feePayment The FeePayment object to update. Must contain a valid Firestore document ID.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun updateFeePayment(feePayment: FeePayment): Int {
        return try {
            if (feePayment.id.isNotBlank()) {
                feePaymentsCollection.document(feePayment.id).set(feePayment).await()
                Log.d(TAG, "Fee payment updated with ID: ${feePayment.id}")
                1 // Indicate success
            } else {
                Log.e(TAG, "Cannot update fee payment: ID is blank.")
                0 // Indicate failure
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating fee payment: ${e.message}", e)
            0 // Indicate failure
        }
    }

    /**
     * Retrieves all fee payments from Firestore in real-time, ordered by payment date descending.
     * Emits a list of FeePayment objects as a Flow whenever the data changes.
     */
    fun getAllFeePayments(): Flow<List<FeePayment>> = callbackFlow {
        val subscription = feePaymentsCollection.orderBy("paymentDate", Query.Direction.DESCENDING).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen for all fee payments failed: ${e.message}", e)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val payments = snapshot.toObjects(FeePayment::class.java)
                    Log.d(TAG, "Fetched ${payments.size} fee payments from 'feePayments' collection.") // Updated log
                    trySend(payments).isSuccess
                } catch (castException: Exception) {
                    Log.e(TAG, "Error converting snapshot to FeePayment list: ${castException.message}", castException)
                    close(castException)
                }
            } else {
                Log.d(TAG, "No fee payments found in 'feePayments' collection or snapshot is empty.") // Updated log
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves fee payments for a specific student from Firestore in real-time, ordered by payment date descending.
     * Emits a list of FeePayment objects as a Flow whenever the data changes.
     * @param studentId The ID of the student.
     */
    fun getFeePaymentsForStudent(studentId: String): Flow<List<FeePayment>> = callbackFlow {
        val subscription = feePaymentsCollection.whereEqualTo("studentId", studentId)
            .orderBy("paymentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for fee payments for student '$studentId' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val payments = snapshot.toObjects(FeePayment::class.java)
                        trySend(payments).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to FeePayment list for student '$studentId': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves fee payments for a specific payment period (year-month) from Firestore in real-time,
     * ordered by payment date descending.
     * Emits a list of FeePayment objects as a Flow whenever the data changes.
     * @param yearMonth The payment period in "yyyy-MM" format.
     */
    fun getFeePaymentsForPeriod(yearMonth: String): Flow<List<FeePayment>> = callbackFlow {
        val subscription = feePaymentsCollection.whereEqualTo("paymentPeriod", yearMonth)
            .orderBy("paymentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen for fee payments for period '$yearMonth' failed: ${e.message}", e)
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    try {
                        val payments = snapshot.toObjects(FeePayment::class.java)
                        trySend(payments).isSuccess
                    } catch (castException: Exception) {
                        Log.e(TAG, "Error converting snapshot to FeePayment list for period '$yearMonth': ${castException.message}", castException)
                        close(castException)
                    }
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Retrieves a specific fee payment from Firestore by its document ID.
     * @param feePaymentId The Firestore document ID of the fee payment.
     * @return The FeePayment object if found, null otherwise.
     */
    suspend fun getFeePaymentById(feePaymentId: String): FeePayment? {
        return try {
            val document = feePaymentsCollection.document(feePaymentId).get().await()
            document.toObject(FeePayment::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting fee payment by ID '$feePaymentId': ${e.message}", e)
            null
        }
    }

    /**
     * Deletes a fee payment from Firestore by its document ID.
     * @param feePaymentId The Firestore document ID of the fee payment to delete.
     * @return The number of documents affected (1 for success, 0 for failure).
     */
    suspend fun deleteFeePaymentById(feePaymentId: String): Int {
        return try {
            feePaymentsCollection.document(feePaymentId).delete().await()
            Log.d(TAG, "Fee payment deleted with ID: $feePaymentId")
            1 // Indicate success
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting fee payment by ID: ${e.message}", e)
            0 // Indicate failure
        }
    }
}
