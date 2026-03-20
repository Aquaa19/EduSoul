package com.aquaa.edusoul.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.aquaa.edusoul.activities.admin.AttendanceReportActivity
import com.aquaa.edusoul.activities.admin.ManageAnnouncementsActivity
import com.aquaa.edusoul.activities.admin.ManageClassScheduleActivity
import com.aquaa.edusoul.activities.admin.ManageExamsActivity
import com.aquaa.edusoul.activities.admin.StatusReportsDashboardActivity

// Define constants for Intent extras to pass data
object AiAssistantConstants {
    // For Class Session
    const val EXTRA_SUBJECT_NAME = "extra_subject_name"
    const val EXTRA_BATCH_NAME = "extra_batch_name"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
    const val EXTRA_START_TIME = "extra_start_time"
    const val EXTRA_END_TIME = "extra_end_time"
    const val EXTRA_TEACHER_NAME = "extra_teacher_name"

    // For Announcement
    const val EXTRA_ANNOUNCEMENT_TITLE = "extra_announcement_title"
    const val EXTRA_ANNOUNCEMENT_CONTENT = "extra_announcement_content"
    const val EXTRA_ANNOUNCEMENT_AUDIENCE = "extra_announcement_audience"

    // For Exam
    const val EXTRA_EXAM_NAME = "extra_exam_name"
    const val EXTRA_EXAM_SUBJECT = "extra_exam_subject"
    const val EXTRA_EXAM_BATCH = "extra_exam_batch" // Optional
    const val EXTRA_EXAM_DATE = "extra_exam_date"
    const val EXTRA_EXAM_MAX_MARKS = "extra_exam_max_marks"


    const val EXTRA_PREFILL_DATA = "extra_prefill_data" // General flag to indicate pre-fill
}

class AiAssistantManager {

