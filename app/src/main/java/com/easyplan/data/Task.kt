package com.easyplan.data

import java.util.Date
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dueDate: Date? = null,
    val dueTime: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Date = Date()
)
