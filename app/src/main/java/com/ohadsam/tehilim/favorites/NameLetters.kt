package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef

/**
 * Psalm 119 is an acrostic of 22 stanzas of 8 verses, one per letter of the alef-bet. A widespread custom is to read, for a
 * person, the stanzas of the letters of their name (and for ilui nishmat also those of "נשמה").
 * (Custom to be confirmed by the app owner before release — see AGENT.md open items.)
 */
object NameLetters {
    private const val ALPHABET = "אבגדהוזחטיכלמנסעפצקרשת"
    private val finals = mapOf('ך' to 'כ', 'ם' to 'מ', 'ן' to 'נ', 'ף' to 'פ', 'ץ' to 'צ')
    private const val VERSES_PER_STANZA = 8
    const val NESHAMA = "נשמה"

    /** Stanza (8 verses) of Psalm 119 for a single letter; null if it is not a Hebrew letter. */
    fun stanza(letter: Char): PsalmRef? {
        val i = ALPHABET.indexOf(finals[letter] ?: letter)
        if (i < 0) return null
        return PsalmRef(119, i * VERSES_PER_STANZA + 1, (i + 1) * VERSES_PER_STANZA)
    }

    /** One stanza per distinct letter, in the order the letters first appear in [name]; non-letters are ignored. */
    fun stanzasFor(name: String): List<PsalmRef> = name.mapNotNull(::stanza).distinct()
}
