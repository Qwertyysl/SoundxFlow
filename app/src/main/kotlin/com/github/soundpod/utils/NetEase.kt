package com.github.soundpod.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object NetEase {
    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                })
            }
            defaultRequest {
                url("https://music.163.com/api/")
                header("Referer", "https://music.163.com")
            }
        }
    }

    @Serializable
    data class SearchResponse(
        val result: SearchData? = null,
        val code: Int? = null
    )

    @Serializable
    data class SearchData(
        val songs: List<Song>? = null
    )

    @Serializable
    data class Song(
        val id: Long,
        val name: String,
        val artists: List<Artist>? = null,
        val duration: Long? = null
    )

    @Serializable
    data class Artist(
        val name: String
    )

    @Serializable
    data class LyricResponse(
        val lrc: LyricData? = null,
        val code: Int? = null
    )

    @Serializable
    data class LyricData(
        val lyric: String? = null
    )

    suspend fun fetchLyrics(
        artist: String,
        title: String,
        durationMs: Long
    ): Result<String?> {
        return runCatching {
            val keyword = "$artist - $title"
            val searchResponse = client.get("search/get/web") {
                parameter("s", keyword)
                parameter("type", 1)
                parameter("limit", 5)
            }.body<SearchResponse>()

            val songs = searchResponse.result?.songs ?: return@runCatching null
            
            // Find the best match by duration
            val bestMatch = songs.find { song ->
                song.duration?.let { d ->
                    Math.abs(d - durationMs) <= 5000 // 5 seconds tolerance
                } ?: false
            } ?: songs.firstOrNull() ?: return@runCatching null

            val lyricResponse = client.get("song/lyric") {
                parameter("id", bestMatch.id)
                parameter("lv", 1)
                parameter("kv", 1)
                parameter("tv", -1)
            }.body<LyricResponse>()

            lyricResponse.lrc?.lyric
        }
    }
}
