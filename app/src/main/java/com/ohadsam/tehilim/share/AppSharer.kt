package com.ohadsam.tehilim.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Shares the installed app itself (its APK) plus Hebrew install/update instructions, e.g. via WhatsApp or e-mail. */
object AppSharer {
    const val APK_MIME = "application/vnd.android.package-archive"
    private const val WHATSAPP = "com.whatsapp"
    private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    private const val GMAIL = "com.google.android.gm"

    fun message(versionName: String): String = """
        שלום! זו אפליקציית "תהילים יומי" (גרסה $versionName) — תהילים לפי יום בחודש העברי, מועדפים, תזכורות וווידג'טים.

        איך מתקינים:
        1. פותחים את הקובץ המצורף (אם הגיע כקובץ zip — קודם מחלצים אותו).
        2. אם מתבקש, מאשרים "התקנה ממקורות לא ידועים" לאפליקציה שממנה פתחתם את הקובץ.
        3. לוחצים "התקן" ואז "פתח".

        עדכון לגרסה חדשה: מתקינים את הקובץ החדש ישירות מעל הקיים — אין צורך להסיר את האפליקציה, וכל המועדפים, הסימניות וההגדרות נשמרים.
    """.trimIndent()

    /** The build number is part of the name: two builds with the same versionName must never look like the same file. */
    fun fileName(versionName: String, zip: Boolean, build: Long? = null) =
        "tehilim-daily-$versionName" + (build?.let { "-b$it" } ?: "") + ".apk" + if (zip) ".zip" else ""

    /** Copies the installed APK into the cache (shared through FileProvider); optionally wrapped in a zip for mail apps that block .apk. */
    fun prepare(context: Context, versionName: String, zip: Boolean): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the latest copy
        val source = File(context.applicationInfo.sourceDir)
        val installed = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        val apk = File(dir, fileName(versionName, false, installed))
        source.copyTo(apk, overwrite = true)
        // Never hand out a file that is not the build that is running now.
        val copied = context.packageManager.getPackageArchiveInfo(apk.path, 0)?.longVersionCode
        check(copied == installed) { "shared APK build $copied differs from the installed build $installed" }
        if (!zip) return apk
        val zipFile = File(dir, fileName(versionName, true, installed))
        zipSingle(apk, zipFile)
        apk.delete()
        return zipFile
    }

    fun zipSingle(src: File, dest: File) {
        ZipOutputStream(dest.outputStream().buffered()).use { out ->
            out.putNextEntry(ZipEntry(src.name))
            src.inputStream().use { it.copyTo(out) }
            out.closeEntry()
        }
    }

    enum class Target { WHATSAPP, EMAIL, ANY }

    fun intent(context: Context, file: File, versionName: String, zip: Boolean, target: Target): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(if (zip) "application/zip" else APK_MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "תהילים יומי $versionName")
            .putExtra(Intent.EXTRA_TEXT, message(versionName))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val preferred = when (target) {
            Target.WHATSAPP -> listOf(WHATSAPP, WHATSAPP_BUSINESS)
            Target.EMAIL -> listOf(GMAIL)
            Target.ANY -> emptyList()
        }.firstOrNull { isInstalled(context, it) }
        if (preferred != null) return send.setPackage(preferred)
        return Intent.createChooser(send, "שיתוף האפליקציה").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun isInstalled(context: Context, pkg: String) = runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

}
