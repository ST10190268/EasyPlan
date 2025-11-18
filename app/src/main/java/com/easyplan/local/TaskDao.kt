package com.easyplan.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * TaskDao - CRUD queries for the offline Room cache.
 *
 * The DAO exposes synchronous APIs because the builder is configured with
 * allowMainThreadQueries() for this academic prototype. In production,
 * these should run off the main thread.
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE needsSync = 1 ORDER BY createdAtMillis ASC")
    fun getPendingSyncTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    fun deleteById(taskId: String)

    @Query("UPDATE tasks SET needsSync = :needsSync WHERE id IN (:taskIds)")
    fun updateSyncState(taskIds: List<String>, needsSync: Boolean)
}
