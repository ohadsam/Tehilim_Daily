package com.ohadsam.tehilim.share

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Pulls the text of a share out of an incoming intent: an opened `.tehilim` file (VIEW) or a file / message sent to the app (SEND). */
object IncomingShare {
    private const val MAX_BYTES = 512 * 1024

    fun isCandidate(intent: Intent?): Boolean {
        val action = intent?.action
        return (action == Intent.ACTION_VIEW && intent.data != null) || action == Intent.ACTION_SEND
    }

    /** Blocking (reads from a content provider): call off the main thread. Null when the intent carries nothing readable. */
    fun readText(context: Context, intent: Intent): String? {
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> streamOf(intent)
            else -> null
        }
        if (uri != null) readUri(context, uri)?.let { return it }
        return if (intent.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) else null
    }

    @Suppress("DEPRECATION")
    private fun streamOf(intent: Intent): Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)

    fun readUri(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (out.size() <= MAX_BYTES) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }.getOrNull()
}
