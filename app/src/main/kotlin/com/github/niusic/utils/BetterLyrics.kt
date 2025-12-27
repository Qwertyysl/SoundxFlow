package com.github.niusic.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BetterLyrics {
    private val client by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    retryOnConnectionFailure(true)
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
            defaultRequest {
                url("https://lyrics-api.boidu.dev/api/")
            }
        }
    }

    @Serializable
    data class LyricsResponse(
        val id: String? = null,
        val source: String? = null,
        val lyrics: List<LyricLine>? = null
    )

    @Serializable
    data class LyricLine(
        val text: String,
        val time: Long // Time in milliseconds
    )

    suspend fun fetchLyrics(videoId: String): Result<String?> {
        return runCatching {
            val response = client.get("lyrics") {
                parameter("id", videoId)
            }.body<LyricsResponse>()

            if (response.lyrics.isNullOrEmpty()) return@runCatching null

            // Convert to LRC format for compatibility with existing parser
            val lrcBuilder = StringBuilder()
            response.lyrics.forEach { line: LyricLine ->
                val totalSeconds = line.time / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val milliseconds = (line.time % 1000) / 10
                lrcBuilder.append(String.format("[%02d:%02d.%02d]%s\n", minutes, seconds, milliseconds, line.text))
            }
            lrcBuilder.toString()
        }
    }
}


