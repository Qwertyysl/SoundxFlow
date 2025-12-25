package com.github.soundpod.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object LrcLib {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
            defaultRequest {
                url("https://lrclib.net/api/")
                header("User-Agent", "SoundPod (https://github.com/soundpod)")
            }
        }
    }

    @Serializable
    data class LyricsResponse(
        val id: Int? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val albumName: String? = null,
        val duration: Double? = null,
        val instrumental: Boolean? = null,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null
    )

    suspend fun fetchLyrics(
        artist: String,
        title: String,
        album: String? = null,
        duration: Long? = null
    ): Result<LyricsResponse?> {
        return runCatching {
            client.get("get") {
                parameter("artist_name", artist)
                parameter("track_name", title)
                if (album != null) parameter("album_name", album)
                if (duration != null) parameter("duration", duration)
            }.body<LyricsResponse>()
        }
    }

    suspend fun searchLyrics(query: String): Result<List<LyricsResponse>> {
        return runCatching {
            client.get("search") {
                parameter("q", query)
            }.body<List<LyricsResponse>>()
        }
    }
}