    /**
     * Processes a given text command and attempts to perform an action,
     * such as launching a specific activity or preparing data for it.
     *
     * @param context The Android Context, typically an Activity.
     * @param command The text command entered by the user.
     */
    fun processCommand(context: Context, command: String) {
        val lowerCaseCommand = command.lowercase().trim()

        when {
            // General commands for opening activities
            lowerCaseCommand.contains("open attendance reports") || lowerCaseCommand.contains("show attendance") -> {
                val intent = Intent(context, AttendanceReportActivity::class.java)
                context.startActivity(intent)
                Toast.makeText(context, "Opening Attendance Reports...", Toast.LENGTH_SHORT).show()
            }
            // Updated to handle complex commands
            lowerCaseCommand.startsWith("give announcement:") || lowerCaseCommand.startsWith("post announcement:") -> {
                parseAndGiveAnnouncement(context, command)
            }
            // New complex command for adding exams
            lowerCaseCommand.startsWith("add exam:") || lowerCaseCommand.startsWith("create exam:") -> {
                parseAndAddExam(context, command)
            }
            // Generic Exam Management command, opens the main activity without pre-filling
            lowerCaseCommand.contains("exam management") || lowerCaseCommand.contains("manage exams") || lowerCaseCommand.contains("edit exams") -> {
                val intent = Intent(context, ManageExamsActivity::class.java)
                context.startActivity(intent)
                Toast.makeText(context, "Opening Exam Management screen (unspecified parameters)...", Toast.LENGTH_SHORT).show()
            }
            lowerCaseCommand.contains("open status reports") || lowerCaseCommand.contains("show reports dashboard") -> {
                val intent = Intent(context, StatusReportsDashboardActivity::class.java)
                context.startActivity(intent)
                Toast.makeText(context, "Opening Status Reports Dashboard...", Toast.LENGTH_SHORT).show()
            }

            // Specific command for adding a class session with parameters
            lowerCaseCommand.startsWith("add class session:") || lowerCaseCommand.startsWith("create class session:") -> {
                parseAndAddClassSession(context, command)
            }
            else -> {
                Toast.makeText(context, "Command not recognized. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Parses the "add class session" command and launches ManageClassScheduleActivity with extras.
     * Expected format: "add class session: Subject:Mathematics, Batch:Grade 10 Warriors, Day:Monday, Start:09:00, End:10:00, Teacher:Mr. Smith"
     */
    private fun parseAndAddClassSession(context: Context, command: String) {
        val paramsString = command.substringAfter(":", "").trim()
        val paramsMap = parseParams(paramsString)

        val subjectName = paramsMap["subject"]
        val batchName = paramsMap["batch"]
        val dayOfWeekStr = paramsMap["day"]
        val startTime = paramsMap["start"]
        val endTime = paramsMap["end"]
        val teacherName = paramsMap["teacher"]

        if (subjectName.isNullOrBlank() || batchName.isNullOrBlank() ||
            dayOfWeekStr.isNullOrBlank() || startTime.isNullOrBlank() ||
            endTime.isNullOrBlank() || teacherName.isNullOrBlank()) {
            Toast.makeText(context, "Missing details for adding class session. Please provide Subject, Batch, Day, Start Time, End Time, and Teacher.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(context, ManageClassScheduleActivity::class.java).apply {
            putExtra(AiAssistantConstants.EXTRA_PREFILL_DATA, true)
            putExtra(AiAssistantConstants.EXTRA_SUBJECT_NAME, subjectName)
            putExtra(AiAssistantConstants.EXTRA_BATCH_NAME, batchName)
            putExtra(AiAssistantConstants.EXTRA_DAY_OF_WEEK, dayOfWeekStr)
            putExtra(AiAssistantConstants.EXTRA_START_TIME, startTime)
            putExtra(AiAssistantConstants.EXTRA_END_TIME, endTime)
            putExtra(AiAssistantConstants.EXTRA_TEACHER_NAME, teacherName)
        }
        context.startActivity(intent)
        Toast.makeText(context, "Preparing to add class session...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Parses the "give announcement" command and launches ManageAnnouncementsActivity with extras.
     * Expected format: "give announcement: Title:Important Update, Content:School closed tomorrow due to heavy rain, Audience:All"
     */
    private fun parseAndGiveAnnouncement(context: Context, command: String) {
        val paramsString = command.substringAfter(":", "").trim()
        val paramsMap = parseParams(paramsString)

        val title = paramsMap["title"]
        val content = paramsMap["content"]
        val audience = paramsMap["audience"]

        if (title.isNullOrBlank() || content.isNullOrBlank() || audience.isNullOrBlank()) {
            Toast.makeText(context, "Missing details for announcement. Please provide Title, Content, and Audience (ALL, PARENTS, TEACHERS).", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(context, ManageAnnouncementsActivity::class.java).apply {
            putExtra(AiAssistantConstants.EXTRA_PREFILL_DATA, true)
            putExtra(AiAssistantConstants.EXTRA_ANNOUNCEMENT_TITLE, title)
            putExtra(AiAssistantConstants.EXTRA_ANNOUNCEMENT_CONTENT, content)
            putExtra(AiAssistantConstants.EXTRA_ANNOUNCEMENT_AUDIENCE, audience.uppercase()) // Ensure audience is uppercase for consistency
        }
        context.startActivity(intent)
        Toast.makeText(context, "Preparing to give announcement...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Parses the "add exam" command and launches ManageExamsActivity with extras.
     * Expected format: "add exam: Name:Mid-Term Math, Subject:Mathematics, Batch:Grade 10 Warriors, Date:2025-07-20, MaxMarks:100"
     */
    private fun parseAndAddExam(context: Context, command: String) {
        val paramsString = command.substringAfter(":", "").trim()
        val paramsMap = parseParams(paramsString)

        val examName = paramsMap["name"]
        val subjectName = paramsMap["subject"]
        val batchName = paramsMap["batch"]
        val examDate = paramsMap["date"]
        val maxMarks = paramsMap["maxmarks"]?.toIntOrNull() // Convert to Int

        if (examName.isNullOrBlank() || subjectName.isNullOrBlank() ||
            examDate.isNullOrBlank() || maxMarks == null) {
            Toast.makeText(context, "Missing details for adding exam. Please provide Name, Subject, Date, and MaxMarks. Batch is optional.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(context, ManageExamsActivity::class.java).apply {
            putExtra(AiAssistantConstants.EXTRA_PREFILL_DATA, true)
            putExtra(AiAssistantConstants.EXTRA_EXAM_NAME, examName)
            putExtra(AiAssistantConstants.EXTRA_EXAM_SUBJECT, subjectName)
            putExtra(AiAssistantConstants.EXTRA_EXAM_BATCH, batchName) // Batch is optional, can be null
            putExtra(AiAssistantConstants.EXTRA_EXAM_DATE, examDate)
            putExtra(AiAssistantConstants.EXTRA_EXAM_MAX_MARKS, maxMarks)
        }
        context.startActivity(intent)
        Toast.makeText(context, "Preparing to add exam...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Helper function to parse key-value pairs from a string.
     * Format: "Key1:Value1, Key2:Value2"
     */
    private fun parseParams(paramsString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        paramsString.split(",").forEach { pair ->
            // Fix: Changed " : " (String) to ' : ' (Char) and used named argument for limit.
            // This clearly specifies the Char delimiter overload and the limit.
            val parts = pair.split(':', limit = 2)
            if (parts.size == 2) {
                map[parts[0].trim().lowercase()] = parts[1].trim()
            }
        }
        return map
    }
}