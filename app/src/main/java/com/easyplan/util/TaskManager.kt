package com.easyplan.util

import com.easyplan.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskManager {
    private val tasks = mutableListOf<Task>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun addTask(task: Task) {
        tasks.add(task)
    }

    fun getAllTasks(): List<Task> = tasks.toList()

    fun getTasksForDate(date: Date): List<Task> {
        return tasks.filter { task ->
            task.dueDate?.let { dueDate ->
                dateFormat.format(dueDate) == dateFormat.format(date)
            } ?: false
        }
    }

    fun getTodayTasks(): List<Task> {
        return getTasksForDate(Date())
    }

    fun updateTask(updatedTask: Task) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            tasks[index] = updatedTask
        }
    }

    fun deleteTask(taskId: String) {
        tasks.removeAll { it.id == taskId }
    }

    fun toggleTaskCompletion(taskId: String) {
        val task = tasks.find { it.id == taskId }
        task?.let {
            updateTask(it.copy(isCompleted = !it.isCompleted))
        }
    }

    // Initialize with some sample tasks
    fun initializeSampleTasks() {
        if (tasks.isEmpty()) {
            val today = Date()
            val calendar = Calendar.getInstance()
            
            // Today's tasks
            addTask(Task(
                title = "Review project proposal",
                description = "Go through the quarterly project proposal and provide feedback",
                dueDate = today,
                dueTime = "2:00 PM"
            ))
            
            addTask(Task(
                title = "Team meeting preparation",
                description = "Prepare slides and agenda for tomorrow's team meeting",
                dueDate = today,
                dueTime = "4:30 PM"
            ))
            
            // Tomorrow's task
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            addTask(Task(
                title = "Client presentation",
                description = "Present the new design concepts to the client",
                dueDate = calendar.time,
                dueTime = "10:00 AM"
            ))
        }
    }
}
