package com.github.niusic.enums

import androidx.annotation.StringRes
import com.github.niusic.R

enum class AppThemeColor(
    @get:StringRes val resourceId: Int
) {
    Dark(
        resourceId = R.string.dark_theme,
    ),
    Light(
        resourceId = R.string.light_theme,
    ),
    System(
        resourceId = R.string.System_default,
    ),
    Spotify(
        resourceId = R.string.spotify,
    ),
    YouTubeMusic(
        resourceId = R.string.youtube_music,
    )
}

