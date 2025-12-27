package com.github.niusic.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.github.niusic.R
import com.github.core.ui.DesignStyle
import com.github.core.ui.LocalAppearance
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import com.github.niusic.ui.modifier.glassEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    @StringRes title: Int,
    snackbarHost: @Composable (() -> Unit) = {},
    floatingActionButton: @Composable (() -> Unit) = {},
    openSearch: () -> Unit,
    openSettings: () -> Unit,
    content: @Composable (() -> Unit)
) {
    val appearance = LocalAppearance.current

    if (appearance.designStyle == DesignStyle.Modern || appearance.designStyle == DesignStyle.Glass) {
        ModernHomeScaffold(
            title = title,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            openSearch = openSearch,
            openSettings = openSettings,
            content = content
        )
    } else {
        ClassicHomeScaffold(
            title = title,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            openSearch = openSearch,
            openSettings = openSettings,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicHomeScaffold(
    @StringRes title: Int,
    snackbarHost: @Composable (() -> Unit) = {},
    floatingActionButton: @Composable (() -> Unit) = {},
    openSearch: () -> Unit,
    openSettings: () -> Unit,
    content: @Composable (() -> Unit)
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = title))
                },
                actions = {
                    TooltipIconButton(
                        description = R.string.search,
                        onClick = openSearch,
                        icon = Icons.Outlined.Search,
                        inTopBar = true
                    )

                    TooltipIconButton(
                        description = R.string.settings,
                        onClick = openSettings,
                        icon = Icons.Outlined.Settings,
                        inTopBar = true
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = paddingValues),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHomeScaffold(
    @StringRes title: Int,
    snackbarHost: @Composable (() -> Unit) = {},
    floatingActionButton: @Composable (() -> Unit) = {},
    openSearch: () -> Unit,
    openSettings: () -> Unit,
    content: @Composable (() -> Unit)
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val appearance = LocalAppearance.current
    val (colorPalette) = appearance
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                modifier = if (isGlassTheme) Modifier.glassEffect(shape = RoundedCornerShape(0.dp), alpha = 0.1f) else Modifier,
                title = {
                    Text(
                        text = stringResource(id = title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    TooltipIconButton(
                        description = R.string.search,
                        onClick = openSearch,
                        icon = Icons.Outlined.Search,
                        inTopBar = true
                    )

                    TooltipIconButton(
                        description = R.string.settings,
                        onClick = openSettings,
                        icon = Icons.Outlined.Settings,
                        inTopBar = true
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = if (isGlassTheme) Color.Transparent else colorPalette.background0.copy(alpha = 0.95f)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content()
        }
    }
}

