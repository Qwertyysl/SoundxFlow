@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.github.niusic.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.niusic.ui.components.HorizontalTabs
import com.github.niusic.ui.components.TopBar
import com.github.niusic.ui.navigation.Routes
import com.github.niusic.azan.AzanWidget
import com.github.core.ui.DesignStyle
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import com.github.niusic.ui.modifier.glassEffect
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun HomeScreen(
    navController: NavController,
    onSettingsClick: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val appearance = LocalAppearance.current
    val (colorPalette) = appearance
    val isGlassTheme = appearance.designStyle == DesignStyle.Glass
    val isDark = appearance.colorPalette.isDark
    
    val navigateToAlbum = { browseId: String ->
        navController.navigate(route = Routes.Album(id = browseId))
    }
    val navigateToArtist = { browseId: String ->
        navController.navigate(route = Routes.Artist(id = browseId))
    }

    val localAppearance = if (isGlassTheme) {
        val textColor = if (isDark) Color.White else Color.Black
        val secondaryTextColor = if (isDark) Color.LightGray else Color.DarkGray
        appearance.copy(
            colorPalette = appearance.colorPalette.copy(
                text = textColor,
                textSecondary = secondaryTextColor,
                iconColor = textColor
            )
        )
    } else appearance

    CompositionLocalProvider(LocalAppearance provides localAppearance) {
        val localColorScheme = if (isGlassTheme) {
            val onColor = if (isDark) Color.White else Color.Black
            MaterialTheme.colorScheme.copy(
                primary = colorPalette.accent,
                onBackground = onColor,
                onSurface = onColor,
                onSurfaceVariant = onColor.copy(alpha = 0.7f),
                surfaceVariant = onColor.copy(alpha = 0.1f)
            )
        } else MaterialTheme.colorScheme

        MaterialTheme(colorScheme = localColorScheme) {
            if (appearance.designStyle == DesignStyle.Modern || isGlassTheme) {
                // MODERN / GLASS LAYOUT
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isGlassTheme) (if (isDark) Color.Black else Color.White) else colorPalette.background0)
                ) {
                    if (isGlassTheme) {
                        // High-performance Liquid background simulation using light gradients
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colorPalette.accent.copy(alpha = if (isDark) 0.2f else 0.15f),
                                            if (isDark) Color.Black else Color.White
                                        ),
                                        center = Offset(x = 1000f, y = 0f),
                                        radius = 1500f
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colorPalette.accent.copy(alpha = if (isDark) 0.15f else 0.1f),
                                            Color.Transparent
                                        ),
                                        center = Offset(x = 0f, y = 1000f),
                                        radius = 1200f
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Home",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Black,
                                    shadow = if (isGlassTheme) Shadow(
                                        color = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.2f),
                                        offset = Offset(1f, 1f),
                                        blurRadius = 2f
                                    ) else null
                                ),
                                color = if (isGlassTheme) (if (isDark) Color.White else Color.Black) else colorPalette.text
                            )
                            Row(
                                modifier = if (isGlassTheme) {
                                    Modifier.glassEffect(
                                        shape = RoundedCornerShape(20.dp), 
                                        alpha = 0.05f,
                                        borderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)
                                    )
                                        .padding(horizontal = 4.dp)
                                } else Modifier
                            ) {
                                IconButton(onClick = { navController.navigate(route = Routes.Search) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                        tint = if (isGlassTheme) (if (isDark) Color.White else Color.Black) else colorPalette.text
                                    )
                                }
                                IconButton(onClick = onSettingsClick) {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        tint = if (isGlassTheme) (if (isDark) Color.White else Color.Black) else colorPalette.text
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .then(
                                    if (isGlassTheme) Modifier.glassEffect(
                                        shape = RoundedCornerShape(16.dp), 
                                        alpha = 0.05f,
                                        borderColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)
                                    )
                                        .padding(8.dp)
                                    else Modifier
                                )
                        ) {
                            AzanWidget()
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .then(
                                    if (isGlassTheme) Modifier.glassEffect(
                                        shape = RoundedCornerShape(12.dp), 
                                        alpha = 0.03f,
                                        borderColor = Color.Black.copy(alpha = 0.05f)
                                    )
                                    else Modifier
                                )
                        ) {
                            HorizontalTabs(pagerState = pagerState)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (page) {
                                    0 -> QuickPicks(
                                        onAlbumClick = navigateToAlbum,
                                        onArtistClick = navigateToArtist,
                                        onPlaylistClick = { browseId -> navController.navigate(route = Routes.Playlist(id = browseId)) },
                                        onOfflinePlaylistClick = { navController.navigate(route = Routes.BuiltInPlaylist(index = 1)) }
                                    )
                                    1 -> HomeSongs(onGoToAlbum = navigateToAlbum, onGoToArtist = navigateToArtist)
                                    2 -> HomeArtistList(onArtistClick = { artist -> navigateToArtist(artist.id) })
                                    3 -> HomeAlbums(onAlbumClick = { album -> navigateToAlbum(album.id) })
                                    4 -> HomePlaylists(
                                        onBuiltInPlaylist = { playlistIndex -> navController.navigate(route = Routes.BuiltInPlaylist(index = playlistIndex)) },
                                        onPlaylistClick = { playlist -> navController.navigate(route = Routes.LocalPlaylist(id = playlist.id)) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // CLASSIC LAYOUT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    TopBar(
                        onSearch = { navController.navigate(route = Routes.Search) },
                        onSettingsClick = onSettingsClick,
                    )

                    AzanWidget()

                    Spacer(modifier = Modifier.padding(vertical = 2.dp))

                    HorizontalTabs(pagerState = pagerState)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                            .background(color = colorPalette.baseColor)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> QuickPicks(
                                    onAlbumClick = navigateToAlbum,
                                    onArtistClick = navigateToArtist,
                                    onPlaylistClick = { browseId ->
                                        navController.navigate(route = Routes.Playlist(id = browseId))
                                    },
                                    onOfflinePlaylistClick = {
                                        navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
                                    }
                                )

                                1 -> HomeSongs(
                                    onGoToAlbum = navigateToAlbum,
                                    onGoToArtist = navigateToArtist
                                )

                                2 -> HomeArtistList(
                                    onArtistClick = { artist -> navigateToArtist(artist.id) }
                                )

                                3 -> HomeAlbums(
                                    onAlbumClick = { album -> navigateToAlbum(album.id) }
                                )

                                4 -> HomePlaylists(
                                    onBuiltInPlaylist = { playlistIndex ->
                                        navController.navigate(route = Routes.BuiltInPlaylist(index = playlistIndex))
                                    },
                                    onPlaylistClick = { playlist ->
                                        navController.navigate(route = Routes.LocalPlaylist(id = playlist.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

