package com.audiusdesk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class HostSelectionResponse(
    val data: List<String> = emptyList(),
)

@Serializable
data class TrackListResponse(
    val data: List<TrackDto> = emptyList(),
)

@Serializable
data class TrackDto(
    val id: String,
    val title: String = "",
    val duration: Int = 0,
    val genre: String? = null,
    val mood: String? = null,
    val permalink: String? = null,
    @SerialName("play_count") val playCount: Long = 0,
    @SerialName("repost_count") val repostCount: Long = 0,
    @SerialName("favorite_count") val favoriteCount: Long = 0,
    @SerialName("is_streamable") val isStreamable: Boolean = true,
    @SerialName("is_stream_gated") val isStreamGated: Boolean = false,
    @SerialName("is_available") val isAvailable: Boolean = true,
    @SerialName("is_delete") val isDeleted: Boolean = false,
    @SerialName("is_unlisted") val isUnlisted: Boolean = false,
    val artwork: ArtworkDto? = null,
    val stream: StreamInfoDto? = null,
    val access: AccessDto = AccessDto(),
    val user: UserDto? = null,
)

@Serializable
data class ArtworkDto(
    @SerialName("150x150") val small: String? = null,
    @SerialName("480x480") val medium: String? = null,
    @SerialName("1000x1000") val large: String? = null,
)

@Serializable
data class StreamInfoDto(
    val url: String? = null,
)

@Serializable
data class AccessDto(
    val stream: Boolean = true,
    val download: Boolean = false,
)

@Serializable
data class UserDto(
    val id: String = "",
    val name: String = "",
    val handle: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
)
