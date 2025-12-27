package com.github.niusic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp // Import Dp
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.core.ui.DesignStyle
import com.github.niusic.ui.modifier.glassEffect
import androidx.compose.foundation.layout.Box

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: String,
    onBackClick: () -> Unit,
    scrollable: Boolean = true,
    horizontalPadding: Dp = 14.dp, // <--- 1. Add Default Parameter
    content: @Composable () -> Unit
) {
    val appearance = LocalAppearance.current
    val (colorPalette) = appearance
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = if (isGlassTheme) Modifier.glassEffect(shape = RoundedCornerShape(0.dp), alpha = 0.1f) else Modifier,
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp),
                            tint = colorPalette.text
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colorPalette.text
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding) // <--- 2. Use the variable
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
            ) {
                content()
            }
        }
    }
}
