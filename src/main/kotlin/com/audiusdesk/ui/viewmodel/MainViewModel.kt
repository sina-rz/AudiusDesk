package com.audiusdesk.ui.viewmodel

import com.audiusdesk.data.model.Track
import com.audiusdesk.data.repository.AudiusRepository
import com.audiusdesk.ui.util.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.util.concurrent.CancellationException
import kotlin.time.Duration.Companion.milliseconds

enum class AppSection(val title: String) {
    EXPLORE("Explore"),
    SEARCH("Search"),
}

data class TrackListState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SearchState(
    val results: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
)


@OptIn(FlowPreview::class)
class MainViewModel(
    private val repository: AudiusRepository,
) : AutoCloseable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    private val _section = MutableStateFlow(AppSection.EXPLORE)
    val section: StateFlow<AppSection> = _section.asStateFlow()

    private val _trending = MutableStateFlow(TrackListState(isLoading = true))
    val trending: StateFlow<TrackListState> = _trending.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refreshTrending()
        observeSearchQuery()
    }

    fun selectSection(section: AppSection) {
        _section.value = section
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank() && _section.value != AppSection.SEARCH) {
            _section.value = AppSection.SEARCH
        }
    }

    fun refreshTrending() {
        scope.launch {
            _trending.update { it.copy(isLoading = true, error = null) }
            try {
                val tracks = repository.trendingTracks()
                _trending.value = TrackListState(tracks = tracks)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                val message = t.userMessage()
                _trending.value = TrackListState(error = message)
                _messages.emit("Could not load trending tracks: $message")
            }
        }
    }

    private fun observeSearchQuery() {
        scope.launch {
            _searchQuery
                .drop(1)
                .debounce(500.milliseconds)
                .collectLatest { query -> performSearch(query.trim()) }
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            _search.value = SearchState()
            return
        }
        _search.update { it.copy(isLoading = true, error = null) }
        try {
            val results = repository.searchTracks(query)
            _search.value = SearchState(results = results, hasSearched = true)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            _search.update { it.copy(isLoading = false, error = t.userMessage()) }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
