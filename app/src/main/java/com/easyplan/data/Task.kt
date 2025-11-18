package com.easyplan.data

import androidx.annotation.Keep
import java.util.Date
import java.util.UUID

/**
 * Task - Data model representing a task/reminder in the EasyPlan app
 *
 * This data class represents a single task with all its properties.
 * The @Keep annotation ensures ProGuard doesn't remove this class during minification,
 * which is essential for Firebase Firestore serialization/deserialization.
 *
 * @property id Unique identifier for the task (auto-generated UUID)
 * @property title Task title/name (required)
 * @property description Detailed description of the task (optional)
 * @property dueDate Date when the task is due (optional)
 * @property dueTime Time when the task is due in HH:mm format (optional)
 * @property isCompleted Whether the task has been completed
 * @property priority Priority level: "high", "medium", or "low"
 * @property category Task category for organization
 * @property color Custom color for the task (hex format)
 * @property createdAt Timestamp when the task was created
 * @property completedAt Timestamp when the task was completed
 *
 * @author EasyPlan Team
 * @version 2.0 - Added priority, category, color, and completedAt for enhanced features
 *
 * References:
 * - Firestore Data Model: https://firebase.google.com/docs/firestore/manage-data/add-data
 * - Kotlin Data Classes: https://kotlinlang.org/docs/data-classes.html
 */
@Keep
data class Task(
    var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var description: String = "",
    var dueDate: Date? = null,
    var dueTime: String? = null,
    var isCompleted: Boolean = false,
    var priority: String = "medium", // "high", "medium", "low"
    var category: String = "personal", // "work", "personal", "study", "health", "shopping", "other"
    var color: String = "#2196F3", // Default Material Blue
    var createdAt: Date = Date(),
    var completedAt: Date? = null // Timestamp when task was completed
) {
    /**
     * No-argument constructor required by Firebase Firestore
     * for automatic deserialization of documents into Task objects.
     *
     * Reference: https://firebase.google.com/docs/firestore/query-data/get-data#custom_objects
     */
    constructor() : this(
        UUID.randomUUID().toString(),
        "",
        "",
        null,
        null,
        false,
        "medium",
        "personal",
        "#2196F3",
        Date(),
        null
    )

    /**
     * Priority levels enum for type-safe priority handling
     */
    enum class Priority(val value: String, val displayName: String, val colorHex: String) {
        HIGH("high", "High Priority", "#F44336"), // Red
        MEDIUM("medium", "Medium Priority", "#FF9800"), // Orange
        LOW("low", "Low Priority", "#4CAF50"); // Green

        companion object {
            fun fromString(value: String): Priority {
                return values().find { it.value == value } ?: MEDIUM
            }
        }
    }

    /**
     * Task categories enum with icons and colors
     */
    enum class Category(
        val value: String,
        val displayName: String,
        val icon: String,
        val colorHex: String
    ) {
        WORK("work", "Work", "💼", "#2196F3"), // Blue
        PERSONAL("personal", "Personal", "🏠", "#9C27B0"), // Purple
        STUDY("study", "Study", "📚", "#FF9800"), // Orange
        HEALTH("health", "Health", "💪", "#4CAF50"), // Green
        SHOPPING("shopping", "Shopping", "🛒", "#E91E63"), // Pink
        OTHER("other", "Other", "📌", "#607D8B"); // Blue Grey

        companion object {
            fun fromString(value: String): Category {
                return values().find { it.value == value } ?: PERSONAL
            }
        }
    }

    /**
     * Helper method to get priority enum
     */
    fun getPriorityEnum(): Priority = Priority.fromString(priority)

    /**
     * Helper method to get category enum
     */
    fun getCategoryEnum(): Category = Category.fromString(category)

    /**
     * Mark task as completed with timestamp
     */
    fun markCompleted() {
        isCompleted = true
        completedAt = Date()
    }

    /**
     * Mark task as incomplete
     */
    fun markIncomplete() {
        isCompleted = false
        completedAt = null
    }
}
