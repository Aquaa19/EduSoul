// src/main/java/com/aquaa/edusoul/dialogs/AiAssistantDialogFragment.kt
package com.aquaa.edusoul.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.aquaa.edusoul.R
import com.aquaa.edusoul.utils.AiAssistantManager
import com.aquaa.edusoul.utils.AiAssistantConstants
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.app.Activity
import android.content.pm.PackageManager
import com.aquaa.edusoul.models.Subject
import com.aquaa.edusoul.models.Batch
import com.aquaa.edusoul.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
// REMOVE THIS INCORRECT IMPORT: import java.util.jar.Manifest

// ADD THIS CORRECT IMPORT FOR ANDROID PERMISSIONS
import android.Manifest // This is the correct import for android.Manifest.permission


class AiAssistantDialogFragment : DialogFragment() {

    private lateinit var chatHistoryTextView: TextView
    private lateinit var commandEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var voiceInputButton: ImageButton

    // NEW: Updated AiAssistantListener interface
    interface AiAssistantListener {
        fun onCommandSent(command: String)
        fun onDialogDismissed()
        // Removed onAiResponseRequested as responses are handled internally by the dialog for conversational flow
        // New suspend functions to get entities by name
        suspend fun getSubjectByName(name: String): Subject?
        suspend fun getBatchByName(name: String): Batch?
        suspend fun getTeacherByName(name: String): User?
    }

    private var listener: AiAssistantListener? = null

    private var currentState: ConversationState = ConversationState.IDLE
    private val conversationData = mutableMapOf<String, String>() // Stores names or IDs based on context

    enum class ConversationState {
        IDLE,
        EXPECTING_CLASS_SESSION_SUBJECT,
        EXPECTING_CLASS_SESSION_BATCH,
        EXPECTING_CLASS_SESSION_DAY,
        EXPECTING_CLASS_SESSION_START_TIME,
        EXPECTING_CLASS_SESSION_END_TIME,
        EXPECTING_CLASS_SESSION_TEACHER,
        EXPECTING_ANNOUNCEMENT_TITLE,
        EXPECTING_ANNOUNCEMENT_CONTENT,
        EXPECTING_ANNOUNCEMENT_AUDIENCE,
        EXPECTING_EXAM_NAME,
        EXPECTING_EXAM_SUBJECT,
        EXPECTING_EXAM_BATCH,
        EXPECTING_EXAM_DATE,
        EXPECTING_EXAM_MAX_MARKS
    }

    companion object {
        const val TAG = "AiAssistantDialog"
    }

