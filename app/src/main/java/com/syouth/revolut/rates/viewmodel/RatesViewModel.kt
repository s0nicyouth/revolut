package com.syouth.revolut.rates.viewmodel

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.rates.model.RatesRemoteModel
import java.math.BigDecimal

@ViewScope
class RatesViewModel constructor(
    private val ratesRemoteModel: RatesRemoteModel,
    private val ratesViewStateHolder: RatesViewStateHolder,
    savedState: Bundle?): ViewModel() {

    val ratesObservable
        get() = ratesObservableInternal as LiveData<RatesViewState>

    private val ratesObservableInternal = MediatorLiveData<RatesViewState>().apply {
        addSource(ratesRemoteModel.ratesRemoteSourceObservable) {
            value = ratesViewStateHolder.updateRates(it)
        }
    }.apply {
        value = ratesViewStateHolder.restoreState(savedState)
    }

    fun bringToTop(currency: String) {
        ratesObservableInternal.value = ratesViewStateHolder.bringToTop(currency)
    }

    fun updateTopCurrencyValue(value: BigDecimal) {
        ratesObservableInternal.value = ratesViewStateHolder.updateTopCurrencyValue(value)
    }

    fun saveState(outState: Bundle) = ratesViewStateHolder.saveState(outState)
}
