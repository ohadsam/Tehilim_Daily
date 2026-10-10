package com.ohadsam.tehilim.ui.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Horizontally scrolling single-select chips that keep the selected chip centred
 * (long days such as day 27 with 15 chapters would otherwise hide it off-screen).
 */
@Composable
fun ChipStrip(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, labels.size) {
        if (selectedIndex in labels.indices) {
            val viewport = listState.layoutInfo.viewportSize.width
            // Negative offset pulls the item towards the middle; exact centring needs the item width, so use a conservative estimate.
            listState.animateScrollToItem(selectedIndex, scrollOffset = -(viewport / 2) + CHIP_HALF_WIDTH_PX)
        }
    }
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(labels) { i, label ->
            FilterChip(
                selected = i == selectedIndex,
                onClick = { onSelect(i) },
                label = { Text(label, modifier = Modifier.padding(horizontal = 2.dp)) },
            )
        }
    }
}

private const val CHIP_HALF_WIDTH_PX = 60
