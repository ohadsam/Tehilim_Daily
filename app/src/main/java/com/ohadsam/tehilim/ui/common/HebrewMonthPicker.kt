package com.ohadsam.tehilim.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/** Hebrew months in calendar order from Tishrei; 12 is "Adar" (also Adar I's slot) and 13 is "Adar II". */
val HebrewMonthChoices: List<Pair<Int, String>> = listOf(
    7 to "תשרי", 8 to "חשוון", 9 to "כסלו", 10 to "טבת", 11 to "שבט", 12 to "אדר", 13 to "אדר ב׳",
    1 to "ניסן", 2 to "אייר", 3 to "סיוון", 4 to "תמוז", 5 to "אב", 6 to "אלול",
)

/** Longest possible length of a Hebrew month in any year (the variable months can have 30 days). */
fun maxDayOfHebrewMonth(month: Int): Int = if (month in intArrayOf(2, 4, 6, 10, 13)) 29 else 30

@Composable
fun HebrewMonthPicker(selected: Int?, placeholder: String, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(HebrewMonthChoices.firstOrNull { it.first == selected }?.second ?: placeholder)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            HebrewMonthChoices.forEach { (m, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(m)
                    open = false
                })
            }
        }
    }
}
