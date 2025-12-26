package com.github.soundxflow.azan

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object JakimApi {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getPrayerTimes(zone: String): JakimResponse? {
        return try {
            val response: String = client.get("https://www.e-solat.gov.my/index.php") {
                parameter("r", "esolatApi/takwimsolat")
                parameter("period", "month")
                parameter("zone", zone)
            }.body()
            
            Log.d("JakimApi", "Response: $response")

            Json { 
                ignoreUnknownKeys = true 
                isLenient = true 
            }.decodeFromString<JakimResponse>(response)
        } catch (e: Exception) {
            Log.e("JakimApi", "Error fetching prayer times", e)
            null
        }
    }
}
