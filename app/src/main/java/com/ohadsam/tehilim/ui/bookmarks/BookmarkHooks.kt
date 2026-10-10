package com.ohadsam.tehilim.ui.bookmarks

import com.ohadsam.tehilim.bookmarks.Bookmark
import com.ohadsam.tehilim.domain.Verse

/** What a reader needs to show and create bookmarks, supplied by [BookmarkHost]. */
class BookmarkHooks(
    val forChapter: (Int) -> Map<Int, Bookmark>,
    val onVerseLongPress: (chapter: Int, verse: Verse) -> Unit,
    val onMarkerClick: (Bookmark) -> Unit,
)
