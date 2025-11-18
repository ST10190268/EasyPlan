package com.easyplan.local

import android.content.Context
import android.util.Log
import com.easyplan.data.Task

/**
 * TaskLocalDataSource - Helper around [TaskDao] with logging.
 */
object TaskLocalDataSource {

    private const val TAG = "TaskLocalDataSource"

    private var taskDao: TaskDao? = null

    fun initialize(context: Context) {
        if (taskDao == null) {
            taskDao = TaskDatabase.getInstance(context).taskDao()
            Log.d(TAG, "initialize: Room task cache ready")
        }
    }

    fun cacheTasks(tasks: List<Task>) {
        val dao = taskDao ?: return
        dao.upsertAll(tasks.map { TaskEntity.fromTask(it) })
        Log.d(TAG, "cacheTasks: Cached ${tasks.size} tasks locally")
    }

    fun upsert(task: Task, needsSync: Boolean) {
        val dao = taskDao ?: return
        dao.upsert(TaskEntity.fromTask(task, needsSync))
        Log.v(TAG, "upsert: Stored ${task.id} (needsSync=$needsSync)")
    }

    fun delete(taskId: String) {
        val dao = taskDao ?: return
        dao.deleteById(taskId)
        Log.v(TAG, "delete: Removed task $taskId from Room cache")
    }

    fun getAllTasks(): List<Task> {
        val dao = taskDao ?: return emptyList()
        val loaded = dao.getAllTasks().map { it.toTask() }
        Log.d(TAG, "getAllTasks: Loaded ${loaded.size} tasks from Room")
        return loaded
    }

    fun getAllEntities(): List<TaskEntity> {
        val dao = taskDao ?: return emptyList()
        return dao.getAllTasks()
    }

    fun getPendingSyncTasks(): List<Task> {
        val dao = taskDao ?: return emptyList()
        val pending = dao.getPendingSyncTasks().map { it.toTask() }
        Log.d(TAG, "getPendingSyncTasks: ${pending.size} pending items")
        return pending
    }

    fun getPendingSyncIds(): List<String> {
        val dao = taskDao ?: return emptyList()
        return dao.getPendingSyncTasks().map { it.id }
    }

    fun markSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        val dao = taskDao ?: return
        dao.updateSyncState(ids, false)
        Log.d(TAG, "markSynced: Updated ${ids.size} tasks to synced")
    }
}
