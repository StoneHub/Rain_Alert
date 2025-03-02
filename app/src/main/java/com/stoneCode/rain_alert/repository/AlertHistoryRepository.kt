package com.stoneCode.rain_alert.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stoneCode.rain_alert.data.AlertHistoryEntry
import com.stoneCode.rain_alert.data.AlertType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Context.alertHistoryDataStore by preferencesDataStore(name = "alert_history")
private val ALERT_HISTORY_KEY = stringPreferencesKey("alert_history")

/**
 * Repository for managing alert history
 */
class AlertHistoryRepository(private val context: Context) {
    
    /**
     * Add a new alert to the history
     */
    suspend fun addAlertToHistory(
        type: AlertType,
        weatherConditions: String,
        temperature: Double? = null,
        precipitation: Int? = null,
        windSpeed: Double? = null
    ) {
        val newEntry = AlertHistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = Date(),
            type = type,
            weatherConditions = weatherConditions,
            temperature = temperature,
            precipitation = precipitation,
            windSpeed = windSpeed
        )
        
        context.alertHistoryDataStore.edit { preferences ->
            val currentHistory = preferences[ALERT_HISTORY_KEY] ?: "[]"
            val historyArray = JSONArray(currentHistory)
            historyArray.put(entryToJson(newEntry))
            
            // Limit history to most recent 50 entries
            val limitedArray = JSONArray()
            val startIndex = if (historyArray.length() > 50) historyArray.length() - 50 else 0
            for (i in startIndex until historyArray.length()) {
                limitedArray.put(historyArray.get(i))
            }
            
            preferences[ALERT_HISTORY_KEY] = limitedArray.toString()
        }
    }
    
    /**
     * Get the full alert history as a Flow
     */
    fun getAlertHistory(): Flow<List<AlertHistoryEntry>> {
        return context.alertHistoryDataStore.data.map { preferences ->
            val historyJson = preferences[ALERT_HISTORY_KEY] ?: "[]"
            val historyArray = JSONArray(historyJson)
            val entries = mutableListOf<AlertHistoryEntry>()
            
            for (i in 0 until historyArray.length()) {
                val entry = jsonToEntry(historyArray.getJSONObject(i))
                entries.add(entry)
            }
            
            // Sort by timestamp, most recent first
            entries.sortByDescending { it.timestamp }
            entries
        }
    }
    
    /**
     * Clear all alert history
     */
    suspend fun clearHistory() {
        context.alertHistoryDataStore.edit { preferences ->
            preferences[ALERT_HISTORY_KEY] = "[]"
        }
    }
    
    // Convert AlertHistoryEntry to JSON
    private fun entryToJson(entry: AlertHistoryEntry): JSONObject {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return JSONObject().apply {
            put("id", entry.id)
            put("timestamp", dateFormat.format(entry.timestamp))
            put("type", entry.type.name)
            put("weatherConditions", entry.weatherConditions)
            entry.temperature?.let { put("temperature", it) }
            entry.precipitation?.let { put("precipitation", it) }
            entry.windSpeed?.let { put("windSpeed", it) }
        }
    }
    
    // Convert JSON to AlertHistoryEntry
    private fun jsonToEntry(json: JSONObject): AlertHistoryEntry {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return AlertHistoryEntry(
            id = json.getString("id"),
            timestamp = dateFormat.parse(json.getString("timestamp")) ?: Date(),
            type = AlertType.valueOf(json.getString("type")),
            weatherConditions = json.getString("weatherConditions"),
            temperature = if (json.has("temperature")) json.getDouble("temperature") else null,
            precipitation = if (json.has("precipitation")) json.getInt("precipitation") else null,
            windSpeed = if (json.has("windSpeed")) json.getDouble("windSpeed") else null
        )
    }
}