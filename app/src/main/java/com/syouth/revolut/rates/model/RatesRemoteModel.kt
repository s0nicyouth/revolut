package com.syouth.revolut.rates.model

import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.dagger.WORKER_HANDLER
import com.syouth.revolut.net.RequestRepeaterFactory
import com.syouth.revolut.net.ResponseCallback
import okhttp3.Request
import java.io.Closeable
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named

private const val RATES_URL = "https://revolut.duckdns.org/latest?base=EUR"

/**
 * Fetches rates every 1 second with EUR as a base currency.
 * Json deserialization is quite slow so we need to do it on worker thread.
 */
@ViewScope
class RatesRemoteModel @Inject constructor(
    private val requestRepeaterFactory: RequestRepeaterFactory,
    private val moshi: Moshi,
    @Named(WORKER_HANDLER) private val workerHandler: Handler
) : ResponseCallback {

    private var currentRequest: Closeable? = null
    private val request = Runnable {
        currentRequest = requestRepeaterFactory.create(
            Request.Builder().url(RATES_URL).build(), this)
    }

    private val ratesRemoteSourceObservableInternal = object : MutableLiveData<RatesModelScheme>() {
        override fun onInactive() {
            workerHandler.post {
                workerHandler.removeCallbacks(request)
                currentRequest?.close()
            }
        }

        override fun onActive() {
            workerHandler.post(request)
        }

    }
    val ratesRemoteSourceObservable =
        ratesRemoteSourceObservableInternal as LiveData<RatesModelScheme>

    @WorkerThread
    override fun invoke(response: String) {
        check(workerHandler.looper == Looper.myLooper()) { throw IllegalThreadStateException("Should be called on worker thread") }

        try {
            moshi.adapter(RatesModelScheme::class.java).fromJson(response)?.let {
                ratesRemoteSourceObservableInternal.postValue(
                    RatesModelScheme(
                        it.base,
                        it.date,
                        linkedMapOf<String, BigDecimal>().apply {
                            put(it.base, 1.toBigDecimal())
                            putAll(it.rates)
                        }))
            }
        } catch (_: IOException) {}
        if (ratesRemoteSourceObservableInternal.hasActiveObservers()) {
            workerHandler.postDelayed(request, 1000)
        }
    }
}
