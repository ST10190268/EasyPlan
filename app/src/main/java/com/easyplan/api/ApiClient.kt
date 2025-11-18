package com.easyplan.api

import android.content.Context
import android.util.Log
import com.easyplan.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ApiClient - Singleton object for managing REST API connections
 * 
 * This object provides a centralized Retrofit instance configured for
 * JSONBin.io API communication. It includes:
 * - HTTP logging for debugging
 * - Timeout configuration
 * - GSON converter for JSON serialization
 * - Singleton pattern for efficient resource usage
 * 
 * @author EasyPlan Team
 * @version 1.0
 * 
 * References:
 * - Retrofit: https://square.github.io/retrofit/
 * - OkHttp: https://square.github.io/okhttp/
 */
object ApiClient {

    private const val TAG = "ApiClient"

    // Base URL will be loaded from string resources
    private var baseUrl: String? = null
    
    /**
     * HTTP logging interceptor for debugging API calls
     * Logs request/response details to Logcat
     */
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    /**
     * OkHttp client with custom configuration
     * - 30 second connection timeout
     * - 30 second read timeout
     * - 30 second write timeout
     * - HTTP logging enabled
     */
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()
    
    /**
     * Retrofit instance configured for JSONBin.io
     * Uses GSON for JSON conversion
     * Base URL is loaded from string resources
     */
    private var retrofit: Retrofit? = null

    /**
     * Initialize the API client with application context
     * This must be called before using the API client
     *
     * @param context Application context for accessing string resources
     */
    fun initialize(context: Context) {
        if (retrofit == null) {
            baseUrl = context.getString(R.string.jsonbin_base_url)
            Log.d(TAG, "Initializing Retrofit instance for JSONBin API with base URL: $baseUrl")
            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl!!)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    /**
     * Get JSONBin service instance
     * Provides access to all API endpoints
     *
     * @return JsonBinService instance
     * @throws IllegalStateException if ApiClient is not initialized
     */
    fun getJsonBinService(): JsonBinService {
        if (retrofit == null) {
            throw IllegalStateException("ApiClient must be initialized with context before use. Call ApiClient.initialize(context) first.")
        }
        Log.d(TAG, "Providing JsonBinService instance")
        return retrofit!!.create(JsonBinService::class.java)
    }
}

