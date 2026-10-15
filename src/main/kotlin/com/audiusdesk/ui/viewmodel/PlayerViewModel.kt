package com.audiusdesk.ui.viewmodel

import com.audiusdesk.core.audio.AudioPlayerManager
import com.audiusdesk.core.audio.PlaybackStatus
import com.audiusdesk.core.audio.PlayerState
import com.audiusdesk.data.model.Track
import com.audiusdesk.data.repository.AudiusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.util.concurrent.CancellationException

enum class RepeatMode { OFF, ALL, ONE }

data class QueueState(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val shuffle: Boolean = false,
) {
    val currentTrack: Track? get() = tracks.getOrNull(currentIndex)
    val hasPrevious: Boolean get() = currentIndex > 0
    val hasNext: Boolean
        get() = tracks.isNotEmpty() &&
                (repeatMode == RepeatMode.ALL || shuffle || currentIndex < tracks.lastIndex)
}


class PlayerViewModel(
    private val repository: AudiusRepository,
    private val audio: AudioPlayerManager,
) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    val playerState: StateFlow<PlayerState> = audio.state

    val messages: SharedFlow<String> = audio.errors

    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private var loadJob: Job? = null

    private val streamUrlCache = object : LinkedHashMap<String, String>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 64
    }

    init {
        audio.onTrackEnded = { scope.launch { advanceAfterTrackEnd() } }
    }


    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (startIndex !in tracks.indices) return
        val state = _queueState.value
        val sameQueue = state.tracks.size == tracks.size &&
                state.tracks.zip(tracks).all { (a, b) -> a.id == b.id }

        _queueState.value = if (sameQueue) state.copy(currentIndex = startIndex)
        else state.copy(tracks = tracks, currentIndex = startIndex)

        startCurrent()
    }

    fun playOrToggle(tracks: List<Track>, index: Int) {
        val tapped = tracks.getOrNull(index) ?: return
        if (audio.state.value.currentTrack?.id == tapped.id) togglePlayPause()
        else playQueue(tracks, index)
    }


    fun togglePlayPause() {
        val s = audio.state.value
        when {
            s.isPlaying -> audio.pause()
            s.status == PlaybackStatus.PAUSED -> audio.resume()
            s.currentTrack != null -> startCurrent()
            _queueState.value.currentTrack != null -> startCurrent()
        }
    }

    fun next() {
        val q = _queueState.value
        val next = nextIndex(q) ?: return
        _queueState.update { it.copy(currentIndex = next) }
        startCurrent()
    }

    fun previous() {
        if (audio.state.value.currentPositionMs > 3_000) {
            audio.seekTo(0)
            return
        }
        val q = _queueState.value
        if (!q.hasPrevious) return
        _queueState.update { it.copy(currentIndex = q.currentIndex - 1) }
        startCurrent()
    }

    fun seekTo(positionMs: Long) = audio.seekTo(positionMs)

    fun setVolume(volume: Float) = audio.setVolume(volume)

    fun toggleMute() = audio.setMuted(!audio.state.value.isMuted)

    fun toggleShuffle() = _queueState.update { it.copy(shuffle = !it.shuffle) }

    fun cycleRepeatMode() = _queueState.update {
        it.copy(
            repeatMode = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            },
        )
    }


    private fun nextIndex(q: QueueState): Int? = when {
        q.tracks.isEmpty() -> null
        q.shuffle -> q.tracks.indices.filter { it != q.currentIndex }.randomOrNull()
        q.currentIndex < q.tracks.lastIndex -> q.currentIndex + 1
        q.repeatMode == RepeatMode.ALL -> 0
        else -> null
    }

    private fun advanceAfterTrackEnd() {
        val q = _queueState.value
        if (q.repeatMode == RepeatMode.ONE) {
            startCurrent()
            return
        }
        val next = nextIndex(q) ?: return
        _queueState.update { it.copy(currentIndex = next) }
        startCurrent()
    }

    private fun startCurrent() {
        val track = _queueState.value.currentTrack ?: return
        loadJob?.cancel()
        loadJob = scope.launch {
            audio.markBuffering(track)
            val cached = streamUrlCache[track.id]
            try {
                val url = cached ?: repository.resolveStreamUrl(track)
                    .also { streamUrlCache[track.id] = it }
                audio.playStream(track, url)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                audio.reportTrackError("Could not resolve a stream for “${track.title}”")
            }
        }
    }

    override fun close() {
        audio.onTrackEnded = null
        scope.cancel()
    }
}
