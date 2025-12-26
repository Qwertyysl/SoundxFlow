package com.github.soundxflow.azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.github.soundxflow.utils.shouldBePlaying
import com.github.soundxflow.MainActivity
import com.github.soundxflow.service.PlayerService
import android.os.IBinder
import android.content.ServiceConnection
import android.content.ComponentName

class AzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.github.soundxflow.azan.PAUSE" -> {
                // We check if something is playing before triggering Azan logic
                // But PAUSE is 5 seconds before. Let's just handle it in PLAY.
            }
            "com.github.soundxflow.azan.PLAY" -> {
                val playRequestIntent = Intent("com.github.soundxflow.azan.PLAY_REQUEST").apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(playRequestIntent)
            }
        }
    }
}
