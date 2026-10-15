package com.audiusdesk

import com.audiusdesk.core.audio.AudioPlayerManager
import com.audiusdesk.core.network.AudiusApiClient
import com.audiusdesk.data.repository.AudiusRepository
import com.audiusdesk.ui.viewmodel.MainViewModel
import com.audiusdesk.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudiusDeskApp : AutoCloseable {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val apiClient = AudiusApiClient()
    val repository = AudiusRepository(apiClient)
    val audioPlayer = AudioPlayerManager(applicationScope)

    val mainViewModel = MainViewModel(repository)
    val playerViewModel = PlayerViewModel(repository, audioPlayer)

    private val closed = AtomicBoolean(false)

    init {
        applicationScope.launch {
            runCatching {
                apiClient.warmUpDiscovery()
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            playerViewModel.close()
            mainViewModel.close()
            audioPlayer.close()
            apiClient.close()
            applicationScope.cancel()
        }
    }
}
