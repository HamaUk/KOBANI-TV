package com.ultratv.tv.nativeapp.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultratv.tv.nativeapp.data.repo.HistoryRepository
import com.ultratv.tv.nativeapp.data.repo.PlaybackContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoviePlayerViewModel @Inject constructor(
    private val playback: PlaybackContext,
    private val history: HistoryRepository,
    private val prefs: com.ultratv.tv.nativeapp.data.prefs.UserPreferencesStore,
) : ViewModel() {

    suspend fun playbackPrefs(): com.ultratv.tv.nativeapp.data.prefs.UserPrefs =
        prefs.flow.first()

    val current: StateFlow<PlaybackContext.Item?> = playback.current

    /**
     * Reads the last persisted position for the current item and returns the
     * offset (ms) the player should seek to once the source is ready.
     */
    suspend fun prepareResume(): Long {
        val c = playback.current.value ?: return 0L
        if (c.kind == "LIVE") return 0L
        return history.resumePositionMs(c.providerId, c.kind, c.remoteId)
    }

    /** Persists the current playback position. Called periodically + on dispose. */
    fun recordProgress(positionMs: Long, durationMs: Long) {
        val c = playback.current.value ?: return
        if (positionMs < 5_000) return // ignore noise from the first 5s
        if (c.kind == "LIVE") return
        viewModelScope.launch {
            history.record(
                providerId = c.providerId,
                kind = c.kind,
                remoteId = c.remoteId,
                title = c.title,
                poster = c.poster,
                streamUrl = c.streamUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                parentRemoteId = c.parentRemoteId,
            )
        }
    }
}
