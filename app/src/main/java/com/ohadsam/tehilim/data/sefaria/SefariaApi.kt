package com.ohadsam.tehilim.data.sefaria

import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Network source of whole chapters. Ranges are always sliced locally from the whole chapter. */
interface PsalmsRemote {
    suspend fun fetchChapter(chapter: Int): RemoteChapter
}

/** Shared HTTP GET with the app's error mapping (offline / 404 / server); returns the body. */
internal fun httpGetBody(client: OkHttpClient, url: okhttp3.HttpUrl, label: String): String {
    val request = Request.Builder().url(url).header("Accept", "application/json").build()
    return try {
        client.newCall(request).execute().use { resp ->
            when {
                resp.code == 404 -> throw PsalmException(PsalmErrorKind.NOT_FOUND, "HTTP 404 for $label")
                !resp.isSuccessful -> throw PsalmException(PsalmErrorKind.SERVER, "HTTP ${resp.code} for $label")
                else -> resp.body?.string() ?: throw PsalmException(PsalmErrorKind.SERVER, "empty body for $label")
            }
        }
    } catch (e: IOException) {
        throw PsalmException(PsalmErrorKind.OFFLINE, "network error for $label: ${e.message}", e)
    }
}

/** Network source of verse-aligned commentary (one commentator, one chapter). */
interface CommentaryRemote {
    suspend fun fetch(chapter: Int, source: com.ohadsam.tehilim.domain.CommentarySource): Map<Int, List<String>>
}

class SefariaCommentaryApi(
    private val client: OkHttpClient,
    private val baseUrl: String = "https://www.sefaria.org",
) : CommentaryRemote {
    override suspend fun fetch(chapter: Int, source: com.ohadsam.tehilim.domain.CommentarySource): Map<Int, List<String>> = withContext(Dispatchers.IO) {
        val label = "${source.sefariaTitle} on Psalms $chapter"
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/v3/texts")
            .addPathSegment(label)
            .addQueryParameter("version", "hebrew")
            .build()
        CommentaryParser.parse(httpGetBody(client, url, label))
    }
}

// verified against kodesh-app's working client (see .claude/skills/sefaria-fetch); live re-check runs in CI `sefaria-probe`.
class SefariaApi(
    private val client: OkHttpClient,
    private val baseUrl: String = "https://www.sefaria.org",
) : PsalmsRemote {
    override suspend fun fetchChapter(chapter: Int): RemoteChapter = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/texts/Psalms.$chapter")
            .addQueryParameter("lang", "he")
            .addQueryParameter("commentary", "0")
            .addQueryParameter("context", "0")
            .build()
        val body = httpGetBody(client, url, "Psalms.$chapter")
        SefariaParser.parseChapter(body)
    }
}
