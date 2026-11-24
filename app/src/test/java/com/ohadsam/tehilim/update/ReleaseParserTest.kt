package com.ohadsam.tehilim.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {
    private val sha = "a".repeat(64)

    private fun release(
        tag: String = "v0.18.0-b310",
        assets: String = """[{"name":"tehilim-daily-0.18.0-b310.apk","size":12345678,
            "browser_download_url":"https://github.com/ohadsam/Tehilim_Daily/releases/download/v0.18.0-b310/tehilim-daily-0.18.0-b310.apk",
            "digest":"sha256:$sha"}]""",
        extra: String = "",
    ) = """{"tag_name":"$tag","html_url":"https://github.com/ohadsam/Tehilim_Daily/releases/tag/$tag","body":"- new thing\n- other",
        "draft":false,"prerelease":false,"assets":$assets $extra,"unknown":{"x":1}}"""

    @Test
    fun readsVersionBuildAssetAndChecksum() {
        val info = ReleaseParser.parse(release())!!
        assertEquals("0.18.0", info.versionName)
        assertEquals(310L, info.build)
        assertEquals("tehilim-daily-0.18.0-b310.apk", info.apkName)
        assertEquals(12345678L, info.sizeBytes)
        assertEquals(sha, info.sha256)
        assertEquals("- new thing\n- other", info.notes)
    }

    @Test
    fun buildFallsBackToTheTagAndChecksumIsOptional() {
        val assets = """[{"name":"app.apk","browser_download_url":"https://github.com/o/r/releases/download/t/app.apk"}]"""
        val info = ReleaseParser.parse(release(assets = assets))!!
        assertEquals(310L, info.build)
        assertNull(info.sha256)
        assertNull(ReleaseParser.parse(release(tag = "v0.18.0", assets = assets))!!.build)
    }

    @Test
    fun ignoresDraftsPrereleasesNonApkAndForeignDownloadLinks() {
        assertNull(ReleaseParser.parse(release().replace("\"draft\":false", "\"draft\":true")))
        assertNull(ReleaseParser.parse(release().replace("\"prerelease\":false", "\"prerelease\":true")))
        assertNull(ReleaseParser.parse(release(assets = """[{"name":"x.aab","browser_download_url":"https://github.com/o/r/x.aab"}]""")))
        assertNull(ReleaseParser.parse(release(assets = """[{"name":"x.apk","browser_download_url":"https://evil.example.com/x.apk"}]""")))
        assertNull(ReleaseParser.parse(release(assets = """[{"name":"x.apk","browser_download_url":"http://github.com/o/r/x.apk"}]""")))
        assertNull(ReleaseParser.parse("not json"))
        assertNull(ReleaseParser.parse("{}"))
    }

    @Test
    fun badChecksumIsDroppedNotTrusted() {
        val assets = """[{"name":"a-b5.apk","browser_download_url":"https://github.com/o/r/a.apk","digest":"sha256:xyz"}]"""
        assertNull(ReleaseParser.parse(release(assets = assets))!!.sha256)
    }

    @Test
    fun newerIsDecidedByBuildNumberThenByVersionName() {
        val withBuild = ReleaseParser.parse(release())!!
        assertTrue(ReleaseParser.isNewer(withBuild, installedBuild = 309, installedName = "9.9.9"))
        assertFalse(ReleaseParser.isNewer(withBuild, installedBuild = 310, installedName = "0.1.0"))
        assertFalse(ReleaseParser.isNewer(withBuild, installedBuild = 400, installedName = "0.1.0"))
        val noBuild = withBuild.copy(build = null)
        assertTrue(ReleaseParser.isNewer(noBuild, 999, "0.17.9"))
        assertFalse(ReleaseParser.isNewer(noBuild, 1, "0.18.0"))
    }

    @Test
    fun versionComparison() {
        assertTrue(ReleaseParser.compareVersions("0.18.0", "0.17.1") > 0)
        assertTrue(ReleaseParser.compareVersions("0.9.0", "0.10.0") < 0)
        assertEquals(0, ReleaseParser.compareVersions("1.0", "1.0.0"))
    }

    @Test
    fun signersMustMatchExactly() {
        assertTrue(sameSigners(listOf("a", "b"), listOf("b", "a")))
        assertFalse(sameSigners(listOf("a"), listOf("b")))
        assertFalse(sameSigners(emptyList(), emptyList()))
        assertFalse(sameSigners(listOf("a"), listOf("a", "b")))
    }
}
