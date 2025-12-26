package com.github.soundxflow.azan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.github.soundxflow.R
import com.github.soundxflow.utils.azanAudioPathKey
import com.github.soundxflow.utils.isAtLeastAndroid8
import com.github.soundxflow.utils.preferences
import kotlinx.coroutines.*

class AzanService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("AzanService", "onStartCommand with action: $action")
        if (action == "PLAY_AZAN") {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, "azan_channel")
                .setContentTitle("Azan")
                .setContentText("Playing Azan...")
                .setSmallIcon(R.drawable.app_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(AZAN_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(AZAN_NOTIFICATION_ID, notification)
            }
            playAzan()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (isAtLeastAndroid8) {
            val name = "Azan Reminder"
            val descriptionText = "Notifications for Azan"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("azan_channel", name, importance).apply {
                description = descriptionText
                setSound(null, null)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun playAzan() {
        val audioPath = preferences.getString(azanAudioPathKey, "")
        Log.d("AzanService", "playAzan: audioPath=$audioPath")
        
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (audioPath.isNullOrEmpty()) {
                    val assetFileDescriptor = resources.openRawResourceFd(com.github.soundxflow.R.raw.azan)
                    setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
                    assetFileDescriptor.close()
                } else {
                    setDataSource(applicationContext, Uri.parse(audioPath))
                }
                prepare()
                start()
                setOnCompletionListener {
                    scope.launch {
                        delay(10000) // 10 seconds delay as requested
                        resumePlayer()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AzanService", "Error playing Azan", e)
            // Fallback for missing resource or error
            if (audioPath.isNullOrEmpty()) {
                 scope.launch {
                    delay(60000) // Simulate azan duration
                    resumePlayer()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } else {
                resumePlayer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun resumePlayer() {
        val intent = Intent("com.github.soundxflow.play").apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val AZAN_NOTIFICATION_ID = 2001
    }
}
