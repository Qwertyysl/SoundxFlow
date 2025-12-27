package com.github.niusic.azan

import kotlinx.serialization.Serializable

@Serializable
data class JakimResponse(
    val status: String? = null,
    val serverTime: String? = null,
    val period: String? = null,
    val zone: String? = null,
    val prayerTime: List<PrayerTime> = emptyList()
)

@Serializable
data class PrayerTime(
    val hijri: String? = null,
    val date: String? = null,
    val day: String? = null,
    val imsak: String? = null,
    val fajr: String? = null,
    val syuruk: String? = null,
    val dhuhr: String? = null,
    val asr: String? = null,
    val maghrib: String? = null,
    val isha: String? = null
)

data class PrayerTimeDisplay(
    val name: String,
    val time: String // 24h format for internal use
)