    // ActivityResultLauncher for SpeechRecognizer
    private val speechRecognizerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenText: String? = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let {
                commandEditText.setText(it)
                sendButton.performClick()
            }
        } else {
            Toast.makeText(context, "Speech input cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }

    // ActivityResultLauncher for requesting RECORD_AUDIO permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(context, "Permission denied to record audio. Cannot use voice input.", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure this style is actually available in your project.
        // As discussed before, com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_MinWidth
        // is often a more reliable choice for modern Android apps if you're using Material Design.
        setStyle(STYLE_NO_TITLE, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_ai_assistant, container, false)

        chatHistoryTextView = view.findViewById(R.id.chatHistoryTextView)
        commandEditText = view.findViewById(R.id.commandEditText)
        sendButton = view.findViewById(R.id.sendButton)
        voiceInputButton = view.findViewById(R.id.voiceInputButton)

        sendButton.setOnClickListener {
            val command = commandEditText.text.toString().trim()
            if (command.isNotBlank()) {
                addMessageToChat("You: $command")
                processUserReply(command)
                commandEditText.text.clear()
            }
        }

        voiceInputButton.setOnClickListener {
            checkAudioPermissionAndStartSpeechRecognition()
        }

        addMessageToChat("AI: How can I help you today?")

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun setAiAssistantListener(listener: AiAssistantListener) {
        this.listener = listener
    }

    fun addMessageToChat(message: String) {
        view?.post {
            chatHistoryTextView.append("\n$message")
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        listener?.onDialogDismissed()
        currentState = ConversationState.IDLE
        conversationData.clear()
    }

    // --- Voice Input Helper Functions ---
    private fun checkAudioPermissionAndStartSpeechRecognition() {
        // Now 'Manifest.permission' correctly refers to Android permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(context, "Speech recognition not available on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}", e)
            Toast.makeText(context, "Error starting speech recognition: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // --- Core conversational logic ---
    private fun processUserReply(reply: String) {
        val lowerCaseReply = reply.lowercase().trim()

        // Handle replies based on current state
        when (currentState) {
            ConversationState.EXPECTING_CLASS_SESSION_SUBJECT -> {
                validateAndStoreEntity(reply, "subject", AiAssistantConstants.EXTRA_SUBJECT_NAME) { name -> listener?.getSubjectByName(name) }
            }
            ConversationState.EXPECTING_CLASS_SESSION_BATCH -> {
                validateAndStoreEntity(reply, "batch", AiAssistantConstants.EXTRA_BATCH_NAME) { name -> listener?.getBatchByName(name) }
            }
            ConversationState.EXPECTING_CLASS_SESSION_DAY -> {
                conversationData["day"] = reply // Day is not validated against DB, just stored
                askForNextClassSessionDetail()
            }
            ConversationState.EXPECTING_CLASS_SESSION_START_TIME -> {
                conversationData["start"] = reply
                askForNextClassSessionDetail()
            }
            ConversationState.EXPECTING_CLASS_SESSION_END_TIME -> {
                conversationData["end"] = reply
                askForNextClassSessionDetail()
            }
            ConversationState.EXPECTING_CLASS_SESSION_TEACHER -> {
                validateAndStoreEntity(reply, "teacher", AiAssistantConstants.EXTRA_TEACHER_NAME) { name -> listener?.getTeacherByName(name) }
            }
            // Conversation states for Announcement
            ConversationState.EXPECTING_ANNOUNCEMENT_TITLE -> {
                conversationData["title"] = reply
                askForNextAnnouncementDetail()
            }
            ConversationState.EXPECTING_ANNOUNCEMENT_CONTENT -> {
                conversationData["content"] = reply
                askForNextAnnouncementDetail()
            }
            ConversationState.EXPECTING_ANNOUNCEMENT_AUDIENCE -> {
                // Audience values are fixed (ALL, PARENTS, TEACHERS)
                val validAudiences = listOf("all", "parents", "teachers")
                if (validAudiences.contains(lowerCaseReply)) {
                    conversationData["audience"] = reply
                    askForNextAnnouncementDetail()
                } else {
                    addMessageToChat("AI: Invalid audience. Please specify ALL, PARENTS, or TEACHERS.")
                    // Keep the state the same to re-ask
                }
            }
            // Conversation states for Exam
            ConversationState.EXPECTING_EXAM_NAME -> {
                conversationData["name"] = reply
                askForNextExamDetail()
            }
            ConversationState.EXPECTING_EXAM_SUBJECT -> {
                validateAndStoreEntity(reply, "subject", AiAssistantConstants.EXTRA_EXAM_SUBJECT) { name -> listener?.getSubjectByName(name) }
            }
            ConversationState.EXPECTING_EXAM_BATCH -> {
                if (lowerCaseReply == "none") { // User explicitly says no batch
                    conversationData["batch"] = "" // Store empty string
                    askForNextExamDetail()
                } else {
                    validateAndStoreEntity(reply, "batch", AiAssistantConstants.EXTRA_EXAM_BATCH) { name -> listener?.getBatchByName(name) }
                }
            }
            ConversationState.EXPECTING_EXAM_DATE -> {
                conversationData["date"] = reply // Date format not validated here, only in target activity
                askForNextExamDetail()
            }
            ConversationState.EXPECTING_EXAM_MAX_MARKS -> {
                conversationData["maxmarks"] = reply // Max marks format not validated here, only in target activity
                askForNextExamDetail()
            }

            ConversationState.IDLE -> {
                // If not in a conversation, try to recognize initial command
                when {
                    lowerCaseReply.startsWith("add class session") || lowerCaseReply.startsWith("create class session") -> {
                        parseClassSessionCommand(reply)
                    }
                    lowerCaseReply.startsWith("give announcement") || lowerCaseReply.startsWith("post announcement") -> {
                        parseAnnouncementCommand(reply)
                    }
                    lowerCaseReply.startsWith("add exam") || lowerCaseReply.startsWith("create exam") -> {
                        parseExamCommand(reply)
                    }
                    else -> {
                        listener?.onCommandSent(reply)
                        dismiss()
                    }
                }
            }
        }
    }

    // NEW: Generic validation and storage for entities
    private fun validateAndStoreEntity(
        reply: String,
        keyInMap: String,
        extraConstant: String, // Not directly used here, but for consistency if we wanted to pass IDs to commandSent
        getEntityByName: suspend (String) -> Any?
    ) = CoroutineScope(Dispatchers.Main).launch { // Launch on Main to update UI
        val entity = withContext(Dispatchers.IO) { // Perform DB lookup on IO thread
            getEntityByName(reply)
        }

        if (entity != null) {
            val id: String = when (entity) { // Ensure 'id' is inferred as non-nullable String
                is Subject -> entity.id
                is Batch -> entity.id
                is User -> entity.id
                else -> {
                    Log.e(TAG, "Unexpected entity type for ID extraction: $entity")
                    addMessageToChat("AI: Internal error processing response. Please retry.")
                    return@launch // Return from the coroutine
                }
            }

            if (id.isNotBlank()) { // Ensure the ID is not blank before using it
                conversationData[keyInMap] = when (entity) {
                    is Subject -> entity.subjectName
                    is Batch -> entity.batchName
                    is User -> entity.fullName ?: entity.username ?: "" // Ensure fullName or username is non-nullable String
                    else -> reply
                } // Store the full name string
                conversationData["${keyInMap}_id"] = id // Store ID separately for internal use
                when (currentState) {
                    ConversationState.EXPECTING_CLASS_SESSION_SUBJECT,
                    ConversationState.EXPECTING_CLASS_SESSION_BATCH,
                    ConversationState.EXPECTING_CLASS_SESSION_TEACHER -> askForNextClassSessionDetail()
                    ConversationState.EXPECTING_EXAM_SUBJECT,
                    ConversationState.EXPECTING_EXAM_BATCH -> askForNextExamDetail()
                    else -> {} // Should not happen for other states
                }
            } else {
                addMessageToChat("AI: Error: Could not get ID for '$reply'. Please try again.")
                // Keep the state to re-ask
            }
        } else {
            addMessageToChat("AI: Sorry, I don't recognize that $keyInMap: '$reply'. Please try again or provide a valid one.")
            // Keep the state to re-ask
        }
    }


    // --- Class Session Conversational Logic ---
    private fun parseClassSessionCommand(command: String) {
        val paramsString = command.substringAfter(":", "").trim()
        val parsedParams = parseParams(paramsString)

        conversationData.clear()
        parsedParams.forEach { (key, value) ->
            conversationData[key] = value
        }

        // Now, validate and store IDs for known entities
        CoroutineScope(Dispatchers.Main).launch {
            var allEntitiesRecognized = true

            val subjectName = conversationData["subject"]
            if (!subjectName.isNullOrBlank()) {
                val subject = withContext(Dispatchers.IO) { listener?.getSubjectByName(subjectName) }
                if (subject != null) {
                    conversationData["subject"] = subject.subjectName // Store official name
                    conversationData["subject_id"] = subject.id.toString()
                } else {
                    allEntitiesRecognized = false
                    addMessageToChat("AI: I don't recognize the subject '$subjectName'. Please provide a valid subject name for the class session.")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_SUBJECT
                }
            }

            val batchName = conversationData["batch"]
            if (allEntitiesRecognized && !batchName.isNullOrBlank()) {
                val batch = withContext(Dispatchers.IO) { listener?.getBatchByName(batchName) }
                if (batch != null) {
                    conversationData["batch"] = batch.batchName // Store official name
                    conversationData["batch_id"] = batch.id.toString()
                } else {
                    allEntitiesRecognized = false
                    addMessageToChat("AI: I don't recognize the batch '$batchName'. Please provide a valid batch name for the class session.")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_BATCH
                }
            }

            val teacherName = conversationData["teacher"]
            if (allEntitiesRecognized && !teacherName.isNullOrBlank()) {
                val teacher = withContext(Dispatchers.IO) { listener?.getTeacherByName(teacherName) }
                if (teacher != null) {
                    conversationData["teacher"] = teacher.fullName ?: teacher.username ?: "" // Line 391: Ensure the result is non-nullable String
                    conversationData["teacher_id"] = teacher.id.toString()
                } else {
                    allEntitiesRecognized = false
                    addMessageToChat("AI: I don't recognize the teacher '$teacherName'. Please provide a valid teacher name for the class session.")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_TEACHER
                }
            }


            if (allEntitiesRecognized && checkAllRequiredFields("class_session")) {
                val fullCommand = buildClassSessionCommand(conversationData)
                listener?.onCommandSent(fullCommand)
                addMessageToChat("AI: Got it! Processing your request to add a class session. Please confirm details in the next screen.")
                currentState = ConversationState.IDLE
                conversationData.clear()
                dismiss()
            } else if (allEntitiesRecognized) { // Means some non-entity required fields are missing
                addMessageToChat("AI: Sure! Let's set up a class session.")
                // Ask for the first missing required field
                askForNextClassSessionDetail()
            }
            // If allEntitiesRecognized is false, error message already added, state set.
        }
    }


    private fun askForNextClassSessionDetail() {
        val requiredFields = listOf("subject", "batch", "day", "start", "end", "teacher")
        val missingFields = requiredFields.filter { !conversationData.containsKey(it) || conversationData[it].isNullOrBlank() }

        if (missingFields.isEmpty()) {
            val fullCommand = buildClassSessionCommand(conversationData)
            listener?.onCommandSent(fullCommand)
            addMessageToChat("AI: All details collected! Processing your request to add a class session. Please confirm details in the next screen.")
            currentState = ConversationState.IDLE
            conversationData.clear()
            dismiss()
        } else {
            when (missingFields.first()) {
                "subject" -> {
                    addMessageToChat("AI: What subject will be taught?")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_SUBJECT
                }
                "batch" -> {
                    addMessageToChat("AI: Which batch is this class for?")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_BATCH
                }
                "day" -> {
                    addMessageToChat("AI: What day of the week is this class? (e.g., Monday)")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_DAY
                }
                "start" -> {
                    addMessageToChat("AI: What time does it start? (e.g., 09:00)")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_START_TIME
                }
                "end" -> {
                    addMessageToChat("AI: What time does it end? (e.g., 10:00)")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_END_TIME
                }
                "teacher" -> {
                    addMessageToChat("AI: Who is the teacher for this class?")
                    currentState = ConversationState.EXPECTING_CLASS_SESSION_TEACHER
                }
            }
        }
    }

    // --- Announcement Conversational Logic ---
    private fun parseAnnouncementCommand(command: String) {
        val paramsString = command.substringAfter(":", "").trim()
        val parsedParams = parseParams(paramsString)

        conversationData.clear()
        parsedParams.forEach { (key, value) ->
            conversationData[key] = value
        }

        val requiredFields = listOf("title", "content", "audience")
        val missingFields = requiredFields.filter { !conversationData.containsKey(it) || conversationData[it].isNullOrBlank() }

        if (missingFields.isEmpty()) {
            // Validate audience value directly here as it's a fixed set
            val audienceInput = conversationData["audience"]?.uppercase()
            val validAudiences = listOf("ALL", "PARENTS", "TEACHERS")
            if (audienceInput != null && validAudiences.contains(audienceInput)) {
                conversationData["audience"] = audienceInput // Store canonical uppercase
                val fullCommand = buildAnnouncementCommand(conversationData)
                listener?.onCommandSent(fullCommand)
                addMessageToChat("AI: Got it! Preparing your announcement. Please confirm details in the next screen.")
                currentState = ConversationState.IDLE
                conversationData.clear()
                dismiss()
            } else {
                addMessageToChat("AI: Invalid audience: '$audienceInput'. Please specify ALL, PARENTS, or TEACHERS.")
                currentState = ConversationState.EXPECTING_ANNOUNCEMENT_AUDIENCE // Re-ask for audience
            }
        } else {
            addMessageToChat("AI: Okay, let's create an announcement.")
            // Set state for the first missing required field
            when (missingFields.first()) {
                "title" -> currentState = ConversationState.EXPECTING_ANNOUNCEMENT_TITLE
                "content" -> currentState = ConversationState.EXPECTING_ANNOUNCEMENT_CONTENT
                "audience" -> currentState = ConversationState.EXPECTING_ANNOUNCEMENT_AUDIENCE
            }
            askForNextAnnouncementDetail() // This will then ask the question based on the set state
        }
    }

    private fun askForNextAnnouncementDetail() {
        val requiredFields = listOf("title", "content", "audience")
        val missingFields = requiredFields.filter { !conversationData.containsKey(it) || conversationData[it].isNullOrBlank() }

        if (missingFields.isEmpty()) {
            // Final validation for audience after multi-turn
            val audienceInput = conversationData["audience"]?.uppercase()
            val validAudiences = listOf("ALL", "PARENTS", "TEACHERS")
            if (audienceInput != null && validAudiences.contains(audienceInput)) {
                conversationData["audience"] = audienceInput
                val fullCommand = buildAnnouncementCommand(conversationData)
                listener?.onCommandSent(fullCommand)
                addMessageToChat("AI: All details collected! Your announcement is ready to be published. Please confirm details in the next screen.")
                currentState = ConversationState.IDLE
                conversationData.clear()
                dismiss()
            } else {
                addMessageToChat("AI: Invalid audience: '${conversationData["audience"]}'. Please specify ALL, PARENTS, or TEACHERS.")
                currentState = ConversationState.EXPECTING_ANNOUNCEMENT_AUDIENCE // Keep state the same to re-ask
            }
        } else {
            when (missingFields.first()) {
                "title" -> {
                    addMessageToChat("AI: What is the title of the announcement?")
                    currentState = ConversationState.EXPECTING_ANNOUNCEMENT_TITLE
                }
                "content" -> {
                    addMessageToChat("AI: What is the content of the announcement?")
                    currentState = ConversationState.EXPECTING_ANNOUNCEMENT_CONTENT
                }
                "audience" -> {
                    addMessageToChat("AI: Who is the target audience? (ALL, PARENTS, TEACHERS)")
                    currentState = ConversationState.EXPECTING_ANNOUNCEMENT_AUDIENCE
                }
            }
        }
    }

    // --- Exam Conversational Logic ---
    private fun parseExamCommand(command: String) {
        val paramsString = command.substringAfter(":", "").trim()
        val parsedParams = parseParams(paramsString)

        conversationData.clear()
        parsedParams.forEach { (key, value) ->
            conversationData[key] = value
        }

        // Validate entities first
        CoroutineScope(Dispatchers.Main).launch {
            var allEntitiesRecognized = true

            val subjectName = conversationData["subject"]
            if (!subjectName.isNullOrBlank()) {
                val subject = withContext(Dispatchers.IO) { listener?.getSubjectByName(subjectName) }
                if (subject != null) {
                    conversationData["subject"] = subject.subjectName
                    conversationData["subject_id"] = subject.id.toString()
                } else {
                    allEntitiesRecognized = false
                    addMessageToChat("AI: I don't recognize the subject '$subjectName'. Please provide a valid subject name for the exam.")
                    currentState = ConversationState.EXPECTING_EXAM_SUBJECT
                }
            }

            val batchName = conversationData["batch"]
            if (allEntitiesRecognized && !batchName.isNullOrBlank()) {
                if (batchName.lowercase() == "none") {
                    conversationData["batch"] = "" // Store empty string if explicitly "none"
                } else {
                    val batch = withContext(Dispatchers.IO) { listener?.getBatchByName(batchName!!) } // Applied non-null assertion here
                    if (batch != null) {
                        conversationData["batch"] = batch.batchName
                        conversationData["batch_id"] = batch.id.toString()
                    } else {
                        allEntitiesRecognized = false
                        addMessageToChat("AI: I don't recognize the batch '$batchName'. Please provide a valid batch name for the exam, or say 'none'.")
                        currentState = ConversationState.EXPECTING_EXAM_BATCH
                    }
                }
            }

            if (allEntitiesRecognized && checkAllRequiredFields("exam")) {
                val fullCommand = buildExamCommand(conversationData)
                listener?.onCommandSent(fullCommand)
                addMessageToChat("AI: Got it! Preparing to add the exam. Please confirm details in the next screen.")
                currentState = ConversationState.IDLE
                conversationData.clear()
                dismiss()
            } else if (allEntitiesRecognized) {
                addMessageToChat("AI: Okay, let's create a new exam.")
                askForNextExamDetail()
            }
        }
    }

    private fun askForNextExamDetail() {
        val requiredFields = listOf("name", "subject", "date", "maxmarks")
        val missingRequiredFields = requiredFields.filter { !conversationData.containsKey(it) || conversationData[it].isNullOrBlank() }

        if (missingRequiredFields.isNotEmpty()) {
            when (missingRequiredFields.first()) {
                "name" -> {
                    addMessageToChat("AI: What is the name of the exam?")
                    currentState = ConversationState.EXPECTING_EXAM_NAME
                }
                "subject" -> {
                    addMessageToChat("AI: Which subject is this exam for?")
                    currentState = ConversationState.EXPECTING_EXAM_SUBJECT
                }
                "date" -> {
                    addMessageToChat("AI: What is the exam date? (YYYY-MM-DD)")
                    currentState = ConversationState.EXPECTING_EXAM_DATE
                }
                "maxmarks" -> {
                    addMessageToChat("AI: What are the maximum marks for this exam?")
                    currentState = ConversationState.EXPECTING_EXAM_MAX_MARKS
                }
            }
            return
        }

        // All required fields are present. Now, if batch is missing, ask for it as an optional step.
        if (conversationData["batch"].isNullOrBlank() && currentState != ConversationState.EXPECTING_EXAM_BATCH) {
            currentState = ConversationState.EXPECTING_EXAM_BATCH
            addMessageToChat("AI: Is there a specific batch for this exam? (Optional, e.g., Grade 10 Warriors) Type 'none' if no batch.")
            return
        }

        val fullCommand = buildExamCommand(conversationData)
        listener?.onCommandSent(fullCommand)
        addMessageToChat("AI: All details collected! Your exam is ready to be added. Please confirm details in the next screen.")
        currentState = ConversationState.IDLE
        conversationData.clear()
        dismiss()
    }

    // Helper to check all required fields based on command type
    private fun checkAllRequiredFields(commandType: String): Boolean {
        return when (commandType) {
            "class_session" -> {
                val fields = listOf("subject", "batch", "day", "start", "end", "teacher")
                fields.all { conversationData.containsKey(it) && !conversationData[it].isNullOrBlank() } &&
                        !conversationData["subject_id"].isNullOrBlank() &&
                        !conversationData["batch_id"].isNullOrBlank() &&
                        !conversationData["teacher_id"].isNullOrBlank()
            }
            "announcement" -> {
                val fields = listOf("title", "content", "audience")
                fields.all { conversationData.containsKey(it) && !conversationData[it].isNullOrBlank() }
            }
            "exam" -> {
                val fields = listOf("name", "subject", "date", "maxmarks")
                fields.all { conversationData.containsKey(it) && !conversationData[it].isNullOrBlank() } &&
                        !conversationData["subject_id"].isNullOrBlank()
                // Batch ID is optional, not checked here as it can be blank
            }
            else -> false
        }
    }


    // Helper function to parse key-value pairs from a string.
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

    // Helper function to build the structured command string for Class Session
    private fun buildClassSessionCommand(data: Map<String, String>): String {
        return "add class session: " +
                "Subject:${data["subject"] ?: ""}, " +
                "Batch:${data["batch"] ?: ""}, " +
                "Day:${data["day"] ?: ""}, " +
                "Start:${data["start"] ?: ""}, " +
                "End:${data["end"] ?: ""}, " +
                "Teacher:${data["teacher"] ?: ""}"
    }

    // Helper function to build the structured command string for Announcement
    private fun buildAnnouncementCommand(data: Map<String, String>): String {
        return "give announcement: " +
                "Title:${data["title"] ?: ""}, " +
                "Content:${data["content"] ?: ""}, " +
                "Audience:${data["audience"] ?: ""}"
    }

    // Helper function to build the structured command string for Exam
    private fun buildExamCommand(data: Map<String, String>): String {
        val batchValue = if (data["batch"]?.lowercase() == "none") "" else (data["batch"] ?: "")
        return "add exam: " +
                "Name:${data["name"] ?: ""}, " +
                "Subject:${data["subject"] ?: ""}, " +
                "Batch:$batchValue, " +
                "Date:${data["date"] ?: ""}, " +
                "MaxMarks:${data["maxmarks"] ?: ""}"
    }
}