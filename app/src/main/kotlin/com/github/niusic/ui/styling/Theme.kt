package com.github.niusic.ui.styling

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.niusic.enums.AppThemeColor
import com.github.core.ui.BuiltInFontFamily.System
import com.github.core.ui.ColorMode
import com.github.core.ui.ColorSource
import com.github.core.ui.Darkness
import com.github.core.ui.DesignStyle
import com.github.core.ui.LocalAppearance
import com.github.core.ui.appearance

private val SpotifyColorScheme = darkColorScheme(
    primary = Color(0xFF1DB954), // Spotify Green
    onPrimary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    secondary = Color(0xFF1DB954)
)

private val YouTubeMusicColorScheme = darkColorScheme(
    primary = Color(0xFFFF0000), // YouTube Red
    onPrimary = Color.White,
    background = Color(0xFF030303), // Slightly darker black
    onBackground = Color.White,
    surface = Color(0xFF030303),
    onSurface = Color.White,
    secondary = Color(0xFFFF0000)
)

private val PureBlackColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,       // pure black
    onBackground = Color.White,
    surface = Color.Black,          // pure black
    onSurface = Color.White
)

private val OffsetWhiteColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = Color(0xFFF6F6F8), // Off-white
    onBackground = Color.Black,
    surface = Color(0xFFF6F6F8),    // Off-white
    onSurface = Color.Black
)

private val MaterialDarkScheme = darkColorScheme(
    background = Color(0xFF121212), // Material dark background
    surface = Color(0xFF121212)
)

private val MaterialLightScheme = lightColorScheme(
    background = Color.White,
    surface = Color.White
)

@Composable
fun AppTheme(
    appThemeColor: AppThemeColor = AppThemeColor.System,
    usePureBlack: Boolean = false,
    useMaterialNeutral: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    designStyle: DesignStyle = DesignStyle.Classic,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseColorScheme = when {
        appThemeColor == AppThemeColor.Spotify -> SpotifyColorScheme
        appThemeColor == AppThemeColor.YouTubeMusic -> YouTubeMusicColorScheme
        
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useMaterialNeutral && darkTheme -> MaterialDarkScheme
        useMaterialNeutral && !darkTheme -> MaterialLightScheme

        usePureBlack && darkTheme -> PureBlackColorScheme

        appThemeColor == AppThemeColor.Dark || (appThemeColor == AppThemeColor.System && darkTheme) -> PureBlackColorScheme
        else -> OffsetWhiteColorScheme
    }

    val appearance = appearance(
        source = if (appThemeColor == AppThemeColor.Spotify || appThemeColor == AppThemeColor.YouTubeMusic) ColorSource.Default else ColorSource.Default,
        mode = if (darkTheme || appThemeColor == AppThemeColor.Spotify || appThemeColor == AppThemeColor.YouTubeMusic) ColorMode.Dark else ColorMode.Light,
        darkness = if ((usePureBlack || appThemeColor == AppThemeColor.Spotify || appThemeColor == AppThemeColor.YouTubeMusic) && (darkTheme || appThemeColor != AppThemeColor.Light)) Darkness.AMOLED else Darkness.Normal,
        materialAccentColor = null,
        sampleBitmap = null,
        fontFamily = System,
        applyFontPadding = true,
        thumbnailRoundness = 8.dp,
        designStyle = designStyle
    )
    
    // Override appearance accent for Spotify/YT Music
    val finalAppearance = if (appThemeColor == AppThemeColor.Spotify) {
        appearance.copy(colorPalette = appearance.colorPalette.copy(accent = Color(0xFF1DB954), onAccent = Color.Black))
    } else if (appThemeColor == AppThemeColor.YouTubeMusic) {
        appearance.copy(colorPalette = appearance.colorPalette.copy(accent = Color(0xFFFF0000), onAccent = Color.White))
    } else {
        appearance
    }

    CompositionLocalProvider(LocalAppearance provides finalAppearance) {
        MaterialTheme(
            colorScheme = baseColorScheme,
            typography = Typography,
            content = content
        )
    }

}

