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
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        val body = try {
            client.newCall(request).execute().use { resp ->
                when {
                    resp.code == 404 -> throw PsalmException(PsalmErrorKind.NOT_FOUND, "HTTP 404 for Psalms.$chapter")
                    !resp.isSuccessful -> throw PsalmException(PsalmErrorKind.SERVER, "HTTP ${resp.code} for Psalms.$chapter")
                    else -> resp.body?.string() ?: throw PsalmException(PsalmErrorKind.SERVER, "empty body for Psalms.$chapter")
                }
            }
        } catch (e: IOException) {
            throw PsalmException(PsalmErrorKind.OFFLINE, "network error for Psalms.$chapter: ${e.message}", e)
        }
        SefariaParser.parseChapter(body)
    }
}
