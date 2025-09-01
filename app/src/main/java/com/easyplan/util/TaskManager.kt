package com.easyplan.util

import com.easyplan.data.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskManager {
    private val tasks = mutableListOf<Task>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val db by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    fun addTask(task: Task) {
        tasks.add(task)
        // Save to Firestore under current user
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .collection("tasks").document(task.id)
                .set(task)
        }
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
            auth.currentUser?.uid?.let { uid ->
                db.collection("users").document(uid)
                    .collection("tasks").document(updatedTask.id)
                    .set(updatedTask)
            }
        }
    }

    fun deleteTask(taskId: String) {
        tasks.removeAll { it.id == taskId }
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .collection("tasks").document(taskId)
                .delete()
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        val task = tasks.find { it.id == taskId }
        task?.let {
            val updated = it.copy(isCompleted = !it.isCompleted)
            updateTask(updated)
        }
    }

    // Load tasks for current user once; caller should refresh UI
    fun loadTasksForUser(onComplete: (() -> Unit)? = null) {
        tasks.clear()
        val uid = auth.currentUser?.uid ?: return onComplete?.invoke() ?: Unit
        db.collection("users").document(uid).collection("tasks")
            .get()
            .addOnSuccessListener { snap ->
                for (doc in snap.documents) {
                    doc.toObject(Task::class.java)?.let { tasks.add(it) }
                }
                onComplete?.invoke()
            }
            .addOnFailureListener {
                onComplete?.invoke()
            }
    }

    // Initialize with sample tasks only when no auth; otherwise Firestore will populate
    fun initializeSampleTasks() {
        if (auth.currentUser == null && tasks.isEmpty()) {
            val today = Date()
            val calendar = Calendar.getInstance()
            addTask(Task(
                title = "Review project proposal",
                description = "Go through the quarterly project proposal and provide feedback",
                dueDate = today,
                dueTime = "14:00"
            ))
            addTask(Task(
                title = "Team meeting preparation",
                description = "Prepare slides and agenda for tomorrow's team meeting",
                dueDate = today,
                dueTime = "16:30"
            ))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            addTask(Task(
                title = "Client presentation",
                description = "Present the new design concepts to the client",
                dueDate = calendar.time,
                dueTime = "10:00"
            ))
        }
    }
}
