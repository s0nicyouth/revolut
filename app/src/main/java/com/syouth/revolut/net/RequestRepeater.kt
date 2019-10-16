package com.syouth.revolut.net

import android.os.Handler
import android.os.Looper
import com.syouth.revolut.dagger.NetworkScope
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.Closeable
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random

/**
 * Implements exponential retry strategy to ensure that request is completed and
 * servers are not thrown to knockdown.
 * Returns response on the same thread it was called from.
 * It's guaranteed that after [RequestRepeater.close] no callbacks will be called.
 */

private const val MAX_DELAY = 2L * 60L * 1000L
private const val MIN_DELAY = 1000L
private const val DELAY_FACTOR = 1.5F
private const val UNCERTAINTY = 0.1F

typealias ResponseCallback = (response: String) -> Unit

@NetworkScope
class RequestRepeaterFactory @Inject constructor(private val client: OkHttpClient) {
    fun create(request: Request,
               callback: ResponseCallback) = RequestRepeater(request, callback, client) as Closeable
}

private class RequestRepeater(
    request: Request,
    private val callback: ResponseCallback,
    private val client: OkHttpClient) : Closeable, Callback {

    private var delay = MIN_DELAY
    private var currentCall: Call = client.newCall(request)
    private val threadHandler = Handler()
    private var closed = false

    init {
        currentCall.enqueue(this)
    }

    override fun onFailure(call: Call, e: IOException) = retry(call)

    override fun onResponse(call: Call, response: Response) = response.body?.use { body ->
        if (response.isSuccessful) {
            val bodyString = body.string()
            threadHandler.post {
                if (!closed) callback(bodyString)
            }
            Unit
        } else retry(call)
    } ?: retry(call)

    override fun close() {
        check(Looper.myLooper() == threadHandler.looper) { throw RuntimeException("Close should be called on the same thread as it was created on") }

        if (closed) return

        closed = true
        currentCall.cancel()
    }

    private fun retry(call: Call) {
        threadHandler.postDelayed({
            if (closed) return@postDelayed
            currentCall = client.newCall(call.request().newBuilder().build())
            currentCall.enqueue(this)

            delay = minOf(MAX_DELAY, (delay * DELAY_FACTOR).toLong())
            val uncertainty = (delay * UNCERTAINTY).toLong()
            delay += Random.nextLong(-uncertainty, uncertainty)
        }, delay)
    }
}
