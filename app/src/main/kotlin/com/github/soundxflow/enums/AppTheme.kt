package com.github.soundxflow.enums

import androidx.annotation.StringRes
import com.github.soundxflow.R

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
    )
}
