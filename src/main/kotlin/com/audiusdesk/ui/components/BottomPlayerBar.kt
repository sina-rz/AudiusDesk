package com.audiusdesk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.audiusdesk.core.audio.PlayerState
import com.audiusdesk.ui.util.formatDurationMs
import com.audiusdesk.ui.viewmodel.QueueState
import com.audiusdesk.ui.viewmodel.RepeatMode
import kotlin.math.roundToInt


@Composable
fun BottomPlayerBar(
    state: PlayerState,
    queue: QueueState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .padding(horizontal = 20.dp),
            ) {
                NowPlaying(
                    state = state,
                    modifier = Modifier.width(290.dp),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                ) {
                    TransportControls(
                        state = state,
                        queue = queue,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                    )
                    SeekControls(state = state, onSeek = onSeek)
                }

                VolumeControls(
                    state = state,
                    onVolumeChange = onVolumeChange,
                    onToggleMute = onToggleMute,
                    modifier = Modifier.width(200.dp),
                )
            }
        }
    }
}


@Composable
private fun NowPlaying(state: PlayerState, modifier: Modifier = Modifier) {
    val track = state.currentTrack
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        ArtworkImage(
            url = track?.artworkSmall,
            contentDescription = track?.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholderIcon = Icons.Filled.MusicNote,
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                text = track?.title ?: "Nothing playing yet",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track?.artistName ?: "Pick a track to start listening",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Composable
private fun TransportControls(
    state: PlayerState,
    queue: QueueState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val activeTint = MaterialTheme.colorScheme.primary
    val idleTint = MaterialTheme.colorScheme.onSurfaceVariant

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleShuffle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle",
                tint = if (queue.shuffle) activeTint else idleTint,
                modifier = Modifier.size(20.dp),
            )
        }

        IconButton(
            onClick = onPrevious,
            enabled = queue.hasPrevious || state.currentPositionMs > 3_000,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = if (queue.hasPrevious || state.currentPositionMs > 3_000)
                    MaterialTheme.colorScheme.onSurface else idleTint.copy(alpha = 0.4f),
                modifier = Modifier.size(26.dp),
            )
        }

        val playEnabled = state.currentTrack != null || queue.currentTrack != null
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (playEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
                .clickable(enabled = playEnabled, onClick = onTogglePlay),
        ) {
            if (state.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = if (playEnabled) MaterialTheme.colorScheme.onPrimary else idleTint.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        IconButton(onClick = onNext, enabled = queue.hasNext, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = if (queue.hasNext) MaterialTheme.colorScheme.onSurface
                else idleTint.copy(alpha = 0.4f),
                modifier = Modifier.size(26.dp),
            )
        }

        IconButton(onClick = onCycleRepeat, modifier = Modifier.size(36.dp)) {
            val (icon, tint) = when (queue.repeatMode) {
                RepeatMode.OFF -> Icons.Filled.Repeat to idleTint
                RepeatMode.ALL -> Icons.Filled.Repeat to activeTint
                RepeatMode.ONE -> Icons.Filled.RepeatOne to activeTint
            }
            Icon(
                imageVector = icon,
                contentDescription = "Repeat: ${queue.repeatMode.name.lowercase()}",
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}


@Composable
private fun SeekControls(state: PlayerState, onSeek: (Long) -> Unit) {
    val durationMs = state.durationMs.coerceAtLeast(0L)
    val rangeEnd = durationMs.toFloat().coerceAtLeast(1f)

    // Local drag position — decouples the thumb from 250 ms engine ticks while dragging.
    var dragPositionMs by remember(state.currentTrack?.id) { mutableStateOf<Float?>(null) }

    val shownPositionMs = dragPositionMs
        ?: state.currentPositionMs.toFloat().coerceIn(0f, rangeEnd)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = formatDurationMs(shownPositionMs.toLong()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        Slider(
            value = shownPositionMs.coerceIn(0f, rangeEnd),
            onValueChange = { dragPositionMs = it },
            onValueChangeFinished = {
                dragPositionMs?.let { onSeek(it.toLong()) }
                dragPositionMs = null
            },
            valueRange = 0f..rangeEnd,
            enabled = state.currentTrack != null && durationMs > 0,
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
        )
        Text(
            text = formatDurationMs(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
    }
}


@Composable
private fun VolumeControls(
    state: PlayerState,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val volumeIcon = when {
        state.isMuted || state.volume <= 0f -> Icons.Filled.VolumeOff
        state.volume < 0.5f -> Icons.Filled.VolumeDown
        else -> Icons.Filled.VolumeUp
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier,
    ) {
        IconButton(onClick = onToggleMute, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = volumeIcon,
                contentDescription = if (state.isMuted) "Unmute" else "Mute",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = state.volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .width(110.dp)
                .height(28.dp),
        )
        Text(
            text = "${(state.volume * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .width(34.dp),
        )
    }
}
