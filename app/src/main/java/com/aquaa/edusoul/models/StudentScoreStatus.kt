package com.aquaa.edusoul.models

import java.util.Locale

/*
 * StudentScoreStatus: A helper model data class used specifically for the
 * score entry screen. It holds a Student object and their current score
 * as entered by the teacher in the UI, making state management in the adapter easier.
 * Converted from Java POJO to Kotlin data class.
 */
data class StudentScoreStatus(
    val student: Student,
    var score: String = "", // Current score as a string from EditText
    var initialScore: String = "" // Original score when loaded, as a string
) {
    init {
        // Initialize initialScore from score if provided in constructor, or keep default
        if (score.isNotBlank()) {
            initialScore = score
        }
    }

    // Secondary constructor to handle double score input from DB
    constructor(student: Student, scoreValue: Double) : this(student) {
        // Use a clean way to handle double-to-string conversion, avoiding ".0" for whole numbers
        if (scoreValue >= 0) {
            this.score = if (scoreValue == scoreValue.toLong().toDouble()) {
                String.format(Locale.US, "%d", scoreValue.toLong())
            } else {
                String.format(Locale.US, "%.2f", scoreValue)
            }
            this.initialScore = this.score
        }
    }
}