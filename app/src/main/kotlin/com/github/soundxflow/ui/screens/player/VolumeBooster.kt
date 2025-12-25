package com.github.soundxflow.ui.screens.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundxflow.R
import com.github.soundxflow.ui.common.IconSource
import com.github.soundxflow.ui.components.SliderSettingsItem
import com.github.soundxflow.ui.components.SwitchSetting
import com.github.soundxflow.utils.rememberPreference
import com.github.soundxflow.utils.volumeBoosterEnabledKey
import com.github.soundxflow.utils.volumeBoosterGainKey

@Composable
fun VolumeBooster(
    onDismiss: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var volumeBoosterEnabled by rememberPreference(volumeBoosterEnabledKey, false)
    var volumeBoosterGain by rememberPreference(volumeBoosterGainKey, 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.done),
                    color = colorPalette.accent
                )
            }
        },
        title = {
            Text(
                text = stringResource(id = R.string.volume_booster),
                color = colorPalette.text
            )
        },
        text = {
            Column {
                SwitchSetting(
                    title = "Enabled",
                    description = "Turn on volume boosting",
                    icon = IconSource.Vector(Icons.AutoMirrored.Filled.VolumeUp),
                    switchState = volumeBoosterEnabled,
                    onSwitchChange = { volumeBoosterEnabled = it }
                )

                if (volumeBoosterEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    SliderSettingsItem(
                        label = "Boost Level",
                        value = volumeBoosterGain.toFloat(),
                        onValueChange = { volumeBoosterGain = it.toInt() },
                        valueRange = 0f..2000f,
                        valueLabel = { "${100 + (it / 20).toInt()}%" },
                        hapticUseIntegerStep = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.volume_booster_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorPalette.text.copy(alpha = 0.6f)
                )
            }
        },
        containerColor = colorPalette.background3
    )
}
