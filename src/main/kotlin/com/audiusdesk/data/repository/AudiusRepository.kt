package com.audiusdesk.data.repository

import com.audiusdesk.core.network.AudiusApiClient
import com.audiusdesk.data.model.Track
import com.audiusdesk.data.model.toDomain


class AudiusRepository(
    private val api: AudiusApiClient,
) {

    suspend fun trendingTracks(limit: Int = 48): List<Track> =
        api.trending(limit)
            .mapNotNull { it.toDomain() }
            .filter { it.isStreamable }
            .distinctBy { it.id }

    suspend fun searchTracks(query: String, limit: Int = 48): List<Track> =
        api.search(query.trim(), limit)
            .mapNotNull { it.toDomain() }
            .filter { it.isStreamable }
            .distinctBy { it.id }

    suspend fun resolveStreamUrl(track: Track): String =
        api.resolveStreamUrl(track.id, track.streamUrlHint)
}
