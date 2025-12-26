@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.github.soundxflow.ui.screens.home

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.soundxflow.ui.components.HorizontalTabs
import com.github.soundxflow.ui.components.TopBar
import com.github.soundxflow.ui.navigation.Routes
import com.github.soundxflow.azan.AzanWidget
import com.github.core.ui.DesignStyle
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun HomeScreen(
    navController: NavController,
    onSettingsClick: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val appearance = LocalAppearance.current
    val (colorPalette) = appearance
    
    val navigateToAlbum = { browseId: String ->
        navController.navigate(route = Routes.Album(id = browseId))
    }
    val navigateToArtist = { browseId: String ->
        navController.navigate(route = Routes.Artist(id = browseId))
    }

    if (appearance.designStyle == DesignStyle.Modern) {
        // MODERN LAYOUT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette.background0)
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = colorPalette.text
                )
                Row {
                    IconButton(onClick = { navController.navigate(route = Routes.Search) }) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = colorPalette.text)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = colorPalette.text)
                    }
                }
            }

            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                AzanWidget()
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalTabs(pagerState = pagerState)
            
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
