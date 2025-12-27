package com.github.niusic

import com.github.niusic.service.PlayerBinder

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.niusic.enums.AppThemeColor
import com.github.niusic.service.PlayerService
import com.github.niusic.ui.navigation.SettingsDestinations
import com.github.niusic.ui.screens.settings.*
import com.github.niusic.ui.styling.AppTheme
import com.github.niusic.utils.appTheme
import com.github.niusic.utils.rememberPreference

class SettingsActivity : ComponentActivity() {

    private var binder by mutableStateOf<PlayerBinder?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerBinder) {
                this@SettingsActivity.binder = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, PlayerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val screenId = intent.getStringExtra("SCREEN_ID") ?: SettingsDestinations.MAIN

        setContent {
            val appThemePreference by rememberPreference(appTheme, AppThemeColor.System)

            val darkTheme = when (appThemePreference) {
                AppThemeColor.System -> isSystemInDarkTheme()
                AppThemeColor.Dark -> true
                AppThemeColor.Light -> false
                else -> true
            }

            AppTheme(
                appThemeColor = appThemePreference,
                darkTheme = darkTheme,
                usePureBlack = false,
                useMaterialNeutral = false
            ) {
                CompositionLocalProvider(value = LocalPlayerServiceBinder provides binder) {
                    when (screenId) {
                        SettingsDestinations.MAIN -> {
                            SettingsScreen(
                                onBackClick = { finish() },
                                onOptionClick = { routeId -> launchSubScreen(routeId) }
                            )
                        }

                        SettingsDestinations.APPEARANCE -> AppearanceSettings(
                            onBackClick = { finish() },
                            onBackgroundClick = { launchSubScreen(SettingsDestinations.BACKGROUND) }
                        )

                        SettingsDestinations.BACKGROUND -> BackgroundSettings(onBackClick = { finish() })
                        SettingsDestinations.PLAYER -> PlayerSettings(onBackClick = { finish() })
                        SettingsDestinations.PRIVACY -> PrivacySettings(onBackClick = { finish() })
                        SettingsDestinations.BACKUP -> BackupSettings(onBackClick = { finish() })
                        SettingsDestinations.DATABASE -> CacheSettings(onBackClick = { finish() })
                        SettingsDestinations.MORE -> MoreSettings(onBackClick = { finish() })
                        SettingsDestinations.EXPERIMENT -> ExperimentSettings(onBackClick = { finish() })
                        SettingsDestinations.AZAN -> AzanSettings(onBack = { finish() })
                        SettingsDestinations.ABOUT -> AboutSettings(onBackClick = { finish() })
                    }
                }
            }
        }
    }

    private fun launchSubScreen(screenId: String) {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            putExtra("SCREEN_ID", screenId)
        }
        startActivity(intent)
    }
}

