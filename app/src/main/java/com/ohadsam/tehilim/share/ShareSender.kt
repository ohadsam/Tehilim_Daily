package com.ohadsam.tehilim.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ohadsam.tehilim.favorites.summarizeChapters
import java.io.File

/** Builds the WhatsApp / e-mail / any-app intent for a [SharePackage]: the `.tehilim` file plus a readable message with the paste-able code. */
object ShareSender {
    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    private const val GMAIL = "com.google.android.gm"

    /** A code longer than this makes the chat message unwieldy; the file alone is then sent. */
    private const val MAX_CODE_IN_MESSAGE = 6000

    fun fileName(pkg: SharePackage): String {
        val number = pkg.parts.singleOrNull()?.label?.let { Regex("\\d+").find(it)?.value }
        val suffix = number?.let { "-part$it" }.orEmpty()
        return "tehilim-${pkg.id.ifEmpty { "share" }}$suffix.${ShareCodec.FILE_EXTENSION}"
    }

    fun message(pkg: SharePackage): String = buildString {
        append("📖 ").appendLine(pkg.title)
        if (pkg.isDivision) {
            appendLine("חלוקת תהילים ל-${pkg.parts.size} חלקים — בחרו את החלק שלכם באפליקציה.")
        } else {
            val part = pkg.parts[0]
            if (part.label.isNotBlank()) appendLine(part.label)
            appendLine("פרקים: ${summarizeChapters(part.refs())}")
        }
        appendLine()
        appendLine("אם האפליקציה \"תהילים יומי\" מותקנת אצלכם — פתחו את הקובץ המצורף והחלק ייטען אוטומטית.")
        val code = ShareCodec.toCode(pkg)
        if (code.length <= MAX_CODE_IN_MESSAGE) {
            appendLine("אפשר גם: להעתיק את כל השורה הבאה ולהדביק באפליקציה (עוד ← שיתוף וחלוקה ← קבלת שיתוף), או לשתף את ההודעה הזו לאפליקציה.")
            append(code)
        }
    }

    fun prepare(context: Context, pkg: SharePackage): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName(pkg))
        file.writeText(ShareCodec.toJson(pkg), Charsets.UTF_8)
        return file
    }

    enum class Target { WHATSAPP, EMAIL, ANY }

    fun intent(context: Context, pkg: SharePackage, target: Target): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", prepare(context, pkg))
        val send = Intent(Intent.ACTION_SEND)
            .setType(ShareCodec.MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, pkg.title)
            .putExtra(Intent.EXTRA_TEXT, message(pkg))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val preferred = when (target) {
            Target.WHATSAPP -> listOf(WHATSAPP, WHATSAPP_BUSINESS)
            Target.EMAIL -> listOf(GMAIL)
            Target.ANY -> emptyList()
        }.firstOrNull { isInstalled(context, it) }
        if (preferred != null) return send.setPackage(preferred)
        return Intent.createChooser(send, pkg.title).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun isInstalled(context: Context, pkg: String) = runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
}
