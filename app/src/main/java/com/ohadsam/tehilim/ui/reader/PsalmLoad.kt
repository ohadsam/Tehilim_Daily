package com.ohadsam.tehilim.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.ohadsam.tehilim.data.PsalmsRepository
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import com.ohadsam.tehilim.domain.PsalmRef
import com.ohadsam.tehilim.domain.PsalmText

sealed interface LoadState {
    data object Loading : LoadState

    data class Success(val text: PsalmText) : LoadState

    data class Error(val kind: PsalmErrorKind) : LoadState
}

/** Loads [ref] through the local-first repository; [retryKey] re-runs it after a failure. */
@Composable
fun rememberPsalm(repo: PsalmsRepository, ref: PsalmRef, retryKey: Int): State<LoadState> =
    produceState<LoadState>(LoadState.Loading, ref, retryKey) {
        value = LoadState.Loading
        value = repo.get(ref).fold(
            onSuccess = { LoadState.Success(it) },
            onFailure = { LoadState.Error((it as? PsalmException)?.kind ?: PsalmErrorKind.INVALID_DATA) },
        )
    }
