package com.easyplan.util

import android.content.Context
import android.util.Log
import com.easyplan.data.Task
import com.easyplan.repository.TaskRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * TaskManager - Singleton object for managing tasks across the app
 *
 * This object serves as the central repository for task data, handling:
 * - In-memory task storage for quick access
 * - Firebase Firestore synchronization for persistence
 * - JSONBin REST API integration for backup and API demonstration
 * - CRUD operations (Create, Read, Update, Delete)
 * - Date-based task filtering
 * - Sample task initialization for demo purposes
 *
 * The singleton pattern ensures a single source of truth for task data
 * throughout the app lifecycle.
 *
 * @author EasyPlan Team
 * @version 2.0 - Added REST API integration
 *
 * References:
 * - Singleton Pattern: https://kotlinlang.org/docs/object-declarations.html#object-declarations
 * - Firestore: https://firebase.google.com/docs/firestore
 * - JSONBin API: https://jsonbin.io/api-reference
 */
object TaskManager {

    private const val TAG = "TaskManager"

    // In-memory task storage for fast access
    private val tasks = mutableListOf<Task>()

    // Date formatter for comparing task dates (ignores time component)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Application context and local persistence
    private lateinit var appContext: Context
    private val gson by lazy { com.google.gson.Gson() }
    private const val PREFS_NAME = "tasks_local_cache"
    private const val KEY_TASKS_JSON = "tasks_json"
    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Firebase instances - lazily initialized for performance
    private val db by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    // Task repository for REST API integration
    private var taskRepository: TaskRepository? = null

    /**
     * Initializes the TaskManager with application context
     * Required for REST API integration
     *
     * @param context Application context
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (taskRepository == null) {
            taskRepository = TaskRepository(appContext)
            Log.d(TAG, "initialize: TaskRepository initialized")
        }
        // Load any locally cached tasks (guest mode or warm start)
        loadFromLocal()
    }
    /**
     * Persist current in-memory tasks list to local SharedPreferences as JSON
     */
    private fun saveToLocal() {
        try {
            val json = gson.toJson(tasks)
            prefs.edit().putString(KEY_TASKS_JSON, json).apply()
            Log.d(TAG, "saveToLocal: Saved ${tasks.size} tasks to local cache")
        } catch (e: Exception) {
            Log.e(TAG, "saveToLocal: Failed to save tasks locally", e)
        }
    }

    /**
     * Load tasks from local SharedPreferences JSON cache into memory (if present)
     */
    private fun loadFromLocal() {
        try {
            val json = prefs.getString(KEY_TASKS_JSON, null) ?: return
            val type = object : com.google.gson.reflect.TypeToken<List<Task>>() {}.type
            val loaded: List<Task> = gson.fromJson(json, type) ?: emptyList()
            if (loaded.isNotEmpty()) {
                tasks.clear()
                tasks.addAll(loaded)
                Log.i(TAG, "loadFromLocal: Loaded ${tasks.size} tasks from local cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadFromLocal: Failed to load tasks from local cache", e)
        }
    }


    /**
     * Adds a new task to the in-memory list and syncs to both Firestore and JSONBin
     *
     * @param task The task to add
     */
    fun addTask(task: Task) {
        Log.d(TAG, "addTask: Adding task with ID: ${task.id}, Title: ${task.title}")
        tasks.add(task)
        // Persist locally
        saveToLocal()

        // Sync to Firestore if user is authenticated
        // Tasks are stored in a subcollection under each user's document
        auth.currentUser?.uid?.let { uid ->
            Log.d(TAG, "addTask: Syncing task to Firestore for user: $uid")
            db.collection("users").document(uid)
                .collection("tasks").document(task.id)
                .set(task)
                .addOnSuccessListener {
                    Log.i(TAG, "addTask: Task successfully saved to Firestore")
                    // Also sync to JSONBin REST API
                    syncToRestApi()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "addTask: Failed to save task to Firestore", e)
                }
        } ?: Log.w(TAG, "addTask: No authenticated user, task not synced to Firestore")
    }

