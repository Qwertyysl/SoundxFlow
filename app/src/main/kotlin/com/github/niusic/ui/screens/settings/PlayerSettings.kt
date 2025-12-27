package com.github.niusic.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.github.core.ui.LocalAppearance
import com.github.niusic.R
import com.github.niusic.ui.common.IconSource
import com.github.niusic.ui.components.SettingsCard
import com.github.niusic.ui.components.SettingsScreenLayout
import com.github.niusic.ui.components.SliderSettingsItem
import com.github.niusic.ui.components.SwitchSetting
import com.github.niusic.enums.MusicStylePreset
import com.github.niusic.utils.musicStylePresetKey
import com.github.niusic.utils.isAtLeastAndroid6
import com.github.niusic.utils.persistentQueueKey
import com.github.niusic.utils.rememberPreference
import com.github.niusic.utils.resumePlaybackWhenDeviceConnectedKey
import com.github.niusic.utils.skipSilenceKey
import com.github.niusic.utils.volumeBoosterEnabledKey
import com.github.niusic.utils.volumeBoosterGainKey
import com.github.niusic.utils.volumeNormalizationKey
import com.github.niusic.utils.floatingLyricsFontSizeKey
import com.github.niusic.utils.lyricsFontSizeKey
import java.util.Locale

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    onBackClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var autoPauseEnabled by remember { mutableStateOf(false) }
    var audioFocus by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableFloatStateOf(1.0f) }
    var crossfade by remember { mutableFloatStateOf(5f) }
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    var volumeBoosterEnabled by rememberPreference(volumeBoosterEnabledKey, false)
    var volumeBoosterGain by rememberPreference(volumeBoosterGainKey, 0)
    var isFloatingLyricsEnabled by rememberPreference(com.github.niusic.utils.isFloatingLyricsEnabledKey, false)
    var lyricsFontSize by rememberPreference(lyricsFontSizeKey, 20)
    var floatingLyricsFontSize by rememberPreference(floatingLyricsFontSizeKey, 20)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    var persistentQueue by rememberPreference(persistentQueueKey, false)
    var musicStylePreset by rememberPreference(musicStylePresetKey, MusicStylePreset.None)

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.player),
        onBackClick = onBackClick,
        content = {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {

                SwitchSetting(
                    icon = IconSource.Vector(Icons.AutoMirrored.Outlined.QueueMusic),
                    title = stringResource(id = R.string.persistent_queue),
                    description = stringResource(id = R.string.persistent_queue_description),
                    switchState = persistentQueue,
                    onSwitchChange = {
                        persistentQueue = it
                    }
                )

                SwitchSetting(
                    icon = IconSource.Vector(Icons.Default.MusicOff),
                    title = stringResource(id = R.string.skip_silence),
                    description = stringResource(id = R.string.skip_silence_description),
                    switchState = skipSilence,
                    onSwitchChange = {
                        skipSilence = it
                    }
                )
                SwitchSetting(
                    icon = IconSource.Icon(painterResource(R.drawable.audio_focus)),
                    title = "Audio Focus",
                    description = "Pause playback when other media is playing",
                    switchState = audioFocus,
                    onSwitchChange = { audioFocus = it }
                )
                SwitchSetting(
                    icon = IconSource.Vector(Icons.Default.Pause),
                    title = "Auto Pause",
                    description = "Pause playback when volume is muted",
                    switchState = autoPauseEnabled,
                    onSwitchChange = { autoPauseEnabled = it }
                )

                if (isAtLeastAndroid6) {
                    SwitchSetting(
                        icon = IconSource.Icon(painterResource(R.drawable.headphone)),
                        title = stringResource(id = R.string.resume_playback),
                        description = stringResource(id = R.string.resume_playback_description),
                        switchState = resumePlaybackWhenDeviceConnected,
                        onSwitchChange = {
                            resumePlaybackWhenDeviceConnected = it
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SwitchSetting(
                    icon = IconSource.Icon(painterResource(R.drawable.mic)),
                    title = "Floating Lyrics",
                    description = "Show synchronized lyrics at the bottom of album art",
                    switchState = isFloatingLyricsEnabled,
                    onSwitchChange = { isFloatingLyricsEnabled = it }
                )

                SliderSettingsItem(
                    label = stringResource(id = R.string.lyrics_font_size),
                    value = lyricsFontSize.toFloat(),
                    onValueChange = { lyricsFontSize = it.toInt() },
                    valueRange = 12f..40f,
                    valueLabel = { "${it.toInt()} sp" },
                    hapticUseIntegerStep = true
                )

                if (isFloatingLyricsEnabled) {
                    SliderSettingsItem(
                        label = stringResource(id = R.string.floating_lyrics_font_size),
                        value = floatingLyricsFontSize.toFloat(),
                        onValueChange = { floatingLyricsFontSize = it.toInt() },
                        valueRange = 12f..40f,
                        valueLabel = { "${it.toInt()} sp" },
                        hapticUseIntegerStep = true
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.lyrics_preview),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorPalette.text.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(colorPalette.background2)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.sample_lyrics),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = lyricsFontSize.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colorPalette.text,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Audio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SliderSettingsItem(
                    label = "Play speed",
                    value = playSpeed,
                    onValueChange = { playSpeed = it },
                    valueRange = 0.5f..2.0f,
                    valueLabel = { String.format(Locale.US, "%.1fx", it) },
                    hapticUseIntegerStep = false,
                    hapticUseFloatStep = true,
                    hapticFloatStep = 0.1f
                )


                SliderSettingsItem(
                    label = "Crossfade between tracks",
                    value = crossfade,
                    onValueChange = { crossfade = it },
                    valueRange = 0f..10f,
                    valueLabel = {
                        val seconds = it.toInt()
                        if (seconds == 0) "Off" else "$seconds seconds"
                    },
                    hapticStep = 1f,
                    hapticUseIntegerStep = true
                )

                SwitchSetting(
                    icon = IconSource.Vector(Icons.AutoMirrored.Filled.VolumeUp),
                    title = stringResource(id = R.string.loudness_normalization),
                    description = stringResource(id = R.string.loudness_normalization_description),
                    switchState = volumeNormalization,
                    onSwitchChange = {
                        volumeNormalization = it
                    }
                )

                SwitchSetting(
                    icon = IconSource.Vector(Icons.AutoMirrored.Filled.VolumeUp),
                    title = stringResource(id = R.string.volume_booster),
                    description = stringResource(id = R.string.volume_booster_description),
                    switchState = volumeBoosterEnabled,
                    onSwitchChange = {
                        volumeBoosterEnabled = it
                    }
                )

                if (volumeBoosterEnabled) {
                    SliderSettingsItem(
                        label = "Boost Level",
                        value = volumeBoosterGain.toFloat(),
                        onValueChange = { volumeBoosterGain = it.toInt() },
                        valueRange = 0f..2000f,
                        valueLabel = { "${100 + (it / 20).toInt()}%" },
                        hapticUseIntegerStep = true
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = colorPalette.text.copy(alpha = 0.1f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.music_style),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorPalette.text
                    )
                    Text(
                        text = stringResource(id = R.string.music_style_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorPalette.text.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicStylePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = musicStylePreset == preset,
                                onClick = { musicStylePreset = preset },
                                label = {
                                    Text(
                                        text = when (preset) {
                                            MusicStylePreset.None -> stringResource(id = R.string.none)
                                            MusicStylePreset.VocalBoost -> stringResource(id = R.string.vocal_boost)
                                            MusicStylePreset.MusicBoost -> stringResource(id = R.string.music_boost)
                                            MusicStylePreset.DolbyAtmos -> stringResource(id = R.string.dolby_atmos)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    )
}
