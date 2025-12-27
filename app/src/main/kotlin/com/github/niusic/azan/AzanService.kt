package com.github.niusic.azan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.util.Log
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.github.niusic.R
import com.github.niusic.utils.azanAudioPathKey
import com.github.niusic.utils.azanQuietModeKey

import com.github.niusic.utils.isAtLeastAndroid8
import com.github.niusic.utils.preferences
import kotlinx.coroutines.*

import androidx.core.content.edit
import com.github.niusic.utils.isAzanPlayingKey

class AzanService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var wasPlayingBeforeAzan = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        if (action == "STOP_AZAN") {
            stopAzan()
            return START_NOT_STICKY
        }

        if (action == "PLAY_AZAN") {
            preferences.edit { putBoolean(isAzanPlayingKey, true) }
            wasPlayingBeforeAzan = intent.getBooleanExtra("WAS_PLAYING", false)
            createNotificationChannel()
            
            val stopIntent = Intent(this, AzanService::class.java).apply {
                this.action = "STOP_AZAN"
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 
                0, 
                stopIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "azan_channel")
                .setContentTitle("Azan")
                .setContentText("Playing Azan...")
                .setSmallIcon(R.drawable.app_icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .addAction(R.drawable.app_icon, "Stop", stopPendingIntent) // Replace app_icon with a stop icon if available
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
        val isQuietMode = preferences.getBoolean(azanQuietModeKey, false)
        if (isQuietMode) {
            scope.launch {
                delay(300000)
                if (wasPlayingBeforeAzan) resumePlayer()
                preferences.edit { putBoolean(isAzanPlayingKey, false) }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return
        }

        val audioPath = preferences.getString(azanAudioPathKey, "")
        
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (audioPath.isNullOrEmpty()) {
                    val assetFileDescriptor = resources.openRawResourceFd(com.github.niusic.R.raw.azantv3)
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
                        if (wasPlayingBeforeAzan) {
                            resumePlayer()
                        }
                        preferences.edit { putBoolean(isAzanPlayingKey, false) }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback for missing resource or error
            if (audioPath.isNullOrEmpty()) {
                 scope.launch {
                    delay(60000) // Simulate azan duration
                    if (wasPlayingBeforeAzan) {
                        resumePlayer()
                    }
                    preferences.edit { putBoolean(isAzanPlayingKey, false) }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } else {
                if (wasPlayingBeforeAzan) {
                    resumePlayer()
                }
                preferences.edit { putBoolean(isAzanPlayingKey, false) }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopAzan() {
        preferences.edit { putBoolean(isAzanPlayingKey, false) }
        mediaPlayer?.stop()
        if (wasPlayingBeforeAzan) {
            resumePlayer()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun resumePlayer() {
        val intent = Intent("com.github.niusic.play").apply {
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

