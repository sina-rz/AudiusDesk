package com.audiusdesk.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.audiusdesk.AudiusDeskApp
import com.audiusdesk.ui.components.BottomPlayerBar
import com.audiusdesk.ui.components.Sidebar
import com.audiusdesk.ui.components.TopBar
import com.audiusdesk.ui.screens.SearchScreen
import com.audiusdesk.ui.screens.TrendingScreen
import com.audiusdesk.ui.viewmodel.AppSection
import kotlinx.coroutines.launch


@Composable
fun AppShell(app: AudiusDeskApp) {
    val mainViewModel = app.mainViewModel
    val playerViewModel = app.playerViewModel

    val section by mainViewModel.section.collectAsState()
    val query by mainViewModel.searchQuery.collectAsState()
    val trending by mainViewModel.trending.collectAsState()
    val search by mainViewModel.search.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val queueState by playerViewModel.queueState.collectAsState()
    val activeHost by app.apiClient.activeHost.collectAsState()


    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        launch { mainViewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
        launch { playerViewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(
                    selected = section,
                    onSelect = mainViewModel::selectSection,
                    activeHost = activeHost,
                )

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TopBar(
                        section = section,
                        query = query,
                        onQueryChange = mainViewModel::onQueryChange,
                        onRefresh = mainViewModel::refreshTrending,
                    )

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (section) {
                            AppSection.EXPLORE -> TrendingScreen(
                                state = trending,
                                playerState = playerState,
                                onRefresh = mainViewModel::refreshTrending,
                                onPlayAt = { index ->
                                    playerViewModel.playOrToggle(trending.tracks, index)
                                },
                            )

                            AppSection.SEARCH -> SearchScreen(
                                state = search,
                                query = query,
                                playerState = playerState,
                                onRetry = { mainViewModel.onQueryChange(query) },
                                onPlayAt = { index ->
                                    playerViewModel.playOrToggle(search.results, index)
                                },
                            )
                        }
                    }

                    BottomPlayerBar(
                        state = playerState,
                        queue = queueState,
                        onTogglePlay = playerViewModel::togglePlayPause,
                        onNext = playerViewModel::next,
                        onPrevious = playerViewModel::previous,
                        onSeek = playerViewModel::seekTo,
                        onVolumeChange = playerViewModel::setVolume,
                        onToggleMute = playerViewModel::toggleMute,
                        onToggleShuffle = playerViewModel::toggleShuffle,
                        onCycleRepeat = playerViewModel::cycleRepeatMode,
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 108.dp),
            )
        }
    }
}
