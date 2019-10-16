package com.syouth.revolut.net

import android.os.Build
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class RequestRepeaterTest {

    private val url = "http://google.com"
    private val responseBody = "{\"base\":\"EUR\",\"date\":\"2018-09-06\",\"rates\":{\"AUD\":1,\"BGN\":6, \"BRL\":0.5}}"

    @Test
    fun testGoodResponse() {
        val request = Request.Builder().url(url).build()
        val fakeCall = FakeCall(
            request,
            false,
            Response.Builder()
                .body(responseBody.toResponseBody())
                .code(200)
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("Ok")
                .build())
        val client = mock<OkHttpClient> {
            on { newCall(any()) } doReturn fakeCall
        }
        val requestRepeaterFactory = RequestRepeaterFactory(client)

        var gotResponse: String? = null
        requestRepeaterFactory.create(request) {
            gotResponse = it
        }
        Robolectric.getForegroundThreadScheduler().advanceBy(1000, TimeUnit.MILLISECONDS)
        Assert.assertEquals(responseBody, gotResponse)
    }

    @Test
    fun testBadResponse() {
        val request = Request.Builder().url(url).build()
        val fakeCall = spy(FakeCall(
            request,
            true,
            Response.Builder()
                .body(responseBody.toResponseBody())
                .code(200)
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("Ok")
                .build()))
        val client = mock<OkHttpClient> {
            on { newCall(any()) } doReturn fakeCall
        }

        val requestRepeaterFactory = RequestRepeaterFactory(client)

        var gotResponse: String? = null
        requestRepeaterFactory.create(request) {
            gotResponse = it
        }

        Robolectric.getForegroundThreadScheduler().advanceBy(1200, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(2500, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(3000, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(4000, TimeUnit.MILLISECONDS)


        Assert.assertNull(gotResponse)
        // We should have 4 repeats considering exponential repeat strategy.
        verify(fakeCall, times(4)).request()
    }

    @Test
    fun testNullResponse() {
        val request = Request.Builder().url(url).build()
        val fakeCall = spy(FakeCall(
            request,
            false,
            Response.Builder()
                .body(null)
                .code(200)
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("Ok")
                .build()))
        val client = mock<OkHttpClient> {
            on { newCall(any()) } doReturn fakeCall
        }

        val requestRepeaterFactory = RequestRepeaterFactory(client)

        var gotResponse: String? = null
        requestRepeaterFactory.create(request) {
            gotResponse = it
        }

        Robolectric.getForegroundThreadScheduler().advanceBy(1200, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(2500, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(3000, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(4000, TimeUnit.MILLISECONDS)


        Assert.assertNull(gotResponse)
        // We should have 4 repeats considering exponential repeat strategy.
        verify(fakeCall, times(4)).request()
    }

    @Test
    fun testErrorResponse() {
        val request = Request.Builder().url(url).build()
        val fakeCall = spy(FakeCall(
            request,
            false,
            Response.Builder()
                .body(responseBody.toResponseBody())
                .code(300)
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("Ok")
                .build()))
        val client = mock<OkHttpClient> {
            on { newCall(any()) } doReturn fakeCall
        }

        val requestRepeaterFactory = RequestRepeaterFactory(client)

        var gotResponse: String? = null
        requestRepeaterFactory.create(request) {
            gotResponse = it
        }

        Robolectric.getForegroundThreadScheduler().advanceBy(1200, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(2500, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(3000, TimeUnit.MILLISECONDS)
        Robolectric.getForegroundThreadScheduler().advanceBy(4000, TimeUnit.MILLISECONDS)


        Assert.assertNull(gotResponse)
        // We should have 4 repeats considering exponential repeat strategy.
        verify(fakeCall, times(4)).request()
    }

    @Test
    fun testClose() {
        val request = Request.Builder().url(url).build()
        val fakeCall = spy(FakeCall(
            request,
            false,
            Response.Builder()
                .body(responseBody.toResponseBody())
                .code(200)
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("Ok")
                .build()))
        val client = mock<OkHttpClient> {
            on { newCall(any()) } doReturn fakeCall
        }
        val requestRepeaterFactory = RequestRepeaterFactory(client)

        var gotResponse: String? = null
        val disposable = requestRepeaterFactory.create(request) {
            gotResponse = it
        }
        disposable.close()
        Robolectric.getForegroundThreadScheduler().advanceBy(5000, TimeUnit.MILLISECONDS)
        Assert.assertNull(gotResponse)
        verify(fakeCall, times(1)).cancel()
    }
}
