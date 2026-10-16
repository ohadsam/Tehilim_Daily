package com.ohadsam.tehilim.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareCodecTest {
    private fun division(n: Int = 30) = SharePackage(
        id = "ab12cd34",
        title = "חלוקת תהילים לרפואת פלוני",
        kind = ShareKind.REFUAH,
        personName = "פלוני",
        motherName = "פלונית",
        parts = DivisionPlanner.divide((1..150).map { com.ohadsam.tehilim.domain.PsalmRef(it) }, n, DivisionMode.BY_LENGTH)
            .mapIndexed { i, refs -> SharePart(partLabel(i + 1, n), csvOf(refs)) },
    )

    @Test
    fun codeRoundTripsAndUsesOnlyChatSafeCharacters() {
        val pkg = division()
        val code = ShareCodec.toCode(pkg)
        assertTrue(code.startsWith("TD1:") && code.endsWith(":"))
        assertTrue("unsafe char in $code", code.removePrefix("TD1:").removeSuffix(":").all { it.isLetterOrDigit() || it == '+' || it == '.' })
        assertTrue("code too long: ${code.length}", code.length < 3000)
        assertEquals(ShareParse.Ok(pkg), ShareCodec.parse(code))
    }

    @Test
    fun findsTheCodeInsideAChatMessage() {
        val pkg = division(5)
        val message = "שלום!\n${ShareSender.message(pkg)}\nתודה רבה"
        assertEquals(ShareParse.Ok(pkg), ShareCodec.parse(message))
    }

    @Test
    fun fileJsonRoundTrips() {
        val pkg = division(3)
        assertEquals(ShareParse.Ok(pkg), ShareCodec.parse(ShareCodec.toJson(pkg)))
        assertEquals(ShareParse.Ok(pkg), ShareCodec.parse("﻿  " + ShareCodec.toJson(pkg) + "\n"))
    }

    @Test
    fun aDamagedCodeIsRejectedNotMisread() {
        val code = ShareCodec.toCode(division())
        val mid = code.length / 2
        val broken = code.substring(0, mid) + (if (code[mid] == 'A') 'B' else 'A') + code.substring(mid + 1)
        assertTrue(ShareCodec.parse(broken) is ShareParse.Invalid)
        assertTrue(ShareCodec.parse(code.dropLast(20) + ":") is ShareParse.Invalid)
    }

    @Test
    fun unrelatedTextIsNotAShare() {
        assertEquals(ShareParse.NotShare, ShareCodec.parse("hello"))
        assertEquals(ShareParse.NotShare, ShareCodec.parse(""))
        assertEquals(ShareParse.NotShare, ShareCodec.parse("{\"a\":1}"))
        assertEquals(ShareParse.NotShare, ShareCodec.parse("{ not json"))
    }

    @Test
    fun rejectsInvalidContent() {
        fun invalid(p: SharePackage) = ShareCodec.parse(ShareCodec.toJson(p)) is ShareParse.Invalid
        val ok = SharePackage(title = "t", parts = listOf(SharePart("", "1,2,119:1-96")))
        assertTrue(ShareCodec.parse(ShareCodec.toJson(ok)) is ShareParse.Ok)
        assertTrue(invalid(ok.copy(parts = listOf(SharePart("", "151")))))
        assertTrue(invalid(ok.copy(parts = listOf(SharePart("", "23:1-30"))))) // chapter 23 has 6 verses
        assertTrue(invalid(ok.copy(parts = listOf(SharePart("", "abc")))))
        assertTrue(invalid(ok.copy(parts = emptyList())))
        assertTrue(invalid(ok.copy(title = " ")))
        assertTrue(invalid(ok.copy(kind = ShareKind.ILUI_NISHMAT))) // needs a name
        assertTrue(invalid(ok.copy(version = SharePackage.VERSION + 1)))
        assertTrue(invalid(ok.copy(parts = List(SharePackage.MAX_PARTS + 1) { SharePart("", "1") })))
    }

    @Test
    fun fileAndMessageNames() {
        val pkg = division(30)
        assertEquals("tehilim-ab12cd34.tehilim", ShareSender.fileName(pkg))
        assertEquals("tehilim-ab12cd34-part7.tehilim", ShareSender.fileName(pkg.onlyPart(6)))
        assertTrue(ShareSender.message(pkg).contains("TD1:"))
        assertTrue(ShareSender.message(pkg).contains("30"))
    }

    @Test
    fun greetingNamesTheRecipientAndPartMessageHasOnlyThatPart() {
        val one = division(30).onlyPart(6)
        val text = ShareSender.message(one, "דן")
        assertTrue(text.startsWith("שלום דן,"))
        val parsed = (ShareCodec.parse(text) as ShareParse.Ok).pkg
        assertEquals(1, parsed.parts.size)
        assertEquals(division(30).parts[6], parsed.parts[0])
    }
}
