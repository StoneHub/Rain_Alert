package com.stoneCode.rain_alert.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.os.Build
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.stonecode.rain_alert.RAIN_ALARM" || intent.action == "android.intent.action.BOOT_COMPLETED") {
            Log.d("AlarmReceiver", "Alarm received, starting RainService")

            val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (!hasLocationPermission) {
                Log.d("AlarmReceiver", "Missing location permissions, starting MainActivity instead of service")
                // Launch MainActivity instead of service when permissions are missing
                val mainActivityIntent = Intent(context, com.stoneCode.rain_alert.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("checkPermissions", true)
                }
                context.startActivity(mainActivityIntent)
                return
            }
            
            val serviceIntent = Intent(context, RainService::class.java).apply {
                action = "START_RAIN_CHECK"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}