package com.ohadsam.tehilim.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest

/** Refuses any downloaded file that is not a newer build of this very app signed with the key of the installed one. */
object UpdateVerifier {
    /** Null when the file may be installed, otherwise the reason (English, for the log). */
    fun check(context: Context, file: File, installedBuild: Long): String? {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.path, FLAGS) ?: return "not an installable package"
        if (archive.packageName != context.packageName) return "different package ${archive.packageName}"
        if (PackageInfoCompat.getLongVersionCode(archive) <= installedBuild) return "not newer (installed build $installedBuild)"
        val installed = pm.getPackageInfo(context.packageName, FLAGS)
        return if (sameSigners(signers(archive), signers(installed))) null else "signed with a different key"
    }

    @Suppress("DEPRECATION")
    private val FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): List<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo?.apkContentsSigners else info.signatures
        return sigs.orEmpty().map { s ->
            MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
