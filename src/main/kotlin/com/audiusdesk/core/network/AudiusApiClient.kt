package com.audiusdesk.core.network

import com.audiusdesk.data.model.HostSelectionResponse
import com.audiusdesk.data.model.TrackDto
import com.audiusdesk.data.model.TrackListResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException

class AudiusApiClient : AutoCloseable {

    private val logger = LoggerFactory.getLogger(AudiusApiClient::class.java)

    companion object {
        const val APP_NAME = "AudiusDesk"
        const val DISCOVERY_URL = "https://api.audius.co"

        private val FALLBACK_HOSTS = listOf(
            "https://api.audius.co",
            "https://discoveryprovider.audius.co",
            "https://discoveryprovider2.audius.co",
            "https://discoveryprovider3.audius.co",
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    private val client = HttpClient(Java) {
        expectSuccess = true
        followRedirects = true

        install(ContentNegotiation) { json(json) }

        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 6_000
            socketTimeoutMillis = 20_000
        }

        install(UserAgent) { agent = "AudiusDesk/1.0 (desktop; ktor-client-java)" }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay(base = 2.0, maxDelayMs = 3_000)
        }
    }

    private val redirectProbe = HttpClient(Java) {
        expectSuccess = false
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
    }

    private val hostMutex = Mutex()

    @Volatile
    private var hosts: List<String> = FALLBACK_HOSTS

    @Volatile
    private var hostIndex: Int = 0

    private val _activeHost = MutableStateFlow<String?>(null)

    val activeHost: StateFlow<String?> = _activeHost.asStateFlow()

    private fun currentHost(): String = hosts[hostIndex % hosts.size]

    suspend fun warmUpDiscovery() = refreshHosts()

    private suspend fun refreshHosts() {
        try {
            val discovered = client.get(DISCOVERY_URL).body<HostSelectionResponse>().data
                .map { it.trim().trimEnd('/') }
                .filter { it.startsWith("http") }

            if (discovered.isNotEmpty()) {
                val merged = (discovered + FALLBACK_HOSTS).distinct()
                hostMutex.withLock {
                    hosts = merged
                    hostIndex %= merged.size
                }
                logger.info("Audius host discovery: {} active node(s)", discovered.size)
            }
        } catch (t: Throwable) {
            logger.warn("Audius host discovery failed, keeping fallbacks: {}", t.message)
        }
    }

    private suspend fun <T> withHostFailover(block: suspend (host: String) -> T): T {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            val host = currentHost()
            try {
                val result = block(host)
                _activeHost.value = host
                return result
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                lastError = t
                logger.warn("Audius request failed on {} (attempt {}/3): {}", host, attempt + 1, t.message)
                hostMutex.withLock {
                    if (attempt == 0) refreshHosts()
                    hostIndex = (hostIndex + 1) % hosts.size
                }
            }
        }
        throw AudiusApiException("All Audius discovery nodes failed to answer", lastError)
    }

    suspend fun trending(limit: Int = 48, timeWindow: String = "week"): List<TrackDto> =
        withHostFailover { host ->
            client.get("$host/v1/tracks/trending") {
                parameter("app_name", APP_NAME)
                parameter("limit", limit)
                parameter("time", timeWindow)
            }.body<TrackListResponse>().data
        }

    suspend fun search(query: String, limit: Int = 48): List<TrackDto> =
        withHostFailover { host ->
            client.get("$host/v1/tracks/search") {
                parameter("app_name", APP_NAME)
                parameter("query", query)
                parameter("limit", limit)
            }.body<TrackListResponse>().data
        }

    suspend fun resolveStreamUrl(trackId: String, fallbackStreamUrl: String?): String {
        val host = currentHost()
        val endpoint = URLBuilder(host).apply {
            appendPathSegments("v1", "tracks", trackId, "stream")
            parameters.append("app_name", APP_NAME)
        }.buildString()

        return try {
            val response = redirectProbe.get(endpoint)
            val location = response.headers[HttpHeaders.Location]
            if (response.status.value in 300..399 && !location.isNullOrBlank()) {
                if (location.startsWith("http", ignoreCase = true)) location else host + location
            } else {
                fallbackStreamUrl ?: endpoint
            }
        } catch (t: Throwable) {
            logger.debug("Stream redirect probe failed for {}: {}", trackId, t.message)
            fallbackStreamUrl ?: endpoint
        }
    }

    override fun close() {
        client.close()
        redirectProbe.close()
    }
}

class AudiusApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
