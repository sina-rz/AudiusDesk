package com.audiusdesk.core.audio

import com.audiusdesk.data.model.Track
import io.github.sinaraz.JudioPlayer
import io.github.sinaraz.PlaybackConfig
import io.github.sinaraz.PlaybackListener
import io.github.sinaraz.model.AudioInfo
import io.github.sinaraz.model.PlaybackError
import io.github.sinaraz.model.PlaybackSnapshot
import io.github.sinaraz.model.PlaybackState
import io.github.sinaraz.model.Track as JudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration


class AudioPlayerManager(
    private val scope: CoroutineScope,
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(AudioPlayerManager::class.java)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()


    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors.asSharedFlow()


    @Volatile
    var onTrackEnded: (() -> Unit)? = null

    private val engineMutex = Mutex()

    private val player: JudioPlayer = JudioPlayer.builder()
        .config(
            PlaybackConfig.builder()
                .progressInterval(Duration.ofMillis(250))
                .previousRestartThreshold(Duration.ofSeconds(3))
                .build(),
        )
        .build()

    init {
        player.addListener(object : PlaybackListener {

            override fun onStateChanged(snapshot: PlaybackSnapshot) = mapEngineState(snapshot)

            override fun onProgress(snapshot: PlaybackSnapshot) {
                _state.update {
                    it.copy(
                        currentPositionMs = snapshot.position().toMillis(),
                        durationMs = maxOf(snapshot.duration().toMillis(), it.durationMs),
                    )
                }
            }

            override fun onTrackLoaded(track: JudioTrack, info: AudioInfo) {
                val durationMs = runCatching { info.duration() }
                    .mapCatching { d -> d?.takeIf { it.toMillis() > 0 }?.toMillis() }
                    .getOrNull()
                if (durationMs != null) {
                    _state.update { it.copy(durationMs = durationMs) }
                }
                logger.info("Stream loaded: {} ({} Hz, {} ch)", track.displayName(), info.sampleRate(), info.channels())
            }

            override fun onPlaybackCompleted(track: JudioTrack) {
                _state.update {
                    it.copy(
                        status = PlaybackStatus.STOPPED,
                        isPlaying = false,
                        isBuffering = false,
                        currentPositionMs = it.durationMs,
                    )
                }
                onTrackEnded?.invoke()
            }

            override fun onError(error: PlaybackError) {
                logger.warn("Playback error on {}: {}", error.track()?.displayName(), error.message())
                _state.update {
                    it.copy(status = PlaybackStatus.ERROR, isPlaying = false, isBuffering = false)
                }
                emitError(error.message() ?: "Playback failed")
            }
        })

        runCatching { player.setVolume(_state.value.volume.toDouble()) }
    }


    fun markBuffering(track: Track) {
        _state.update {
            it.copy(
                currentTrack = track,
                status = PlaybackStatus.BUFFERING,
                isPlaying = false,
                isBuffering = true,
                currentPositionMs = 0L,
                durationMs = track.durationMs,
            )
        }
    }

    fun playStream(track: Track, streamUrl: String) {
        markBuffering(track)
        scope.launch(Dispatchers.IO) {
            engineMutex.withLock {
                runCatching {
                    player.clear()
                    player.addUrl(URI.create(streamUrl))
                    player.setMuted(_state.value.isMuted)
                    player.setVolume(_state.value.volume.toDouble())
                    player.play()
                }.onFailure { t ->
                    logger.error("Failed to start stream: {}", streamUrl, t)
                    _state.update {
                        it.copy(status = PlaybackStatus.ERROR, isPlaying = false, isBuffering = false)
                    }
                    emitError("Could not start the stream: ${t.message ?: t.javaClass.simpleName}")
                }
            }
        }
    }


    fun pause() = transportSilently("pause") { player.pause() }

    fun resume() = transportSilently("resume") { player.resume() }

    fun stop() = transportSilently("stop") {
        player.stop()
        _state.update {
            it.copy(status = PlaybackStatus.STOPPED, isPlaying = false, currentPositionMs = 0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _state.value.durationMs
        if (duration <= 0L || _state.value.currentTrack == null) return
        val clamped = positionMs.coerceIn(0L, duration)
        _state.update { it.copy(currentPositionMs = clamped) }
        runCatching { player.seek(Duration.ofMillis(clamped)) }
            .onFailure { emitError("Seek is not supported for this stream") }
    }


    fun setVolume(volume: Float) {
        val value = volume.coerceIn(0f, 1f)
        _state.update { it.copy(volume = value, isMuted = if (value > 0f) false else it.isMuted) }
        runCatching { player.setVolume(value.toDouble()) }
    }

    fun setMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
        runCatching { player.setMuted(muted) }
    }


    fun reportTrackError(message: String) {
        _state.update { it.copy(status = PlaybackStatus.ERROR, isPlaying = false, isBuffering = false) }
        emitError(message)
    }

    private fun mapEngineState(snapshot: PlaybackSnapshot) {
        val previous = _state.value.status
        val status = when (snapshot.state()) {
            PlaybackState.EMPTY -> PlaybackStatus.IDLE
            PlaybackState.LOADING -> PlaybackStatus.BUFFERING
            PlaybackState.READY -> if (previous == PlaybackStatus.BUFFERING) previous else PlaybackStatus.PAUSED
            PlaybackState.PLAYING -> PlaybackStatus.PLAYING
            PlaybackState.PAUSED -> PlaybackStatus.PAUSED
            PlaybackState.SEEKING -> previous
            PlaybackState.STOPPING, PlaybackState.STOPPED, PlaybackState.ENDED -> PlaybackStatus.STOPPED
            PlaybackState.ERROR -> PlaybackStatus.ERROR
            PlaybackState.CLOSED -> PlaybackStatus.IDLE
        }
        if (status != previous) {
            _state.update {
                it.copy(
                    status = status,
                    isPlaying = status == PlaybackStatus.PLAYING,
                    isBuffering = status == PlaybackStatus.BUFFERING,
                )
            }
        }
    }

    private fun transportSilently(name: String, block: () -> Unit) {
        runCatching { block() }.onFailure { t ->
            logger.debug("{} rejected by engine: {}", name, t.message)
        }
    }

    private fun emitError(message: String) {
        _errors.tryEmit(message)
    }

    override fun close() {
        onTrackEnded = null
        runCatching { player.close() }
    }
}
