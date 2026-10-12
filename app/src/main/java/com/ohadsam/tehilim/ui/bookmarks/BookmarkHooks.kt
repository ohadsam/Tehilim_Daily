package com.ohadsam.tehilim.ui.bookmarks

import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.domain.Verse

/** Lets a reader tell the bookmark UI which verse is at the top of the screen right now. */
class VisibleVerseHolder {
    var provider: (() -> Pair<Int, Verse>?)? = null

    fun current(): Pair<Int, Verse>? = provider?.invoke()
}

/** What a reader needs to show and create bookmarks, supplied by [BookmarkHost]. */
class BookmarkHooks(
    val forChapter: (Int) -> Map<Int, Bookmark>,
    val onVerseLongPress: (chapter: Int, verse: Verse) -> Unit,
    val onMarkerClick: (Bookmark) -> Unit,
    val all: List<Bookmark> = emptyList(),
    /** Adds (or opens the editor of) the bookmark for the verse at the top of the screen. */
    val onAddHere: () -> Unit = {},
    val visibleVerse: VisibleVerseHolder = VisibleVerseHolder(),
)
