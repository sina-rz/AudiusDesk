package com.audiusdesk.data.model


data class Track(
    val id: String,
    val title: String,
    val artistName: String,
    val artistHandle: String,
    val durationSeconds: Int,
    val artworkSmall: String?,
    val artworkMedium: String?,
    val artworkLarge: String?,
    val playCount: Long,
    val repostCount: Long,
    val favoriteCount: Long,
    val genre: String?,
    val isStreamable: Boolean,
    val permalinkUrl: String?,
    val streamUrlHint: String?,
) {
    val durationMs: Long get() = durationSeconds * 1_000L
}

fun TrackDto.toDomain(): Track? {
    if (id.isBlank()) return null

    val streamable = isStreamable && isAvailable && !isDeleted && !isStreamGated && access.stream
    val artist = user?.name?.takeIf { it.isNotBlank() } ?: "Unknown artist"

    return Track(
        id = id,
        title = title.ifBlank { "Untitled track" },
        artistName = artist,
        artistHandle = user?.handle.orEmpty(),
        durationSeconds = duration.coerceAtLeast(0),
        artworkSmall = artwork?.small ?: artwork?.medium,
        artworkMedium = artwork?.medium ?: artwork?.small ?: artwork?.large,
        artworkLarge = artwork?.large ?: artwork?.medium ?: artwork?.small,
        playCount = playCount,
        repostCount = repostCount,
        favoriteCount = favoriteCount,
        genre = genre,
        isStreamable = streamable,
        permalinkUrl = permalink?.let { "https://audius.co$it" },
        streamUrlHint = stream?.url?.takeIf { it.startsWith("http") },
    )
}
