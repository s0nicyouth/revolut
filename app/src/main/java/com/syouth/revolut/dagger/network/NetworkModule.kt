package com.syouth.revolut.dagger.network

import com.syouth.revolut.dagger.NetworkScope
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient

@Module
object NetworkModule {

    @Provides
    @JvmStatic
    @NetworkScope
    fun okHttp() = OkHttpClient()
}
