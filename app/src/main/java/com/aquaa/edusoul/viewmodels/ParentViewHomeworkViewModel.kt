package com.aquaa.edusoul.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.aquaa.edusoul.models.ParentHomeworkDetails
import com.aquaa.edusoul.models.Student
import com.aquaa.edusoul.models.StudentAssignment
import com.aquaa.edusoul.repositories.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

class ParentViewHomeworkViewModel(
    private val studentRepository: StudentRepository,
    private val homeworkRepository: HomeworkRepository,
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val TAG = "ParentViewHomeworkVM"

    private val _children = MutableLiveData<List<Student>>()
    val children: LiveData<List<Student>> get() = _children

    private val _selectedChildId = MutableLiveData<String>()
    val selectedChildId: LiveData<String> get() = _selectedChildId // Expose for Activity to observe

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // LiveData for Toast messages
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> get() = _toastMessage

    // LiveData for file upload progress
    private val _uploadProgress = MutableLiveData<Int?>()
    val uploadProgress: LiveData<Int?> get() = _uploadProgress

    // LiveData for file upload success/failure
    private val _uploadSuccess = MutableLiveData<Boolean?>()
    val uploadSuccess: LiveData<Boolean?> get() = _uploadSuccess

    // LiveData for general error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // This LiveData block correctly handles switching student assignments
    val homeworks: LiveData<List<ParentHomeworkDetails>> = _selectedChildId.switchMap { studentId ->
        liveData(Dispatchers.IO) {
            _isLoading.postValue(true) // Start loading indicator

            if (studentId.isBlank()) {
                emit(emptyList())
                Log.d(TAG, "Homeworks LiveData: studentId is blank, emitting empty list.")
                _isLoading.postValue(false)
                return@liveData
            }

            try {
                // Combine multiple data sources into a single stream
                combine(
                    studentAssignmentRepository.getStudentAssignmentsByStudent(studentId),
                    homeworkRepository.getAllHomework(),
                    subjectRepository.getAllSubjects()
                ) { assignmentsForChild, allHomeworks, allSubjects ->
                    Log.d(TAG, "COMBINE INPUTS: Student ID: '$studentId'")
                    Log.d(TAG, "  Assignments for Child: ${assignmentsForChild.size} records. IDs: ${assignmentsForChild.map { it.id }}")
                    Log.d(TAG, "  All Homeworks: ${allHomeworks.size} records. IDs: ${allHomeworks.map { it.id }}")
                    Log.d(TAG, "  All Subjects: ${allSubjects.size} records. IDs: ${allSubjects.map { it.id }}")

                    val homeworksMap = allHomeworks.associateBy { it.id }
                    val subjectsMap = allSubjects.associateBy { it.id }

                    // Fetch student details
                    val student = studentRepository.getStudentById(studentId)
                    if (student == null) {
                        Log.w(TAG, "Homeworks LiveData: Selected student with ID $studentId not found during homework processing.")
                        return@combine emptyList<ParentHomeworkDetails>() // Return empty list if student not found
                    }

                    // Map assignments to ParentHomeworkDetails
                    val homeworkDetails = assignmentsForChild.mapNotNull { assignment ->
                        val homework = homeworksMap[assignment.homeworkId]
                        val subject = homework?.subjectId?.let { subjectsMap[it] }

                        if (homework != null && subject != null) {
                            Log.d(TAG, "  Mapping Assignment ID: ${assignment.id} -> Homework ID: ${homework.id} -> Subject ID: ${subject.id}")
                            ParentHomeworkDetails(
                                studentAssignmentId = assignment.id,
                                homeworkId = homework.id,
                                studentId = student.id,
                                assignedDate = assignment.assignedDate,
                                dueDate = assignment.dueDate,
                                status = assignment.status,
                                submissionDate = assignment.submissionDate,
                                marksObtained = assignment.marksObtained,
                                remarks = assignment.remarks,
                                submissionPath = assignment.submissionPath,
                                submissionMimeType = assignment.submissionMimeType,
                                homeworkTitle = homework.title,
                                homeworkDescription = homework.description,
                                homeworkMaxMarks = homework.maxMarks,
                                homeworkType = homework.homeworkType,
                                homeworkAttachmentPath = homework.attachmentPath,
                                homeworkAttachmentMimeType = homework.attachmentMimeType,
                                studentName = student.fullName,
                                subjectName = subject.subjectName
                            )
                        } else {
                            Log.w(TAG, "  Skipping Assignment ID: ${assignment.id} because Homework (found: ${homework!=null}) or Subject (found: ${subject!=null}) was null after lookup.")
                            null
                        }
                    }
                    Log.d(TAG, "COMBINE EMITTING: Final list of ${homeworkDetails.size} ParentHomeworkDetails for student $studentId.")
                    homeworkDetails.sortedByDescending { it.dueDate } // Sort by due date
                }.collect {
                    emit(it) // Emit the combined and mapped data
                    _isLoading.postValue(false) // Stop loading indicator after successful emission
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in homeworks LiveData block for student $studentId: ${e.message}", e)
                _errorMessage.postValue("Failed to load homework: ${e.message}") // Post error message
                emit(emptyList()) // Emit empty list on error
                _isLoading.postValue(false) // Stop loading indicator on error
            }
        }
    }

    fun loadChildren(parentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                studentRepository.getStudentsByParent(parentId).collect { studentsList ->
                    Log.d(TAG, "loadChildren: Fetched ${studentsList.size} children for parent $parentId.")
                    _children.postValue(studentsList)
                    // Auto-select the first child if no child is currently selected
                    if (_selectedChildId.value.isNullOrBlank() && studentsList.isNotEmpty()) {
                        _selectedChildId.postValue(studentsList.first().id)
                        Log.d(TAG, "loadChildren: Auto-selected first child: ${studentsList.first().fullName} (${studentsList.first().id})")
                    } else if (_selectedChildId.value != null && studentsList.isNotEmpty()) {
                        // If a child was previously selected, ensure it's still in the list, otherwise default to first
                        val currentSelectionExists = studentsList.any { it.id == _selectedChildId.value }
                        if (!currentSelectionExists) {
                            _selectedChildId.postValue(studentsList.first().id)
                            Log.d(TAG, "loadChildren: Previous selection not found, auto-selected first child: ${studentsList.first().fullName} (${studentsList.first().id})")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading children: ${e.message}", e)
                _errorMessage.postValue("Failed to load children: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadHomeworkForChild(studentId: String) {
        // Only update if the studentId is different to avoid unnecessary re-triggers
        if (_selectedChildId.value != studentId) {
            Log.d(TAG, "loadHomeworkForChild: Updating selected child ID to '$studentId'.")
            _selectedChildId.value = studentId // This will trigger the homeworks LiveData switchMap
        } else {
            Log.d(TAG, "loadHomeworkForChild: Child ID '$studentId' already selected, skipping update.")
        }
    }

    // New function to handle file upload directly in the ViewModel
    // Updated signature to receive filename and mimeType as strings from Activity
    fun uploadHomeworkSubmission(studentAssignmentId: String, fileUri: Uri, fileName: String, mimeType: String?) {
        Log.d(TAG, "uploadHomeworkSubmission: Received studentAssignmentId: $studentAssignmentId")
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true) // Start loading indicator
            _uploadProgress.postValue(0) // Reset progress
            _uploadSuccess.postValue(null) // Reset success status

            try {
                val assignment = studentAssignmentRepository.getStudentAssignmentById(studentAssignmentId)
                if (assignment == null) {
                    _errorMessage.postValue("Error: Assignment not found for upload.")
                    _uploadSuccess.postValue(false)
                    Log.e(TAG, "uploadHomeworkSubmission: Assignment $studentAssignmentId not found.")
                    return@launch
                }

                val userId = assignment.studentId // Assuming assignment has studentId
                val storageRef = FirebaseStorage.getInstance().reference
                val submissionRef = storageRef.child("submissions/$userId/${UUID.randomUUID()}_${fileName}")

                val uploadTask = submissionRef.putFile(fileUri)

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    _uploadProgress.postValue(progress)
                    Log.d(TAG, "Upload progress: $progress%")
                }.addOnSuccessListener {
                    submissionRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // --- CRITICAL CHANGE START ---
                        // Instead of directly submitting, just update the assignment with the submission path and MIME type.
                        // The status remains 'Assigned' or an intermediate 'Uploaded' status until the parent clicks 'Submit'.
                        viewModelScope.launch(Dispatchers.IO) {
                            val updatedAssignment = assignment.copy(
                                submissionPath = downloadUri.toString(),
                                submissionMimeType = mimeType
                                // Do NOT change status to SUBMITTED here. Keep it ASSIGNED or similar.
                                // status = StudentAssignment.STATUS_SUBMITTED, // REMOVED THIS LINE
                                // submissionDate = currentDate // REMOVED THIS LINE
                            )
                            studentAssignmentRepository.updateStudentAssignment(updatedAssignment)
                            _uploadSuccess.postValue(true)
                            _toastMessage.postValue("File uploaded. Click 'Submit' to finalize.") // Inform user to click submit
                            Log.d(TAG, "File uploaded for assignment $studentAssignmentId. Path updated. Download URL: $downloadUri")
                        }
                        // --- CRITICAL CHANGE END ---
                    }.addOnFailureListener { downloadUrlException ->
                        _errorMessage.postValue("Failed to get download URL: ${downloadUrlException.message}")
                        _uploadSuccess.postValue(false)
                        Log.e(TAG, "Failed to get download URL: ${downloadUrlException.message}", downloadUrlException)
                    }
                }.addOnFailureListener { exception ->
                    _errorMessage.postValue("Upload failed: ${exception.message}")
                    _uploadSuccess.postValue(false)
                    Log.e(TAG, "Upload failed for assignment $studentAssignmentId: ${exception.message}", exception)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error during upload process: ${e.message}")
                _uploadSuccess.postValue(false)
                Log.e(TAG, "Error during upload process for $studentAssignmentId: ${e.message}", e)
            } finally {
                // Keep loading indicator on until parent clicks submit or error is cleared if this is used as overall loading
                // For now, let's keep it tied to upload progress only.
                // _isLoading.postValue(false) // This can be managed by the Activity based on _uploadProgress
            }
        }
    }

    fun submitHomework(assignmentId: String, submissionPath: String, submissionMimeType: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val assignment = studentAssignmentRepository.getStudentAssignmentById(assignmentId)
                if (assignment != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    val updatedAssignment = assignment.copy(
                        submissionPath = submissionPath, // Ensure path is still correct
                        submissionMimeType = submissionMimeType, // Ensure mime type is still correct
                        status = StudentAssignment.STATUS_SUBMITTED, // NOW set status to Submitted
                        submissionDate = currentDate
                    )
                    studentAssignmentRepository.updateStudentAssignment(updatedAssignment)
                    _toastMessage.postValue("Homework submitted successfully.")
                    Log.d(TAG, "submitHomework: Submitted assignment ${assignment.id}.")
                } else {
                    _toastMessage.postValue("Failed to find assignment for submission.")
                    Log.e(TAG, "submitHomework: Assignment ${assignmentId} not found for submission.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting homework: ${e.message}", e)
                _errorMessage.postValue("Failed to submit homework: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun deleteSubmission(assignmentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val assignment = studentAssignmentRepository.getStudentAssignmentById(assignmentId)
                if (assignment != null) {
                    val updatedAssignment = assignment.copy(
                        submissionPath = null,
                        submissionDate = null,
                        status = StudentAssignment.STATUS_ASSIGNED,
                        marksObtained = null,
                        remarks = null,
                        submissionMimeType = null
                    )
                    studentAssignmentRepository.updateStudentAssignment(updatedAssignment)
                    _toastMessage.postValue("Submission deleted. You can now re-submit.")
                    Log.d(TAG, "deleteSubmission: Deleted submission for assignment ${assignment.id}.")
                } else {
                    _toastMessage.postValue("Failed to find assignment to delete submission.")
                    Log.e(TAG, "deleteSubmission: Assignment ${assignmentId} not found for deletion.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting submission: ${e.message}", e)
                _errorMessage.postValue("Failed to delete submission: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Clear LiveData messages after they are consumed by the UI
    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun clearUploadStatus() {
        _uploadProgress.value = null
        _uploadSuccess.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}