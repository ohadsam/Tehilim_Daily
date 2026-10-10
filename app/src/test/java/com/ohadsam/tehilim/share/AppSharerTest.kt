package com.ohadsam.tehilim.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class AppSharerTest {
    @Test
    fun messageMentionsVersionAndInPlaceUpdate() {
        val m = AppSharer.message("1.2.3")
        assertTrue(m.contains("1.2.3"))
        assertTrue(m.contains("אין צורך להסיר"))
        assertTrue(m.contains("ממקורות לא ידועים"))
    }

    @Test
    fun fileNames() {
        assertEquals("tehilim-daily-1.0.apk", AppSharer.fileName("1.0", false))
        assertEquals("tehilim-daily-1.0.apk.zip", AppSharer.fileName("1.0", true))
    }

    @Test
    fun zipContainsTheApkBytesUnchanged() {
        val dir = File.createTempFile("share", "").apply { delete(); mkdirs() }
        val apk = File(dir, "a.apk").apply { writeBytes(ByteArray(5000) { (it % 251).toByte() }) }
        val zip = File(dir, "a.zip")
        AppSharer.zipSingle(apk, zip)
        ZipFile(zip).use { z ->
            val e = z.getEntry("a.apk")
            assertEquals(apk.readBytes().toList(), z.getInputStream(e).readBytes().toList())
        }
        dir.deleteRecursively()
    }
}
