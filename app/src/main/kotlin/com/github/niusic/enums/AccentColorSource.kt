package com.github.niusic.enums

import androidx.annotation.StringRes
import com.github.niusic.R

enum class AccentColorSource(
    @get:StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.defualt,
    ),
    Dynamic(
        resourceId = R.string.dynamic,
    ),
    MaterialYou(
        resourceId = R.string.material_you,
    )
}
