package com.github.soundxflow.azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class AzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AzanReceiver", "Received intent with action: ${intent.action}")
        when (intent.action) {
            "com.github.soundxflow.azan.PAUSE" -> {
                Log.d("AzanReceiver", "Pausing player for Azan")
                val pauseIntent = Intent("com.github.soundxflow.pause").apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(pauseIntent)
            }
            "com.github.soundxflow.azan.PLAY" -> {
                Log.d("AzanReceiver", "Starting AzanService to play Azan")
                val serviceIntent = Intent(context, AzanService::class.java).apply {
                    action = "PLAY_AZAN"
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
