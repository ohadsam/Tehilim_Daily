package com.ohadsam.tehilim.share

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

sealed interface ShareParse {
    data class Ok(val pkg: SharePackage) : ShareParse

    /** The text holds nothing that looks like a share. */
    data object NotShare : ShareParse

    /** It looks like a share but is damaged, unsupported or from a newer app version. */
    data class Invalid(val reason: String) : ShareParse
}

/**
 * Two interchangeable encodings of a [SharePackage]:
 *  - a file (`.tehilim`) holding the JSON — opened straight from WhatsApp / mail;
 *  - a text code `TD1:<payload>:` (deflate + base64) that survives being pasted into any chat, and can be forwarded or pasted
 *    into the app. The payload alphabet avoids `_ * ~` which chat apps turn into formatting, and `:` ends the code.
 */
object ShareCodec {
    const val FILE_EXTENSION = "tehilim"
    const val MIME = "application/x-tehilim"
    private const val PREFIX = "TD1:"
    private const val MAX_INPUT = 512 * 1024
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }
    private val codeRegex = Regex("TD1:([A-Za-z0-9+.]+):")

    fun toJson(p: SharePackage): String = json.encodeToString(SharePackage.serializer(), p)

    fun toCode(p: SharePackage): String {
        val raw = toJson(p).toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        DeflaterOutputStream(out, Deflater(Deflater.BEST_COMPRESSION)).use { it.write(raw) }
        val b64 = Base64.getEncoder().withoutPadding().encodeToString(out.toByteArray())
        return PREFIX + b64.replace('/', '.') + ":"
    }

    /** Accepts a whole `.tehilim` file, a chat message containing a code, or just the code. */
    fun parse(text: String): ShareParse {
        if (text.length > MAX_INPUT) return ShareParse.Invalid("too large")
        val trimmed = text.trim { it.isWhitespace() || it == '\uFEFF' }
        val body = when {
            trimmed.startsWith("{") -> trimmed
            else -> {
                val payload = codeRegex.find(trimmed)?.groupValues?.get(1) ?: return ShareParse.NotShare
                inflate(payload) ?: return ShareParse.Invalid("damaged code")
            }
        }
        val pkg = try {
            json.decodeFromString(SharePackage.serializer(), body)
        } catch (e: Exception) {
            return if (trimmed.startsWith("{")) ShareParse.NotShare else ShareParse.Invalid("damaged data")
        }
        if (pkg.format != SharePackage.FORMAT) return ShareParse.NotShare
        return pkg.validate()?.let { ShareParse.Invalid(it) } ?: ShareParse.Ok(pkg)
    }

    private fun inflate(payload: String): String? = runCatching {
        val bytes = Base64.getDecoder().decode(payload.replace('.', '/'))
        // Adler-32 inside the zlib stream makes a damaged code fail here instead of yielding wrong chapters.
        val out = ByteArrayOutputStream()
        InflaterInputStream(bytes.inputStream()).use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                require(out.size() <= MAX_INPUT) { "too large" }
            }
        }
        out.toString(Charsets.UTF_8.name())
    }.getOrNull()
}
