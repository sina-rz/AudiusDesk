package com.audiusdesk.ui.util

import com.audiusdesk.core.network.AudiusApiException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.userMessage(): String = when (this) {
    is UnknownHostException, is ConnectException -> "no connection to the Audius network"
    is SocketTimeoutException -> "the request timed out"
    is AudiusApiException -> "the Audius network is unreachable right now"
    else -> message?.takeIf { it.isNotBlank() } ?: "an unexpected error occurred"
}
