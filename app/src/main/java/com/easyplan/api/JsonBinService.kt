package com.easyplan.api

import com.easyplan.data.Task
import retrofit2.Call
import retrofit2.http.*

/**
 * JsonBinService - Retrofit interface for JSONBin.io REST API
 * 
 * JSONBin.io is a free JSON storage service that provides a REST API
 * for storing and retrieving JSON data. This interface defines the
 * endpoints for task management operations.
 * 
 * API Documentation: https://jsonbin.io/api-reference
 * 
 * Features:
 * - Create/Read/Update operations for task collections
 * - No authentication required for public bins (or use API key for private)
 * - Automatic JSON serialization/deserialization
 * 
 * @author EasyPlan Team
 * @version 1.0
 */
interface JsonBinService {
    
    /**
     * Response wrapper for JSONBin API
     * JSONBin returns data in a "record" field
     */
    data class JsonBinResponse<T>(
        val record: T,
        val metadata: Metadata? = null
    )
    
    data class Metadata(
        val id: String,
        val createdAt: String
    )
    
    /**
     * Task collection wrapper for storing multiple tasks
     */
    data class TaskCollection(
        val tasks: List<Task>,
        val userId: String,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    /**
     * Creates a new bin with task data
     * 
     * @param apiKey Your JSONBin API key (optional, use for private bins)
     * @param collection The task collection to store
     * @return Response containing the created bin metadata
     */
    @POST("b")
    @Headers("Content-Type: application/json")
    fun createBin(
        @Header("X-Master-Key") apiKey: String,
        @Body collection: TaskCollection
    ): Call<JsonBinResponse<TaskCollection>>
    
    /**
     * Retrieves task data from a bin
     * 
     * @param apiKey Your JSONBin API key
     * @param binId The ID of the bin to retrieve
     * @return Response containing the task collection
     */
    @GET("b/{binId}/latest")
    fun getTasks(
        @Header("X-Master-Key") apiKey: String,
        @Path("binId") binId: String
    ): Call<JsonBinResponse<TaskCollection>>
    
    /**
     * Updates an existing bin with new task data
     * 
     * @param apiKey Your JSONBin API key
     * @param binId The ID of the bin to update
     * @param collection The updated task collection
     * @return Response containing the updated data
     */
    @PUT("b/{binId}")
    @Headers("Content-Type: application/json")
    fun updateTasks(
        @Header("X-Master-Key") apiKey: String,
        @Path("binId") binId: String,
        @Body collection: TaskCollection
    ): Call<JsonBinResponse<TaskCollection>>
}

