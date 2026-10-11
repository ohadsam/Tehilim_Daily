package com.ohadsam.tehilim.favorites

import com.ohadsam.tehilim.domain.PsalmRef

/** What the user typed: one or more names (e.g. first + second name) and whether to add the stanzas of "נשמה" after them. */
data class NameSpec(val names: List<String>, val neshama: Boolean) {
    val fullName: String get() = names.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    fun encode(): String = names.joinToString("|") { it.replace("|", " ").replace("#", " ") } + "#" + if (neshama) "1" else "0"

    companion object {
        fun decode(s: String?): NameSpec? {
            if (s.isNullOrBlank() || '#' !in s) return null
            val (names, flag) = s.substringBeforeLast('#') to s.substringAfterLast('#')
            return NameSpec(names.split("|").filter { it.isNotBlank() }, flag == "1").takeIf { it.names.isNotEmpty() }
        }
    }
}

enum class NameSection { NAME, NESHAMA }

/** One psalm 119 stanza to read, with the word shown above it and which characters of that word are "its" letter. */
data class NameSegment(val ref: PsalmRef, val section: NameSection, val label: String, val highlight: Set<Int>)

/**
 * Turns a full name into the stanzas of Psalm 119 to read: every distinct letter of the name in order of first appearance
 * (all its occurrences are highlighted in the name shown above the stanza), then — optionally — the four letters of "נשמה".
 * The "נשמה" section is always complete, even when a letter also occurs in the name.
 */
object NameLetterPlan {
    fun build(spec: NameSpec): List<NameSegment> {
        val full = spec.fullName
        val out = mutableListOf<NameSegment>()
        val seen = LinkedHashMap<PsalmRef, MutableSet<Int>>()
        full.forEachIndexed { i, ch -> NameLetters.stanza(ch)?.let { seen.getOrPut(it) { mutableSetOf() } += i } }
        seen.forEach { (ref, idx) -> out += NameSegment(ref, NameSection.NAME, full, idx) }
        if (spec.neshama) {
            NameLetters.NESHAMA.forEachIndexed { i, ch ->
                NameLetters.stanza(ch)?.let { out += NameSegment(it, NameSection.NESHAMA, NameLetters.NESHAMA, setOf(i)) }
            }
        }
        return out
    }

    fun refs(spec: NameSpec): List<PsalmRef> = build(spec).map { it.ref }
}
