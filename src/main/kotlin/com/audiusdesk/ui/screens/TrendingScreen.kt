package com.audiusdesk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.audiusdesk.core.audio.PlayerState
import com.audiusdesk.ui.components.ErrorPane
import com.audiusdesk.ui.components.LoadingPane
import com.audiusdesk.ui.components.TrackCard
import com.audiusdesk.ui.viewmodel.TrackListState


@Composable
fun TrendingScreen(
    state: TrackListState,
    playerState: PlayerState,
    onRefresh: () -> Unit,
    onPlayAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.tracks.isEmpty() && state.isLoading ->
            LoadingPane("Loading the trending chart…", modifier)

        state.tracks.isEmpty() && state.error != null ->
            ErrorPane(message = state.error, onRetry = onRefresh, modifier = modifier)

        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 196.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GridHeader(
                    title = "Trending this week",
                    subtitle = "The most played tracks across the Audius network · ${state.tracks.size} tracks",
                )
            }
            itemsIndexed(
                items = state.tracks,
                key = { _, track -> track.id },
                contentType = { _, _ -> "track" },
            ) { index, track ->
                TrackCard(
                    track = track,
                    isCurrent = playerState.currentTrack?.id == track.id,
                    isPlaying = playerState.isPlaying,
                    onClick = { onPlayAt(index) },
                )
            }
        }
    }
}

@Composable
fun GridHeader(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
