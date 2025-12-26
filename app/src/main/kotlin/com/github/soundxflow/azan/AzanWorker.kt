package com.github.soundxflow.azan

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.work.*
import com.github.soundxflow.utils.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AzanWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = applicationContext.preferences
        val azanEnabled = preferences.getBoolean(azanReminderEnabledKey, false)
        if (!azanEnabled) {
            return Result.success()
        }

        val zone = preferences.getString(azanLocationKey, "WLY01") ?: "WLY01"
        val response = JakimApi.getPrayerTimes(zone) ?: run {
            return Result.retry()
        }

        if (response.prayerTime.isEmpty()) {
            return Result.failure()
        }

        // Update UI with today's times
        val todayStr = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date())
        val todayPrayerTime = response.prayerTime.find { it.date == todayStr } ?: response.prayerTime.first()
        
        preferences.edit {
            putString(prayerTimesTodayKey, Json.encodeToString(todayPrayerTime))
        }

        // Schedule alarms for all days returned (usually just today if period=today, or a week if changed)
        response.prayerTime.forEach { prayerTime ->
            scheduleAlarms(prayerTime)
        }

        return Result.success()
    }

    private fun scheduleAlarms(prayerTime: PrayerTime) {
        val times = listOfNotNull(
            prayerTime.fajr,
            prayerTime.dhuhr,
            prayerTime.asr,
            prayerTime.maghrib,
            prayerTime.isha
        )

        val dateStr = prayerTime.date // format is usually "dd-MMM-yyyy"
        
        times.forEachIndexed { index, time ->
            scheduleAlarmForPrayer(time, dateStr, index)
        }
    }

    private fun scheduleAlarmForPrayer(timeStr: String, dateStr: String?, prayerId: Int) {
        val sdfSeconds = SimpleDateFormat("HH:mm:ss", Locale.US)
        val sdfMinutes = SimpleDateFormat("HH:mm", Locale.US)
        
        val prayerTimeDate = try {
            sdfSeconds.parse(timeStr)
        } catch (e: Exception) {
            try {
                sdfMinutes.parse(timeStr)
            } catch (e2: Exception) {
                null
            }
        } ?: return
        
        val prayerCalendar = Calendar.getInstance().apply {
            time = prayerTimeDate
        }

        if (dateStr != null) {
            try {
                val dateSdf = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
                val actualDate = dateSdf.parse(dateStr)
                if (actualDate != null) {
                    val dateCal = Calendar.getInstance().apply { time = actualDate }
                    prayerCalendar.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                    prayerCalendar.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                    prayerCalendar.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                }
            } catch (e: Exception) {
                // Fallback to today if date parsing fails
                val today = Calendar.getInstance()
                prayerCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                prayerCalendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
                prayerCalendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
            }
        } else {
            val today = Calendar.getInstance()
            prayerCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
            prayerCalendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
            prayerCalendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
        }

        val now = System.currentTimeMillis()
        
        // Use a unique ID based on date and prayer to avoid overlapping alarms
        val baseId = (prayerCalendar.get(Calendar.DAY_OF_YEAR) * 100) + (prayerId * 10)

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Use AlarmClock for maximum precision if we can
        val intentOpenApp = Intent(applicationContext, com.github.soundxflow.MainActivity::class.java)
        val pendingIntentOpenApp = PendingIntent.getActivity(
            applicationContext,
            0,
            intentOpenApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Alarm for 5 seconds before
        if (prayerCalendar.timeInMillis - 5000 > now) {
            val intentBefore = Intent(applicationContext, AzanReceiver::class.java).apply {
                action = "com.github.soundxflow.azan.PAUSE"
                putExtra("PRAYER_ID", prayerId)
                `package` = applicationContext.packageName
            }
            val pendingIntentBefore = PendingIntent.getBroadcast(
                applicationContext,
                baseId + 1,
                intentBefore,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmInfo = AlarmManager.AlarmClockInfo(prayerCalendar.timeInMillis - 5000, pendingIntentOpenApp)
            alarmManager.setAlarmClock(alarmInfo, pendingIntentBefore)
        }

        // Alarm for Azan
        if (prayerCalendar.timeInMillis > now) {
            val intentAzan = Intent(applicationContext, AzanReceiver::class.java).apply {
                action = "com.github.soundxflow.azan.PLAY"
                putExtra("PRAYER_ID", prayerId)
                `package` = applicationContext.packageName
            }
            val pendingIntentAzan = PendingIntent.getBroadcast(
                applicationContext,
                baseId + 2,
                intentAzan,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmInfo = AlarmManager.AlarmClockInfo(prayerCalendar.timeInMillis, pendingIntentOpenApp)
            alarmManager.setAlarmClock(alarmInfo, pendingIntentAzan)
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<AzanWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "AzanWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
        
        fun runOnce(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<AzanWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
