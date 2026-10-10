package com.ohadsam.tehilim.data

import com.ohadsam.tehilim.data.sefaria.SefariaApi
import com.ohadsam.tehilim.domain.PsalmErrorKind
import com.ohadsam.tehilim.domain.PsalmException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SefariaApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: SefariaApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        api = SefariaApi(OkHttpClient.Builder().retryOnConnectionFailure(false).build(), server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() = server.shutdown()

    private suspend fun failureOf(): Throwable =
        try {
            api.fetchChapter(1)
            error("expected failure")
        } catch (e: Exception) {
            e
        }

    private suspend fun assertKind(expected: PsalmErrorKind) {
        val e = failureOf()
        assertTrue("expected $expected but got $e", e is PsalmException && e.kind == expected)
    }

    @Test
    fun requestsTheDocumentedUrlAndParsesBody() = runTest {
        server.enqueue(MockResponse().setBody("""{"he":["אב"],"length":1}"""))
        val r = api.fetchChapter(119)
        assertEquals(listOf("אב"), r.verses)
        val req = server.takeRequest()
        assertEquals("/api/texts/Psalms.119?lang=he&commentary=0&context=0", req.path)
        assertTrue(req.getHeader("Accept")!!.contains("json"))
    }

    @Test
    fun http404IsNotFound() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertKind(PsalmErrorKind.NOT_FOUND)
    }

    @Test
    fun http503IsServerError() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        assertKind(PsalmErrorKind.SERVER)
    }

    @Test
    fun droppedConnectionIsOffline() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertKind(PsalmErrorKind.OFFLINE)
    }

    @Test
    fun invalidBodyIsInvalidData() = runTest {
        server.enqueue(MockResponse().setBody("""{"he":["x"]}"""))
        assertKind(PsalmErrorKind.INVALID_DATA)
    }
}
