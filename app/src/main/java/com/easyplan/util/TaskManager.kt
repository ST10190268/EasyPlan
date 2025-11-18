package com.easyplan.util

import android.content.Context
import android.util.Log
import com.easyplan.data.Task
import com.easyplan.local.TaskLocalDataSource
import com.easyplan.repository.TaskRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * TaskManager - In-memory + offline cache (Room) + Firebase + JSONBin sync layer.
 *
 * References:
 * - Firestore Sync: https://firebase.google.com/docs/firestore/manage-data/add-data
 * - Offline Caching with Room: https://developer.android.com/training/data-storage/room
 */
object TaskManager {

    private const val TAG = "TaskManager"

    private val tasks = mutableListOf<Task>()
    private val pendingSyncIds = mutableSetOf<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var appContext: Context
    private val db by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }
    private var taskRepository: TaskRepository? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        TaskLocalDataSource.initialize(appContext)
        if (taskRepository == null) {
            taskRepository = TaskRepository(appContext)
            Log.d(TAG, "initialize: TaskRepository ready")
        }
        loadFromLocal()
    }

    private fun loadFromLocal() {
        try {
            val entities = TaskLocalDataSource.getAllEntities()
            tasks.clear()
            tasks.addAll(entities.map { it.toTask() })
            pendingSyncIds.clear()
            pendingSyncIds.addAll(TaskLocalDataSource.getPendingSyncIds())
            Log.i(TAG, "loadFromLocal: ${tasks.size} tasks cached (pending=${pendingSyncIds.size})")
        } catch (e: Exception) {
            Log.e(TAG, "loadFromLocal: Failed to hydrate Room cache", e)
        }
    }

    private fun canSyncNow(): Boolean =
        ::appContext.isInitialized && auth.currentUser != null && NetworkUtils.isOnline(appContext)

    private fun shouldDeferSync(): Boolean =
        auth.currentUser != null && (!::appContext.isInitialized || !NetworkUtils.isOnline(appContext))

    fun getPendingSyncCount(): Int = pendingSyncIds.size

    fun hasPendingSync(): Boolean = pendingSyncIds.isNotEmpty()

    fun addTask(task: Task) {
        Log.d(TAG, "addTask: ${task.id}")
        tasks.add(task)

        val needsDeferredSync = shouldDeferSync()
        TaskLocalDataSource.upsert(task, needsDeferredSync)
        if (needsDeferredSync) pendingSyncIds.add(task.id) else pendingSyncIds.remove(task.id)

        if (canSyncNow()) {
            pushTaskToFirestore(task) {
                syncToRestApi()
            }
        } else if (auth.currentUser == null) {
            Log.d(TAG, "addTask: Guest mode task stored locally")
        } else {
            Log.w(TAG, "addTask: Offline - queued for sync")
        }
    }

    private fun pushTaskToFirestore(task: Task, onComplete: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid ?: run {
            onComplete?.invoke()
            return
        }
        db.collection("users")
            .document(uid)
            .collection("tasks")
            .document(task.id)
            .set(task)
            .addOnSuccessListener {
                Log.i(TAG, "pushTaskToFirestore: Synced ${task.id}")
                TaskLocalDataSource.upsert(task, false)
                pendingSyncIds.remove(task.id)
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "pushTaskToFirestore: Failed ${task.id}", e)
                pendingSyncIds.add(task.id)
                onComplete?.invoke()
            }
    }

    private fun pushDeleteToFirestore(taskId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users")
            .document(uid)
            .collection("tasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener {
                Log.i(TAG, "pushDeleteToFirestore: Deleted $taskId remotely")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "pushDeleteToFirestore: Failed to delete $taskId", e)
            }
    }

    fun syncToRestApi(onComplete: ((Boolean) -> Unit)? = null) {
        if (!::appContext.isInitialized || !NetworkUtils.isOnline(appContext)) {
            Log.w(TAG, "syncToRestApi: Skipped - offline")
            onComplete?.invoke(false)
            return
        }
        val repo = taskRepository ?: run {
            Log.e(TAG, "syncToRestApi: Repository missing")
            onComplete?.invoke(false)
            return
        }
        repo.syncToJsonBin(tasks.toList()) { success ->
            if (success) {
                Log.i(TAG, "syncToRestApi: Success")
            } else {
                Log.e(TAG, "syncToRestApi: Failed")
            }
            onComplete?.invoke(success)
        }
    }

    fun loadFromRestApi(onComplete: () -> Unit) {
        if (!NetworkUtils.isOnline(appContext)) {
            Log.w(TAG, "loadFromRestApi: Skipped - offline")
            onComplete()
            return
        }

        taskRepository?.let { repo ->
            repo.loadFromJsonBin { loadedTasks ->
                if (loadedTasks != null) {
                    Log.i(TAG, "loadFromRestApi: Imported ${loadedTasks.size} tasks")
                    tasks.clear()
                    tasks.addAll(loadedTasks)
                    TaskLocalDataSource.cacheTasks(tasks)
                    pendingSyncIds.clear()
                } else {
                    Log.e(TAG, "loadFromRestApi: Failed to load data")
                }
                onComplete()
            }
        } ?: run {
            Log.e(TAG, "loadFromRestApi: Repository missing")
            onComplete()
        }
    }

    fun getAllTasks(): List<Task> = tasks.toList()

    fun getTasksForDate(date: Date): List<Task> {
        val dateStr = dateFormat.format(date)
        return tasks.filter { it.dueDate?.let { due -> dateFormat.format(due) == dateStr } ?: false }
    }

    fun getJsonBinId(): String? = taskRepository?.getBinId()

    fun getTodayTasks(): List<Task> = getTasksForDate(Date())

    fun updateTask(updatedTask: Task) {
        val index = tasks.indexOfFirst { it.id == updatedTask.id }
        if (index == -1) {
            Log.w(TAG, "updateTask: Missing ${updatedTask.id}")
            return
        }
        tasks[index] = updatedTask

        val needsDeferredSync = shouldDeferSync()
        TaskLocalDataSource.upsert(updatedTask, needsDeferredSync)
        if (needsDeferredSync) pendingSyncIds.add(updatedTask.id) else pendingSyncIds.remove(updatedTask.id)

        if (canSyncNow()) {
            pushTaskToFirestore(updatedTask)
        } else {
            Log.d(TAG, "updateTask: Stored offline -> ${updatedTask.id}")
        }
    }

    fun deleteTask(taskId: String) {
        val canDelete = auth.currentUser == null || canSyncNow()
        if (!canDelete) {
            Log.w(TAG, "deleteTask: Cannot delete while offline for signed-in user")
            return
        }

        val removed = tasks.removeAll { it.id == taskId }
        if (!removed) {
            Log.w(TAG, "deleteTask: Task $taskId not found")
            return
        }
        TaskLocalDataSource.delete(taskId)
        pendingSyncIds.remove(taskId)
        if (auth.currentUser != null) {
            pushDeleteToFirestore(taskId)
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        val task = tasks.find { it.id == taskId } ?: return
        if (task.isCompleted) {
            task.markIncomplete()
        } else {
            task.markCompleted()
        }
        updateTask(task)
    }

    fun loadTasksForUser(onComplete: (() -> Unit)? = null) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "loadTasksForUser: No authenticated user, skip remote load")
            onComplete?.invoke()
            return
        }
        db.collection("users")
            .document(uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "loadTasksForUser: fetched ${snapshot.size()} docs")
                snapshot.documents.mapNotNull { it.toObject(Task::class.java) }
                    .forEach { remoteTask ->
                        TaskLocalDataSource.upsert(remoteTask, false)
                    }
                loadFromLocal()
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "loadTasksForUser: Failed to load Firestore data", e)
                onComplete?.invoke()
            }
    }

    fun syncPendingTasks(onComplete: ((Boolean) -> Unit)? = null) {
        if (!canSyncNow()) {
            Log.d(TAG, "syncPendingTasks: No connectivity/auth")
            onComplete?.invoke(false)
            return
        }
        val pending = TaskLocalDataSource.getPendingSyncTasks()
        if (pending.isEmpty()) {
            Log.d(TAG, "syncPendingTasks: Nothing to sync")
            onComplete?.invoke(true)
            return
        }

        val uid = auth.currentUser?.uid ?: return
        val batch = db.batch()
        val userRef = db.collection("users").document(uid).collection("tasks")
        pending.forEach { task ->
            batch.set(userRef.document(task.id), task)
        }

        val pendingIds = pending.map { it.id }

        batch.commit()
            .addOnSuccessListener {
                Log.i(TAG, "syncPendingTasks: Synced ${pending.size} items")
                TaskLocalDataSource.markSynced(pendingIds)
                pendingSyncIds.removeAll(pendingIds)
                syncToRestApi()
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "syncPendingTasks: Failed", e)
                onComplete?.invoke(false)
            }
    }

    fun canDeleteWhileOffline(): Boolean = auth.currentUser == null || canSyncNow()

    fun initializeSampleTasks() {
        if (auth.currentUser == null && tasks.isEmpty()) {
            val today = Date()
            val calendar = Calendar.getInstance()

            addTask(
                Task(
                    title = "Review project proposal",
                    description = "Go through the quarterly project proposal and provide feedback",
                    dueDate = today,
                    dueTime = "14:00"
                )
            )
            addTask(
                Task(
                    title = "Team meeting preparation",
                    description = "Prepare slides and agenda for tomorrow's team meeting",
                    dueDate = today,
                    dueTime = "16:30"
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            addTask(
                Task(
                    title = "Client presentation",
                    description = "Present the new design concepts to the client",
                    dueDate = calendar.time,
                    dueTime = "10:00"
                )
            )
            Log.i(TAG, "initializeSampleTasks: Seeded demo data")
        }
    }
}
