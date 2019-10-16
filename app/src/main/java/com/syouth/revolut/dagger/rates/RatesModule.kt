package com.syouth.revolut.dagger.rates

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.dagger.WORKER_HANDLER
import com.syouth.revolut.dagger.WORKER_THREAD
import com.syouth.revolut.rates.model.BigDecimalAdapter
import com.syouth.revolut.rates.model.RatesRemoteModel
import com.syouth.revolut.rates.viewmodel.RatesViewModel
import com.syouth.revolut.rates.viewmodel.RatesViewStateHolder
import dagger.Binds
import dagger.Module
import dagger.Provides
import java.util.Date
import javax.inject.Named

@Module
abstract class RatesModule {

    @Binds
    @ViewScope
    abstract fun lifecycleOwner(activity: AppCompatActivity): LifecycleOwner

    @Binds
    @ViewScope
    abstract fun context(activity: AppCompatActivity): Context

    @Module
    companion object {

        @Provides
        @JvmStatic
        @ViewScope
        @Named(WORKER_THREAD)
        fun computeThread() = HandlerThread(WORKER_THREAD).apply { start() }

        @Provides
        @JvmStatic
        @ViewScope
        @Named(WORKER_HANDLER)
        fun computeHandler(@Named(WORKER_THREAD) thread: HandlerThread) = Handler(thread.looper)

        @Provides
        @JvmStatic
        @ViewScope
        fun moshi() = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(BigDecimalAdapter())
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .build()!!

        @Provides
        @JvmStatic
        @ViewScope
        fun viewModel(
            activity: AppCompatActivity,
            ratesRemoteModel: RatesRemoteModel,
            ratesViewStateHolder: RatesViewStateHolder,
            savedState: Bundle?) = ViewModelProviders
            .of(activity,
                ViewModelFactory(ratesRemoteModel, ratesViewStateHolder, savedState))[RatesViewModel::class.java]

        @Provides
        @JvmStatic
        @ViewScope
        fun inputMethodManager(context: Context)  =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
}

@Suppress("UNCHECKED_CAST")
private class ViewModelFactory(
    private val ratesRemoteModel: RatesRemoteModel,
    private val ratesViewStateHolder: RatesViewStateHolder,
    private val savedState: Bundle?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        check(RatesViewModel::class.java == modelClass) { throw IllegalStateException("Can construct only RatesViewModel") }

        return RatesViewModel(ratesRemoteModel, ratesViewStateHolder, savedState) as T
    }

}
