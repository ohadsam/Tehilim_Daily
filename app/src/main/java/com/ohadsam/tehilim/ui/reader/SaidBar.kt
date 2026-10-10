package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.tehilim.R

/** One-tap "נאמר" toggle shown under day-based readers; filled and checked once marked. */
@Composable
fun SaidBar(said: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val m = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    if (said) {
        Button(onClick = onToggle, modifier = m) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Text(stringResource(R.string.said_done), modifier = Modifier.padding(start = 8.dp))
        }
    } else {
        FilledTonalButton(onClick = onToggle, modifier = m) {
            Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null)
            Text(stringResource(R.string.said_mark), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
