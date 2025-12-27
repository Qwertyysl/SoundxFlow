package com.github.niusic.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.niusic.azan.AzanZone
import com.github.niusic.azan.AzanWorker
import com.github.niusic.azan.azanZones
import com.github.niusic.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzanSettings(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var azanEnabled by rememberPreference(azanReminderEnabledKey, false)
    var selectedZone by rememberPreference(azanLocationKey, "WLY01")
    var azanAudioPath by rememberPreference(azanAudioPathKey, "")
    var azanQuietMode by rememberPreference(azanQuietModeKey, false)

    var showZoneDialog by remember { mutableStateOf(false) }
    
    val canScheduleExactAlarms = context.canScheduleExactAlarms
    val isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations

    LaunchedEffect(azanEnabled, selectedZone) {
        if (azanEnabled) {
            AzanWorker.runOnce(context)
            AzanWorker.enqueue(context)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                azanAudioPath = it.toString()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Azan Reminder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingColum(
                title = "Enable Azan Reminder",
                description = "Pause music and play Azan during prayer times",
                trailingContent = {
                    Switch(checked = azanEnabled, onCheckedChange = { azanEnabled = it })
                },
                onClick = { azanEnabled = !azanEnabled }
            )

            SettingColum(
                title = "Location (Zone)",
                description = azanZones.find { it.code == selectedZone }?.name ?: selectedZone,
                onClick = { showZoneDialog = true }
            )

            SettingColum(
                title = "Simple Quiet Mode",
                description = "Pause music for 5 minutes instead of playing Azan audio",
                trailingContent = {
                    Switch(checked = azanQuietMode, onCheckedChange = { azanQuietMode = it })
                },
                onClick = { azanQuietMode = !azanQuietMode }
            )

            SettingColum(
                title = "Azan Audio",
                description = if (azanAudioPath.isEmpty()) "Default (azantv3)" else "Selected: ${Uri.parse(azanAudioPath).lastPathSegment}",
                onClick = {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                }
            )

            if (azanEnabled) {
                if (!canScheduleExactAlarms) {
                    InfoCard(
                        title = "Exact Alarm Permission Required",
                        description = "To trigger Azan accurately, the app needs permission to schedule exact alarms.",
                        buttonText = "Grant Permission",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                })
                            }
                        }
                    )
                }

                if (!isIgnoringBatteryOptimizations) {
                    InfoCard(
                        title = "Battery Optimization",
                        description = "The system may kill the background worker. Disable battery optimization for better reliability.",
                        buttonText = "Disable",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                })
                            }
                        }
                    )
                }
            }
        }
    }

    if (showZoneDialog) {
        AlertDialog(
            onDismissRequest = { showZoneDialog = false },
            title = { Text("Select Zone") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(azanZones) { zone ->
                        Text(
                            text = "${zone.code} - ${zone.name}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedZone = zone.code
                                    showZoneDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showZoneDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(buttonText)
            }
        }
    }
}
