package com.syouth.revolut.rates.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.Moshi
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.net.RequestRepeaterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import java.math.BigDecimal
import java.math.MathContext
import javax.inject.Inject

private const val RATES_URL = "https://revolut.duckdns.org/latest?base=EUR"

/**
 * Fetches rates every 1 second with EUR as a base currency.
 * Json deserialization is quite slow so we need to do it on worker thread.
 */
@ViewScope
class RatesRemoteModel @Inject constructor(
    private val requestRepeaterFactory: RequestRepeaterFactory,
    private val moshi: Moshi) {

    private val context = CoroutineScope(Dispatchers.Unconfined)

    private var currentRequest: Job? = null

    private val ratesRemoteSourceObservableInternal = object : MutableLiveData<RatesModelScheme>() {
        override fun onInactive() {
            currentRequest?.cancel()
        }

        override fun onActive() = request()

    }
    val ratesRemoteSourceObservable =
        ratesRemoteSourceObservableInternal as LiveData<RatesModelScheme>

    private fun request() {
        currentRequest = context.launch {
            while (isActive) {
                val ratesModelScheme = moshi.adapter(RatesModelScheme::class.java).fromJson(
                    requestRepeaterFactory.create(Request.Builder().url(RATES_URL).build()))!!
                ratesRemoteSourceObservableInternal.postValue(
                    RatesModelScheme(
                        ratesModelScheme.base,
                        ratesModelScheme.date,
                        linkedMapOf<String, BigDecimal>().apply {
                            put(ratesModelScheme.base, 1.toBigDecimal(MathContext.DECIMAL128))
                            putAll(ratesModelScheme.rates)
                        }))
                delay(1000)
            }
        }
    }
}
