@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.soundxflow.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundxflow.R
import com.github.soundxflow.ui.common.IconSource
import com.github.soundxflow.ui.components.SettingsCard
import com.github.soundxflow.ui.components.SettingsScreenLayout
import com.github.soundxflow.viewmodels.SettingsViewModel
import com.github.core.ui.LocalAppearance
import com.github.core.ui.DesignStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onOptionClick: (String) -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val appearance = LocalAppearance.current
    val (colorPalette) = appearance

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.settings),
        onBackClick = onBackClick,
        content = {
            if (appearance.designStyle == DesignStyle.Modern) {
                // MODERN SETTINGS
                Spacer(modifier = Modifier.height(16.dp))
                
                sections.forEach { section ->
                    Text(
                        text = section.title?.let { stringResource(id = it) } ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorPalette.textSecondary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorPalette.surface)
                    ) {
                        section.options.forEachIndexed { index, option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOptionClick(option.screenId) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (option.icon != null) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = colorPalette.accent
                                    )
                                } else {
                                    option.iconRes?.let { iconResId ->
                                        Icon(
                                            painter = painterResource(iconResId),
                                            contentDescription = null,
                                            tint = colorPalette.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = stringResource(id = option.title),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorPalette.text,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colorPalette.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            if (index < section.options.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = colorPalette.textDisabled.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                // CLASSIC SETTINGS
                Spacer(modifier = Modifier.height(8.dp))

                sections.forEach { section ->
                    SettingsCard {
                        section.options.forEach { option ->
                            if (option.icon != null) {
                                SettingRow(
                                    title = stringResource(id = option.title),
                                    icon = IconSource.Vector(option.icon),
                                    onClick = { onOptionClick(option.screenId) }
                                )
                            } else {
                                option.iconRes?.let { iconResId ->
                                    SettingRow(
                                        title = stringResource(id = option.title),
                                        icon = IconSource.Icon(painterResource(iconResId)),
                                        onClick = { onOptionClick(option.screenId) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    )
}
