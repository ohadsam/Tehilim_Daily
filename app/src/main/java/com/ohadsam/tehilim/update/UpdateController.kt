package com.ohadsam.tehilim.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.ohadsam.tehilim.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * In-app updates for sideloaded installs: ask GitHub for the latest release of this repository, download its APK into the
 * cache, check it really is a newer build of this app signed with the same key, then hand it to the system installer.
 * Android always asks the user to confirm the install.
 */
class UpdateController(
    private val context: Context,
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    private val installedBuild: Long,
    private val installedName: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var job: Job? = null
    private var downloaded: File? = null

    /** Bumped by [cancel] so a download that is still running stops writing state and files. */
    @Volatile private var generation = 0

    var autoCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO, value).apply()

    /** The version the user chose "later" for: not offered again automatically (a newer one still is). */
    private var dismissedBuild: Long
        get() = prefs.getLong(KEY_DISMISSED, 0)
        set(value) = prefs.edit().putLong(KEY_DISMISSED, value).apply()

    fun dismissOffer(info: UpdateInfo) {
        dismissedBuild = info.build ?: 0
    }

    /** True when an update was found and the user has not already declined this very build. */
    fun shouldOffer(info: UpdateInfo): Boolean = info.build == null || info.build != dismissedBuild

    /** Called on app start: at most one check a day, silent on failure. */
    fun autoCheckIfDue() {
        if (!autoCheck || clock() - prefs.getLong(KEY_LAST, 0) < DAY_MS) return
        if (_state.value !is UpdateState.Idle) return
        check(silent = true)
    }

    fun check(silent: Boolean = false) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        job = scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
            result.onSuccess { info ->
                prefs.edit().putLong(KEY_LAST, clock()).apply()
                _state.value = if (info != null && ReleaseParser.isNewer(info, installedBuild, installedName)) {
                    UpdateState.Available(info)
                } else {
                    UpdateState.UpToDate(clock())
                }
                logger.i(TAG, "check: latest=${info?.versionName} build=${info?.build} installed=$installedBuild")
            }.onFailure { e ->
                val error = (e as? UpdateException)?.error ?: UpdateError.SERVER
                logger.w(TAG, "check failed: $error ${e.message}")
                _state.value = if (silent) UpdateState.Idle else UpdateState.Failed(error)
            }
        }
    }

    fun download(info: UpdateInfo) {
        if (_state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Downloading(info, 0)
        val gen = ++generation
        job = scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { fetchApk(info, gen) } }
            if (gen != generation) return@launch
            result.onSuccess {
                downloaded = it
                _state.value = UpdateState.Ready(info)
                logger.i(TAG, "downloaded ${info.apkName}")
            }.onFailure { e ->
                val error = (e as? UpdateException)?.error ?: UpdateError.SERVER
                logger.w(TAG, "download failed: $error ${e.message}")
                clearDownloads()
                _state.value = UpdateState.Failed(error)
            }
        }
    }

    fun cancel() {
        generation++
        job?.cancel()
        clearDownloads()
        _state.value = UpdateState.Idle
    }

    fun reset() {
        if (_state.value is UpdateState.Failed || _state.value is UpdateState.UpToDate) _state.value = UpdateState.Idle
    }

    /** The intent that installs the downloaded file, or the settings page where "install unknown apps" is allowed for this app. */
    fun installIntent(): Intent? {
        val file = downloaded?.takeIf { it.exists() } ?: return null
        if (!context.packageManager.canRequestPackageInstalls()) {
            return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Frees the cache once the installed build is at least as new as the downloaded one (after the update, or on a stale file). */
    fun cleanupIfInstalled() {
        val dir = File(context.cacheDir, DIR)
        if (dir.exists() && _state.value !is UpdateState.Downloading && _state.value !is UpdateState.Ready) dir.deleteRecursively()
    }

    private fun clearDownloads() {
        downloaded = null
        File(context.cacheDir, DIR).deleteRecursively()
    }

    private fun fetchLatest(): UpdateInfo? {
        val request = Request.Builder().url(LATEST_URL).header("Accept", "application/vnd.github+json").build()
        try {
            client.newCall(request).execute().use { resp ->
                return when {
                    resp.code == 404 -> throw UpdateException(UpdateError.NOT_PUBLIC, "HTTP 404")
                    !resp.isSuccessful -> throw UpdateException(UpdateError.SERVER, "HTTP ${resp.code}")
                    else -> ReleaseParser.parse(resp.body?.string().orEmpty())
                }
            }
        } catch (e: IOException) {
            throw UpdateException(UpdateError.OFFLINE, e.message.orEmpty(), e)
        }
    }

    private fun fetchApk(info: UpdateInfo, gen: Int): File {
        val dir = File(context.cacheDir, DIR).apply { deleteRecursively(); mkdirs() }
        val file = File(dir, info.apkName)
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw UpdateException(UpdateError.SERVER, "HTTP ${resp.code}")
                val body = resp.body ?: throw UpdateException(UpdateError.SERVER, "empty body")
                val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                var done = 0L
                var lastPercent = -1
                body.byteStream().use { input ->
                    file.outputStream().buffered().use { out ->
                        val buf = ByteArray(BUFFER)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            if (gen != generation) throw UpdateException(UpdateError.OFFLINE, "cancelled")
                            out.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            done += n
                            val percent = if (total > 0) (done * 100 / total).toInt().coerceIn(0, 100) else 0
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _state.value = UpdateState.Downloading(info, percent)
                            }
                        }
                    }
                }
            }
        } catch (e: java.io.FileNotFoundException) {
            throw UpdateException(UpdateError.STORAGE, e.message.orEmpty(), e)
        } catch (e: IOException) {
            throw UpdateException(if (e.message?.contains("ENOSPC") == true) UpdateError.STORAGE else UpdateError.OFFLINE, e.message.orEmpty(), e)
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (info.sha256 != null && info.sha256 != actual) throw UpdateException(UpdateError.BAD_FILE, "checksum mismatch")
        UpdateVerifier.check(context, file, installedBuild)?.let { throw UpdateException(UpdateError.BAD_FILE, it) }
        return file
    }

    private class UpdateException(val error: UpdateError, message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        const val REPO = "ohadsam/Tehilim_Daily"
        private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
        const val APK_MIME = "application/vnd.android.package-archive"
        private const val DIR = "updates"
        private const val TAG = "Update"
        private const val KEY_AUTO = "auto"
        private const val KEY_LAST = "last_check"
        private const val KEY_DISMISSED = "dismissed_build"
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val BUFFER = 16 * 1024
    }
}
