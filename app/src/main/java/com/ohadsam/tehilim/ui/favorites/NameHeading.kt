package com.ohadsam.tehilim.ui.favorites

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.ohadsam.tehilim.favorites.NameSegment

/** The word above a stanza: every letter plain, except the stanza's own letter(s) — large, bold, coloured and shaded. */
fun nameHeading(seg: NameSegment, normal: Color, accent: Color, accentBackground: Color): AnnotatedString = buildAnnotatedString {
    seg.label.forEachIndexed { i, ch ->
        if (i in seg.highlight) {
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Black, fontSize = 34.sp, background = accentBackground)) { append(ch) }
        } else {
            withStyle(SpanStyle(color = normal, fontSize = 26.sp)) { append(ch) }
        }
    }
}
