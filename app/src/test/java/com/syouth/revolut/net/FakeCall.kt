package com.syouth.revolut.net

import android.os.Handler
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import java.io.IOException

open class FakeCall(
    private val request: Request,
    private val fail: Boolean,
    private val response: Response) : Call {

    private var executed = false
    private var cancelled = false

    override fun cancel() {
        cancelled = true
    }

    override fun clone() = this

    override fun enqueue(responseCallback: Callback) {
        Handler().postDelayed({
            if (!fail) {
                responseCallback.onResponse(this, response)
            } else {
                responseCallback.onFailure(this, IOException())
            }
        }, 100)
    }

    override fun execute(): Response {
        executed = true
        return response
    }

    override fun isCanceled() = cancelled

    override fun isExecuted() = executed

    override fun request() = request.newBuilder().build()

    override fun timeout() = Timeout()

}
