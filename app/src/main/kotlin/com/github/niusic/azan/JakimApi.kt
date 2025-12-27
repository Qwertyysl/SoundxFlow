package com.github.niusic.azan

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object JakimApi {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getPrayerTimes(zone: String): JakimResponse? {
        return try {
            client.get("https://www.e-solat.gov.my/index.php") {
                parameter("r", "esolatApi/takwimsolat")
                parameter("period", "month")
                parameter("zone", zone)
            }.body<JakimResponse>()
        } catch (e: Exception) {
            Log.e("JakimApi", "Error fetching prayer times", e)
            null
        }
    }
}

