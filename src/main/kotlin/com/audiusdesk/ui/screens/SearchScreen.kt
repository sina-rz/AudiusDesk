package com.audiusdesk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.audiusdesk.core.audio.PlayerState
import com.audiusdesk.ui.components.EmptyPane
import com.audiusdesk.ui.components.EmptyPanes
import com.audiusdesk.ui.components.ErrorPane
import com.audiusdesk.ui.components.LoadingPane
import com.audiusdesk.ui.components.TrackCard
import com.audiusdesk.ui.viewmodel.SearchState


@Composable
fun SearchScreen(
    state: SearchState,
    query: String,
    playerState: PlayerState,
    onRetry: () -> Unit,
    onPlayAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        query.isBlank() -> EmptyPane(EmptyPanes.SearchHint, modifier)

        state.results.isEmpty() && state.isLoading ->
            LoadingPane("Searching for “$query”…", modifier)

        state.results.isEmpty() && state.error != null ->
            ErrorPane(message = state.error, onRetry = onRetry, modifier = modifier)

        state.results.isEmpty() && state.hasSearched ->
            EmptyPane(EmptyPanes.NoResults, modifier)

        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 196.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GridHeader(
                    title = "Results for “$query”",
                    subtitle = "${state.results.size} streamable tracks on Audius",
                )
            }
            itemsIndexed(
                items = state.results,
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
