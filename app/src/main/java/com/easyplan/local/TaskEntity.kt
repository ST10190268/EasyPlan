package com.easyplan.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.easyplan.data.Task
import java.util.Date

/**
 * TaskEntity - Room representation of [Task] persisted for offline use.
 *
 * References:
 * Room documentation: https://developer.android.com/training/data-storage/room
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dueDateMillis: Long?,
    val dueTime: String?,
    val isCompleted: Boolean,
    val priority: String,
    val category: String,
    val color: String,
    val createdAtMillis: Long,
    val completedAtMillis: Long?,
    val needsSync: Boolean
) {
    fun toTask(): Task = Task(
        id = id,
        title = title,
        description = description,
        dueDate = dueDateMillis?.let { Date(it) },
        dueTime = dueTime,
        isCompleted = isCompleted,
        priority = priority,
        category = category,
        color = color,
        createdAt = Date(createdAtMillis),
        completedAt = completedAtMillis?.let { Date(it) }
    )

    companion object {
        fun fromTask(task: Task, needsSync: Boolean = false): TaskEntity =
            TaskEntity(
                id = task.id,
                title = task.title,
                description = task.description,
                dueDateMillis = task.dueDate?.time,
                dueTime = task.dueTime,
                isCompleted = task.isCompleted,
                priority = task.priority,
                category = task.category,
                color = task.color,
                createdAtMillis = task.createdAt.time,
                completedAtMillis = task.completedAt?.time,
                needsSync = needsSync
            )
    }
}
