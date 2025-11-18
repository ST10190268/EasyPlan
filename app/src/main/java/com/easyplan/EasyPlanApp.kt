package com.easyplan

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.easyplan.notifications.NotificationUtils
import com.easyplan.util.LanguageManager
import com.easyplan.util.TaskManager

/**
 * EasyPlanApp - Initializes global utilities (Locale, TaskManager, notifications).
 */
class EasyPlanApp : Application() {

    companion object {
        private const val TAG = "EasyPlanApp"
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network available – attempting offline sync")
            TaskManager.syncPendingTasks()
        }
    }

    override fun onCreate() {
        super.onCreate()
        LanguageManager.applyStoredLanguage(this)
        TaskManager.initialize(this)
        NotificationUtils.ensureChannels(this)
        registerNetworkMonitor()
    }

    private fun registerNetworkMonitor() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            Log.d(TAG, "registerNetworkMonitor: Network callback registered")
        } catch (e: Exception) {
            Log.e(TAG, "registerNetworkMonitor: Failed to register callback", e)
        }
    }
}
