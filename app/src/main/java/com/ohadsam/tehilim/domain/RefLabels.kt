package com.ohadsam.tehilim.domain

/** "קי״ט" for a whole chapter, "קי״ט (א–פח)" for a verse range — compact label for chips. */
fun PsalmRef.shortLabel(): String {
    val ch = HebrewNumerals.toHebrew(chapter)
    if (isWhole) return ch
    return "$ch (${HebrewNumerals.toHebrew(fromVerse!!, false)}–${HebrewNumerals.toHebrew(toVerse!!, false)})"
}

/** "פרק קי״ט" / "פרק קי״ט · פסוקים א׳–פ״ח" — heading above the text. */
fun PsalmRef.title(): String {
    val base = "פרק ${HebrewNumerals.toHebrew(chapter)}"
    if (isWhole) return base
    return "$base · פסוקים ${HebrewNumerals.toHebrew(fromVerse!!)}–${HebrewNumerals.toHebrew(toVerse!!)}"
}