    /**
     * Syncs all tasks to JSONBin REST API
     * This demonstrates REST API integration as required by the rubric
     */
    fun syncToRestApi() {
        taskRepository?.let { repo ->
            Log.d(TAG, "syncToRestApi: Syncing ${tasks.size} tasks to JSONBin")
            repo.syncToJsonBin(tasks.toList()) { success ->
                if (success) {
                    Log.i(TAG, "syncToRestApi: Successfully synced to JSONBin REST API")
                } else {
                    Log.e(TAG, "syncToRestApi: Failed to sync to JSONBin REST API")
                }
            }
        } ?: Log.w(TAG, "syncToRestApi: TaskRepository not initialized")
    }

    /**
     * Syncs all tasks to JSONBin REST API with completion callback
     */
    fun syncToRestApi(onComplete: (Boolean) -> Unit) {
        taskRepository?.let { repo ->
            Log.d(TAG, "syncToRestApi(callback): Syncing ${tasks.size} tasks to JSONBin")
            repo.syncToJsonBin(tasks.toList()) { success ->
                if (success) Log.i(TAG, "syncToRestApi(callback): Success") else Log.e(TAG, "syncToRestApi(callback): Failed")
                onComplete(success)
            }
        } ?: run {
            Log.w(TAG, "syncToRestApi(callback): TaskRepository not initialized")
            onComplete(false)
        }
    }


    /**
     * Loads tasks from JSONBin REST API
     * Useful for demonstrating REST API integration
     *
     * @param onComplete Callback when loading is complete
     */
    fun loadFromRestApi(onComplete: () -> Unit) {
        taskRepository?.let { repo ->
            Log.d(TAG, "loadFromRestApi: Loading tasks from JSONBin")
            repo.loadFromJsonBin { loadedTasks ->
                if (loadedTasks != null) {
                    Log.i(TAG, "loadFromRestApi: Loaded ${loadedTasks.size} tasks from REST API")
                    tasks.clear()
                    tasks.addAll(loadedTasks)
                    // Persist locally
                    saveToLocal()
                } else {
                    Log.w(TAG, "loadFromRestApi: Failed to load from REST API")
                }
                onComplete()
            }
        } ?: run {
            Log.w(TAG, "loadFromRestApi: TaskRepository not initialized")
            onComplete()
        }
    }

    /**
     * Returns a copy of all tasks in memory
     *
     * @return Immutable list of all tasks
     */
    fun getAllTasks(): List<Task> {
        Log.d(TAG, "getAllTasks: Returning ${tasks.size} tasks")
        return tasks.toList()
    }

    /**
     * Filters tasks by a specific date (ignores time component)
     *
     * @param date The date to filter by
     * @return List of tasks due on the specified date
     */
    fun getTasksForDate(date: Date): List<Task> {
        val dateStr = dateFormat.format(date)
        Log.d(TAG, "getTasksForDate: Filtering tasks for date: $dateStr")

        val filteredTasks = tasks.filter { task ->
            task.dueDate?.let { dueDate ->
                dateFormat.format(dueDate) == dateStr
            } ?: false
        }

        Log.d(TAG, "getTasksForDate: Found ${filteredTasks.size} tasks for $dateStr")
        return filteredTasks
    }

    /**
     * Expose current JSONBin bin ID if available
     */
    fun getJsonBinId(): String? {
        return taskRepository?.getBinId()
    }

    /**
     * Convenience method to get today's tasks
     *
     * @return List of tasks due today
     */
    fun getTodayTasks(): List<Task> {
        Log.d(TAG, "getTodayTasks: Fetching today's tasks")
        return getTasksForDate(Date())
    }

