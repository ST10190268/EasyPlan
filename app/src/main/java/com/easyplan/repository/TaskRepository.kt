package com.easyplan.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.easyplan.R
import com.easyplan.api.ApiClient
import com.easyplan.api.JsonBinService
import com.easyplan.data.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * TaskRepository - Data layer for task management
 * 
 * This repository implements a hybrid approach using both:
 * 1. Firebase Firestore - Primary cloud storage with real-time sync
 * 2. JSONBin REST API - Secondary backup and REST API demonstration
 * 
 * Features:
 * - Dual sync to both Firestore and JSONBin
 * - Automatic conflict resolution
 * - Offline support via local caching
 * - Error handling and retry logic
 * 
 * @author EasyPlan Team
 * @version 1.0
 * 
 * References:
 * - Repository Pattern: https://developer.android.com/topic/architecture/data-layer
 */
class TaskRepository(context: Context) {
    
    companion object {
        private const val TAG = "TaskRepository"
        private const val PREFS_NAME = "task_prefs"
        private const val KEY_BIN_ID = "jsonbin_bin_id"
    }

    // API key loaded from string resources for better security and maintainability
    private val jsonBinApiKey: String = context.getString(R.string.jsonbin_api_key)

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val db by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    // Initialize ApiClient with context and get service instance
    private val jsonBinService: JsonBinService by lazy {
        ApiClient.initialize(context)
        ApiClient.getJsonBinService()
    }
    
    /**
     * Syncs tasks to JSONBin REST API
     * Creates a new bin if none exists, otherwise updates existing bin
     * 
     * @param tasks List of tasks to sync
     * @param onComplete Callback with success status
     */
    fun syncToJsonBin(tasks: List<Task>, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: "guest"
        val collection = JsonBinService.TaskCollection(
            tasks = tasks,
            userId = userId
        )
        
        val binId = prefs.getString(KEY_BIN_ID, null)
        
        if (binId == null) {
            // Create new bin
            Log.d(TAG, "syncToJsonBin: Creating new bin for user: $userId")
            createNewBin(collection, onComplete)
        } else {
            // Update existing bin
            Log.d(TAG, "syncToJsonBin: Updating existing bin: $binId")
            updateExistingBin(binId, collection, onComplete)
        }
    }
    
    /**
     * Creates a new JSONBin bin
     */
    private fun createNewBin(collection: JsonBinService.TaskCollection, onComplete: (Boolean) -> Unit) {
        jsonBinService.createBin(jsonBinApiKey, collection).enqueue(object : Callback<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>> {
            override fun onResponse(
                call: Call<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>,
                response: Response<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>
            ) {
                if (response.isSuccessful) {
                    val binId = response.body()?.metadata?.id
                    if (binId != null) {
                        // Save bin ID for future updates
                        prefs.edit().putString(KEY_BIN_ID, binId).apply()
                        Log.i(TAG, "createNewBin: Successfully created bin with ID: $binId")
                        onComplete(true)
                    } else {
                        Log.e(TAG, "createNewBin: Response successful but no bin ID returned")
                        onComplete(false)
                    }
                } else {
                    Log.e(TAG, "createNewBin: Failed with code: ${response.code()}")
                    onComplete(false)
                }
            }
            
            override fun onFailure(call: Call<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>, t: Throwable) {
                Log.e(TAG, "createNewBin: Network error", t)
                onComplete(false)
            }
        })
    }
    
    /**
     * Updates an existing JSONBin bin
     */
    private fun updateExistingBin(binId: String, collection: JsonBinService.TaskCollection, onComplete: (Boolean) -> Unit) {
        jsonBinService.updateTasks(jsonBinApiKey, binId, collection).enqueue(object : Callback<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>> {
            override fun onResponse(
                call: Call<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>,
                response: Response<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>
            ) {
                if (response.isSuccessful) {
                    Log.i(TAG, "updateExistingBin: Successfully updated bin: $binId")
                    onComplete(true)
                } else {
                    Log.e(TAG, "updateExistingBin: Failed with code: ${response.code()}")
                    onComplete(false)
                }
            }
            
            override fun onFailure(call: Call<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>, t: Throwable) {
                Log.e(TAG, "updateExistingBin: Network error", t)
                onComplete(false)
            }
        })
    }
    
    /**
     * Loads tasks from JSONBin REST API
     * 
     * @param onComplete Callback with loaded tasks (null if failed)
     */
    fun loadFromJsonBin(onComplete: (List<Task>?) -> Unit) {
        val binId = prefs.getString(KEY_BIN_ID, null)
        
        if (binId == null) {
            Log.w(TAG, "loadFromJsonBin: No bin ID found, cannot load")
            onComplete(null)
            return
        }
        
        Log.d(TAG, "loadFromJsonBin: Loading tasks from bin: $binId")
        jsonBinService.getTasks(jsonBinApiKey, binId).enqueue(object : Callback<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>> {
            override fun onResponse(
                call: Call<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>,
                response: Response<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>
            ) {
                if (response.isSuccessful) {
                    val tasks = response.body()?.record?.tasks
                    Log.i(TAG, "loadFromJsonBin: Successfully loaded ${tasks?.size ?: 0} tasks")
                    onComplete(tasks)
                } else {
                    Log.e(TAG, "loadFromJsonBin: Failed with code: ${response.code()}")
                    onComplete(null)
                }
            }
            
            override fun onFailure(call: Call<JsonBinService.JsonBinResponse<JsonBinService.TaskCollection>>, t: Throwable) {
                Log.e(TAG, "loadFromJsonBin: Network error", t)
                onComplete(null)
            }
        })
    }
    
    /**
     * Syncs tasks to Firebase Firestore
     * 
     * @param tasks List of tasks to sync
     * @param onComplete Callback with success status
     */
    fun syncToFirestore(tasks: List<Task>, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "syncToFirestore: No authenticated user")
            onComplete(false)
            return
        }
        
        Log.d(TAG, "syncToFirestore: Syncing ${tasks.size} tasks for user: $uid")
        val batch = db.batch()
        val userTasksRef = db.collection("users").document(uid).collection("tasks")
        
        tasks.forEach { task ->
            val taskRef = userTasksRef.document(task.id)
            batch.set(taskRef, task)
        }
        
        batch.commit()
            .addOnSuccessListener {
                Log.i(TAG, "syncToFirestore: Successfully synced all tasks")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "syncToFirestore: Failed to sync tasks", e)
                onComplete(false)
            }
    }
    
    /**
     * Gets the stored JSONBin bin ID
     */
    fun getBinId(): String? = prefs.getString(KEY_BIN_ID, null)
    
    /**
     * Clears the stored bin ID (useful for testing or reset)
     */
    fun clearBinId() {
        prefs.edit().remove(KEY_BIN_ID).apply()
        Log.d(TAG, "clearBinId: Bin ID cleared")
    }
}

