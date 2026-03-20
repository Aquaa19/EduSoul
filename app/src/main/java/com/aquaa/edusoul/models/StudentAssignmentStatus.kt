package com.aquaa.edusoul.models

/**
 * This is a helper model data class used for the grading screen.
 * It holds a StudentAssignment object, the student's name, and allows for
 * temporary storage of grade/remarks entered in the UI before they are saved.
 */
data class StudentAssignmentStatus(
    val assignment: StudentAssignment? = null, // Made nullable and defaulted to null
    val studentName: String = "",          // Defaulted to empty string
    var marksObtained: Double? = null,     // Already defaults to null
    var remarks: String? = null            // Already defaults to null
) {
    init {
        // Initialize with existing data from the assignment if available
        // Need to handle the case where assignment is null
        this.marksObtained = assignment?.marksObtained
        this.remarks = assignment?.remarks
    }

    // These properties proxy to the underlying assignment object for display purposes
    // Need to handle the case where assignment is null
    val submissionDate: String?
        get() = assignment?.submissionDate

    val submissionPath: String?
        get() = assignment?.submissionPath
}