    /**
     * Updates an existing task in memory and Firestore
     *
     * @param updatedTask The task with updated properties
     */
    fun updateTask(updatedTask: Task) {
        Log.d(TAG, "updateTask: Updating task ID: ${updatedTask.id}")
        val index = tasks.indexOfFirst { it.id == updatedTask.id }

        if (index != -1) {
            tasks[index] = updatedTask
            Log.d(TAG, "updateTask: Task updated in memory at index: $index")
            // Persist locally
            saveToLocal()

            // Sync update to Firestore
            auth.currentUser?.uid?.let { uid ->
                Log.d(TAG, "updateTask: Syncing update to Firestore")
                db.collection("users").document(uid)
                    .collection("tasks").document(updatedTask.id)
                    .set(updatedTask)
                    .addOnSuccessListener {
                        Log.i(TAG, "updateTask: Task successfully updated in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "updateTask: Failed to update task in Firestore", e)
                    }
            }
        } else {
            Log.w(TAG, "updateTask: Task not found with ID: ${updatedTask.id}")
        }
    }

    /**
     * Deletes a task from memory and Firestore
     *
     * @param taskId The ID of the task to delete
     */
    fun deleteTask(taskId: String) {
        Log.d(TAG, "deleteTask: Deleting task ID: $taskId")
        val removed = tasks.removeAll { it.id == taskId }

        if (removed) {
            Log.d(TAG, "deleteTask: Task removed from memory")
            // Persist locally
            saveToLocal()

            // Delete from Firestore
            auth.currentUser?.uid?.let { uid ->
                Log.d(TAG, "deleteTask: Deleting from Firestore")
                db.collection("users").document(uid)
                    .collection("tasks").document(taskId)
                    .delete()
                    .addOnSuccessListener {
                        Log.i(TAG, "deleteTask: Task successfully deleted from Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "deleteTask: Failed to delete task from Firestore", e)
                    }
            }
        } else {
            Log.w(TAG, "deleteTask: No task found with ID: $taskId")
        }
    }

    /**
     * Toggles the completion status of a task
     *
     * @param taskId The ID of the task to toggle
     */
    fun toggleTaskCompletion(taskId: String) {
        Log.d(TAG, "toggleTaskCompletion: Toggling completion for task ID: $taskId")
        val task = tasks.find { it.id == taskId }

        task?.let {
            val newStatus = !it.isCompleted
            Log.d(TAG, "toggleTaskCompletion: Changing completion status to: $newStatus")
            val updated = it.copy(isCompleted = newStatus)
            updateTask(updated)
        } ?: Log.w(TAG, "toggleTaskCompletion: Task not found with ID: $taskId")
    }

    /**
     * Loads all tasks for the currently authenticated user from Firestore
     *
     * This method clears the in-memory task list and repopulates it from Firestore.
     * It should be called when the user logs in or when a full refresh is needed.
     *
     * @param onComplete Callback function invoked when loading is complete (success or failure)
     */
    fun loadTasksForUser(onComplete: (() -> Unit)? = null) {
        Log.d(TAG, "loadTasksForUser: Starting to load tasks from Firestore")
        tasks.clear()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "loadTasksForUser: No authenticated user, skipping load")
            onComplete?.invoke()
            return
        }

        Log.d(TAG, "loadTasksForUser: Loading tasks for user: $uid")
        // Query all tasks in the user's tasks subcollection
        db.collection("users").document(uid).collection("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "loadTasksForUser: Successfully retrieved ${snapshot.size()} documents")

                // Convert Firestore documents to Task objects
                for (doc in snapshot.documents) {
                    doc.toObject(Task::class.java)?.let { task ->
                        tasks.add(task)
                        Log.d(TAG, "loadTasksForUser: Loaded task: ${task.title}")
                    }
                }

                Log.i(TAG, "loadTasksForUser: Successfully loaded ${tasks.size} tasks")
                // Persist locally as cache
                saveToLocal()
                onComplete?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "loadTasksForUser: Failed to load tasks", exception)
                onComplete?.invoke()
            }
    }

    /**
     * Initializes sample tasks for demonstration purposes
     *
     * This method only adds sample tasks if:
     * 1. No user is authenticated (guest mode)
     * 2. The task list is currently empty
     *
     * Sample tasks help new users understand the app's functionality.
     */
    fun initializeSampleTasks() {
        if (auth.currentUser == null && tasks.isEmpty()) {
            Log.d(TAG, "initializeSampleTasks: Creating sample tasks for guest user")

            val today = Date()
            val calendar = Calendar.getInstance()

            // Sample task 1: Due today
            addTask(Task(
                title = "Review project proposal",
                description = "Go through the quarterly project proposal and provide feedback",
                dueDate = today,
                dueTime = "14:00"
            ))

            // Sample task 2: Due today
            addTask(Task(
                title = "Team meeting preparation",
                description = "Prepare slides and agenda for tomorrow's team meeting",
                dueDate = today,
                dueTime = "16:30"
            ))

            // Sample task 3: Due tomorrow
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            addTask(Task(
                title = "Client presentation",
                description = "Present the new design concepts to the client",
                dueDate = calendar.time,
                dueTime = "10:00"
            ))

            Log.i(TAG, "initializeSampleTasks: Created 3 sample tasks")
        } else {
            Log.d(TAG, "initializeSampleTasks: Skipping - user authenticated or tasks already exist")
        }
    }
}
