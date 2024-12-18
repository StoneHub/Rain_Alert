package com.StoneCode.rain_alert.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.stonecode.rain_alert.RAIN_ALARM" || intent.action == "android.intent.action.BOOT_COMPLETED") {
            Log.d("AlarmReceiver", "Alarm received, starting RainService")

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