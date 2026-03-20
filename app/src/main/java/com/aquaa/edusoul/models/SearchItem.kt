package com.aquaa.edusoul.models

import com.aquaa.edusoul.R

// A sealed class or interface is ideal for representing different types of search results
sealed class SearchItem {
    data class StudentItem(val student: Student) : SearchItem()
    data class UserItem(val user: User) : SearchItem()
    data class SubjectItem(val subject: Subject) : SearchItem()
    data class BatchItem(val batch: Batch) : SearchItem()
    // Add more types as you implement search for other entities (e.g., Homework, Exam, Announcement)

    // Helper properties for display in a generic RecyclerView
    fun getTitle(): String {
        return when (this) {
            is StudentItem -> student.fullName
            is UserItem -> user.fullName
            is SubjectItem -> subject.subjectName
            is BatchItem -> batch.batchName
            // Add more cases for other item types
        }
    }

    fun getSubtitle(): String {
        return when (this) {
            is StudentItem -> "Adm No: ${student.admissionNumber ?: "N/A"}"
            is UserItem -> "Role: ${user.role}"
            is SubjectItem -> "Code: ${subject.subjectCode ?: "N/A"}"
            is BatchItem -> "Grade: ${batch.gradeLevel ?: "N/A"}"
            // Add more cases for other item types
        }
    }

    fun getIconResId(): Int {
        // You'll need to define appropriate icons or use placeholders
        return when (this) {
            is StudentItem -> R.drawable.ic_student // Assuming you have this drawable
            is UserItem -> when(user.role) { // Assuming you have specific user role icons
                User.ROLE_TEACHER -> R.drawable.ic_teacher
                User.ROLE_PARENT -> R.drawable.ic_parent
                User.ROLE_ADMIN -> R.drawable.ic_manage_users // Or a specific admin icon
                User.ROLE_OWNER -> R.drawable.ic_manage_users // Or a specific owner icon
                else -> R.drawable.ic_person_placeholder
            }
            is SubjectItem -> R.drawable.ic_subjects
            is BatchItem -> R.drawable.ic_batches
            // Add more cases
            else -> R.drawable.ic_search // Default placeholder
        }
    }
}