package com.syouth.revolut.rates.model

import android.os.Build
import android.os.Handler
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.syouth.revolut.net.FakeCall
import com.syouth.revolut.net.RequestRepeaterFactory
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.TimeUnit

private const val TEST_URL = "https://revolut.duckdns.org/latest?base=EUR"
private const val RESPONSE_BODY = "{\"base\":\"EUR\",\"date\":\"2018-09-06\",\"rates\":{\"AUD\":1.6085,\"BGN\":1.9463,\"BRL\":4.7685,\"CAD\":1.5263,\"CHF\":1.122,\"CNY\":7.9065,\"CZK\":25.59,\"DKK\":7.4204,\"GBP\":0.89387,\"HKD\":9.088,\"HRK\":7.398,\"HUF\":324.9,\"IDR\":17239.0,\"ILS\":4.1503,\"INR\":83.31,\"ISK\":127.18,\"JPY\":128.92,\"KRW\":1298.4,\"MXN\":22.257,\"MYR\":4.7886,\"NOK\":9.7285,\"NZD\":1.7547,\"PHP\":62.288,\"PLN\":4.2973,\"RON\":4.6159,\"RUB\":79.188,\"SEK\":10.539,\"SGD\":1.5922,\"THB\":37.945,\"TRY\":7.5911,\"USD\":1.1577,\"ZAR\":17.737}}"

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class RatesRemoteModelTest {

    private lateinit var ratesRemoteModel: RatesRemoteModel

    @Before
    fun before() {
        val request = Request.Builder().url(TEST_URL).build()
        val fakeCall = FakeCall(
            request,
            false,
            Response.Builder()
                .body(RESPONSE_BODY.toResponseBody())
                .code(200)
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("Ok")
                .build())
        val client = mock<OkHttpClient> {
            on { newCall(any()) } doReturn fakeCall
        }
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(BigDecimalAdapter())
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .build()
        val requestRepeaterFactory = RequestRepeaterFactory(client)
        ratesRemoteModel = RatesRemoteModel(requestRepeaterFactory, moshi, Handler())
    }

    @Test
    fun testModelAssembledCorrect() {
        var called = false
        ratesRemoteModel.ratesRemoteSourceObservable.observeForever {
            Assert.assertEquals(33, it.rates.size)
            Assert.assertNotNull(it.rates["EUR"])
            called = true
        }
        Robolectric.getBackgroundThreadScheduler().advanceToLastPostedRunnable()
        Robolectric.getForegroundThreadScheduler().advanceBy(1000, TimeUnit.MILLISECONDS)

        Assert.assertTrue(called)
    }
}
