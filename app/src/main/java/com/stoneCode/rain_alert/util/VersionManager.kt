package com.stoneCode.rain_alert.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manages app version information and handles app updates.
 */
class VersionManager(private val context: Context) {
    companion object {
        private val Context.versionDataStore by preferencesDataStore("version_data")
        private val LAST_VERSION_CODE = intPreferencesKey("last_version_code")
        private val LAST_VERSION_NAME = stringPreferencesKey("last_version_name")
        private const val TAG = "VersionManager"
    }

    // Get the current app version code
    private fun getCurrentVersionCode(): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting version code", e)
            0
        }
    }

    // Get the current app version name
    private fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting version name", e)
            ""
        }
    }

    // Get the last saved version code
    fun getLastVersionCode(): Flow<Int> {
        return context.versionDataStore.data.map { preferences ->
            preferences[LAST_VERSION_CODE] ?: 0
        }
    }

    // Get the last saved version name
    fun getLastVersionName(): Flow<String> {
        return context.versionDataStore.data.map { preferences ->
            preferences[LAST_VERSION_NAME] ?: ""
        }
    }

    // Update the saved version info
    private suspend fun updateVersionInfo() {
        context.versionDataStore.edit { preferences ->
            preferences[LAST_VERSION_CODE] = getCurrentVersionCode()
            preferences[LAST_VERSION_NAME] = getCurrentVersionName()
        }
    }

    /**
     * Checks if the app has been updated and performs any necessary actions.
     * Returns true if an update was detected.
     */
    suspend fun checkForUpdate(): Boolean {
        val currentVersionCode = getCurrentVersionCode()
        val lastVersionCode = getLastVersionCode().first()
        val isUpdate = lastVersionCode > 0 && currentVersionCode > lastVersionCode

        if (isUpdate) {
            Log.i(TAG, "App updated from version code $lastVersionCode to $currentVersionCode")
            
            // Log the update event to Firebase
            val firebaseLogger = FirebaseLogger.getInstance()
            firebaseLogger.logAppUpdate(
                oldVersionCode = lastVersionCode,
                newVersionCode = currentVersionCode,
                oldVersionName = getLastVersionName().first(),
                newVersionName = getCurrentVersionName()
            )
            
            // Perform any update-specific actions here
            // handleUpdate(lastVersionCode, currentVersionCode)
        }

        // For first install
        if (lastVersionCode == 0) {
            Log.i(TAG, "First install detected, version code: $currentVersionCode")
            // Perform first install actions if needed
        }

        // Always update version info
        updateVersionInfo()
        
        return isUpdate
    }

    /**
     * Handles specific update actions based on version changes.
     * This can be expanded as the app evolves to handle database migrations,
     * preference changes, etc.
     */
    private fun handleUpdate(oldVersion: Int, newVersion: Int) {
        // Example of version-specific upgrade handling
        // when (oldVersion) {
        //     1 -> {
        //         // Upgrade from version 1 to 2 requires some action
        //         if (newVersion >= 2) {
        //             // Perform specific upgrade steps
        //         }
        //     }
        // }
    }
}
