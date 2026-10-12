package com.ohadsam.tehilim.ui.reader

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ohadsam.tehilim.R

/** One-tap "נאמר" toggle: a compact chip that fills and shows a check once marked. */
@Composable
fun SaidBar(said: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = said,
        onClick = onToggle,
        label = { Text(stringResource(if (said) R.string.said_done_short else R.string.said_mark_short)) },
        leadingIcon = if (said) ({ Icon(Icons.Filled.Check, contentDescription = null) }) else null,
        modifier = modifier,
    )
}
