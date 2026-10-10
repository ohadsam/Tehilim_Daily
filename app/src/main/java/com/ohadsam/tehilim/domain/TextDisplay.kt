package com.ohadsam.tehilim.domain

/**
 * View-only transformations of the stored text (the stored text itself is never changed):
 * nikud = vowel points and shin/sin dots; ta'amim = cantillation marks. Meteg (U+05BD) is kept with the vowels.
 */
object TextDisplay {
    private fun isTaam(c: Char) = c in '֑'..'֯'

    private fun isNikud(c: Char) = c in 'ְ'..'ֽ' || c == 'ֿ' || c == 'ׁ' || c == 'ׂ' || c == 'ׇ'

    fun apply(text: String, showNikud: Boolean, showTaamim: Boolean): String {
        if (showNikud && showTaamim) return text
        val sb = StringBuilder(text.length)
        for (c in text) {
            when {
                isTaam(c) && !showTaamim -> Unit
                isNikud(c) && !showNikud -> Unit
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
