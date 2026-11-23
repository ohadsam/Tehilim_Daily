package com.ohadsam.tehilim.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A published build the app could update to. [build] is the versionCode when the release names it (`…-b<code>.apk`). */
data class UpdateInfo(
    val versionName: String,
    val build: Long?,
    val apkUrl: String,
    val apkName: String,
    val sizeBytes: Long,
    val sha256: String?,
    val notes: String,
    val pageUrl: String,
)

/**
 * Reads GitHub's "latest release" answer. The release workflow attaches `tehilim-daily-<version>-b<versionCode>.apk` and
 * writes the What's New list into the release notes. Anything that does not look like that (draft, pre-release, no APK, a
 * download link outside github.com) is ignored so a stray release can never be offered as an update.
 */
object ReleaseParser {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val buildInName = Regex("-b(\\d+)\\.apk$")
    private val buildInTag = Regex("-b(\\d+)$")

    @Serializable
    private data class Asset(
        val name: String,
        val size: Long = 0,
        @SerialName("browser_download_url") val url: String,
        val digest: String? = null,
    )

    @Serializable
    private data class Release(
        @SerialName("tag_name") val tag: String,
        val body: String? = null,
        @SerialName("html_url") val page: String = "",
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<Asset> = emptyList(),
    )

    fun parse(text: String): UpdateInfo? {
        val r = try {
            json.decodeFromString(Release.serializer(), text)
        } catch (e: kotlinx.serialization.SerializationException) {
            return null
        }
        if (r.draft || r.prerelease) return null
        val apk = r.assets.firstOrNull { it.name.endsWith(".apk") && it.url.startsWith(TRUSTED_PREFIX) } ?: return null
        val build = buildInName.find(apk.name)?.groupValues?.get(1)?.toLongOrNull()
            ?: buildInTag.find(r.tag)?.groupValues?.get(1)?.toLongOrNull()
        val sha = apk.digest?.removePrefix("sha256:")?.lowercase()?.takeIf { it.length == 64 && it.all { c -> c in "0123456789abcdef" } }
        return UpdateInfo(
            versionName = r.tag.removePrefix("v").replace(buildInTag, ""),
            build = build,
            apkUrl = apk.url,
            apkName = apk.name,
            sizeBytes = apk.size,
            sha256 = sha,
            notes = r.body.orEmpty().trim(),
            pageUrl = r.page,
        )
    }

    /** Only downloads from GitHub itself are followed (the repository's release assets live under github.com). */
    const val TRUSTED_PREFIX = "https://github.com/"

    fun isNewer(info: UpdateInfo, installedBuild: Long, installedName: String): Boolean =
        info.build?.let { it > installedBuild } ?: (compareVersions(info.versionName, installedName) > 0)

    /** "0.18.0" vs "0.17.1": numeric, part by part; missing parts count as 0, non-numeric parts as 0. */
    fun compareVersions(a: String, b: String): Int {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }
}

enum class UpdateError {
    /** No connection / timeout. */
    OFFLINE,

    /** The releases page does not exist or is not public. */
    NOT_PUBLIC,

    /** Unexpected answer from the server (also rate limiting). */
    SERVER,

    /** Download is damaged, is not this app, is not newer, or is signed by someone else. */
    BAD_FILE,

    /** Could not write the file (storage full). */
    STORAGE,
}

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class UpToDate(val checkedAt: Long) : UpdateState

    data class Available(val info: UpdateInfo) : UpdateState

    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateState

    data class Ready(val info: UpdateInfo) : UpdateState

    data class Failed(val error: UpdateError) : UpdateState
}

/** True when two APKs are signed by exactly the same certificates (digests, order-independent). */
fun sameSigners(a: Collection<String>, b: Collection<String>): Boolean = a.isNotEmpty() && a.toSet() == b.toSet()
