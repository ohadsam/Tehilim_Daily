package com.ohadsam.tehilim.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohadsam.tehilim.data.prefs.ReaderSettings
import com.ohadsam.tehilim.domain.TextDisplay
import kotlin.math.roundToInt

private const val SAMPLE = "אַשְׁרֵי־הָאִישׁ אֲשֶׁר לֹא הָלַךְ בַּעֲצַת רְשָׁעִים"

/** Live preview of the chosen look (the real widget uses system fonts and cannot show per-line spacing exactly). */
@Composable
fun StylePreview(d: WidgetDisplayPrefs, app: ReaderSettings) {
    val r = WidgetDisplay.resolve(d, app)
    val bg = r.background?.let { Color(it).copy(alpha = r.bgAlpha) } ?: MaterialTheme.colorScheme.surface
    val fg = r.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)).padding(12.dp)) {
        if (r.showTitle) Text("תצוגה מקדימה", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(Modifier.padding(top = r.spacingDp.dp), verticalAlignment = Alignment.Top) {
            Text(if (r.numbers) "א" else "·", color = MaterialTheme.colorScheme.secondary, fontSize = (r.fontSp * 0.7f).sp, modifier = Modifier.padding(end = 6.dp))
            Text(
                TextDisplay.apply(SAMPLE, r.nikud, r.taamim),
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = fg, fontSize = r.fontSp.sp, fontWeight = if (r.bold) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = when (r.family) { "serif" -> FontFamily.Serif; "mono" -> FontFamily.Monospace; else -> FontFamily.SansSerif },
                    textAlign = if (r.centered) TextAlign.Center else TextAlign.Start,
                ),
            )
        }
    }
}

/** All the look options of a widget; [textOptions] hides nikud/ta'amim/verse numbers for widgets that show no psalm text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleEditor(d: WidgetDisplayPrefs, onChange: (WidgetDisplayPrefs) -> Unit, textOptions: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToggleRow("לפי הגדרות הקריאה של האפליקציה (גודל${if (textOptions) ", ניקוד, טעמים, מספרי פסוקים" else ""})", d.followApp) { onChange(d.copy(followApp = it)) }
        if (!d.followApp) {
            Text("גודל גופן: ${d.fontSp.roundToInt()}")
            Slider(d.fontSp, { onChange(d.copy(fontSp = it)) }, valueRange = WidgetDisplay.MIN_SP..WidgetDisplay.MAX_SP)
            if (textOptions) {
                ToggleRow("ניקוד", d.nikud) { onChange(d.copy(nikud = it)) }
                ToggleRow("טעמי מקרא", d.taamim) { onChange(d.copy(taamim = it)) }
                ToggleRow("מספרי פסוקים", d.numbers) { onChange(d.copy(numbers = it)) }
            }
        }
        ToggleRow("גופן מודגש", d.bold) { onChange(d.copy(bold = it)) }
        Text("משפחת גופן")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("sans" to "רגיל", "serif" to "סריף", "mono" to "קבוע").forEach { (key, label) ->
                FilterChip(d.family == key, { onChange(d.copy(family = key)) }, { Text(label) })
            }
        }
        Text("יישור טקסט")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!d.centered, { onChange(d.copy(centered = false)) }, { Text("לתחילת השורה") })
            FilterChip(d.centered, { onChange(d.copy(centered = true)) }, { Text("למרכז") })
        }
        Text("מרווח בין שורות: ${d.spacingDp}")
        Slider(d.spacingDp.toFloat(), { onChange(d.copy(spacingDp = it.roundToInt())) }, valueRange = 0f..WidgetDisplay.MAX_SPACING.toFloat(), steps = WidgetDisplay.MAX_SPACING - 1)
        ToggleRow("להציג כותרת", d.showTitle) { onChange(d.copy(showTitle = it)) }
        Text("צבע רקע")
        Swatches(BACKGROUNDS, d.background) { onChange(d.copy(background = it)) }
        if (d.background != 0) {
            Text("שקיפות הרקע: ${(d.bgAlpha * 100).roundToInt()}%")
            Slider(d.bgAlpha, { onChange(d.copy(bgAlpha = it)) }, valueRange = WidgetDisplay.MIN_ALPHA..1f)
        }
        Text("צבע טקסט")
        Swatches(TEXTS, d.textColor) { onChange(d.copy(textColor = it)) }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Switch(value, onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(colors: List<Color>, selected: Int, onPick: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onPick(0) }) { Text("ברירת מחדל") }
        colors.forEach { c ->
            val argb = c.toArgb()
            val sel = selected == argb
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c)
                    .border(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onPick(argb) },
            )
        }
    }
}

private val BACKGROUNDS = listOf(0xFFFBF6EA, 0xFFFFFFFF, 0xFFF4ECD8, 0xFFEAF4EA, 0xFF1C2335, 0xFF0F1420, 0xFF000000).map { Color(it) }
private val TEXTS = listOf(0xFF1B1B1F, 0xFF000000, 0xFF3E2C00, 0xFFFFFFFF, 0xFFE6E1D6, 0xFFF2C14E).map { Color(it) }
