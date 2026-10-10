package com.ohadsam.tehilim.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Runs "download all psalms" in the app scope so it survives leaving the screen; [state] is null when idle. */
class PrefetchController(private val repo: PsalmsRepository, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<PrefetchProgress?>(null)
    val state: StateFlow<PrefetchProgress?> = _state.asStateFlow()

    fun start() {
        if (_state.value != null) return
        _state.value = PrefetchProgress(0, com.ohadsam.tehilim.domain.PSALMS_COUNT, 0)
        scope.launch {
            try {
                repo.prefetchAll { _state.value = it }
            } finally {
                _state.value = null
            }
        }
    }
}
