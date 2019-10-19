package com.syouth.revolut.net

import com.syouth.revolut.dagger.NetworkScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
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

@NetworkScope
class RequestRepeaterFactory @Inject constructor(private val client: OkHttpClient) {
    suspend fun create(request: Request) = request(request, client)
}

private suspend fun request(request: Request, client: OkHttpClient) = withContext(Dispatchers.IO) {
    var delay = MIN_DELAY
    while (isActive) {
        try {
            val response = client.newCall(request).execute()
            response.body?.use { if (response.isSuccessful && isActive) return@withContext it.string() }
            if (!isActive) yield()
        } catch (_: IOException) {}
        kotlinx.coroutines.delay(delay)

        delay = minOf(MAX_DELAY, (delay * DELAY_FACTOR).toLong())
        val uncertanity = (delay * UNCERTAINTY).toLong()
        delay += Random.nextLong(-uncertanity, uncertanity)
    }

    throw RuntimeException("Should not get here")
}
