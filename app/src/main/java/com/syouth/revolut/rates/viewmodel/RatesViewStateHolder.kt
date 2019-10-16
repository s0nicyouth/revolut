package com.syouth.revolut.rates.viewmodel

import android.os.Bundle
import com.squareup.moshi.Moshi
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.rates.model.RatesModelScheme
import java.math.BigDecimal
import java.math.MathContext
import java.util.Currency
import javax.inject.Inject

private const val STATE_RATES_MODEL = "rates_model"
private const val STATE_RATES_LIST = "rates_list"

/**
 * Holds and updates view state by processing new data arrived.
 */
@ViewScope
class RatesViewStateHolder @Inject constructor(private val moshi: Moshi) {

    private val rates = mutableListOf<Rate>()
    private var lastRateModel: RatesModelScheme? = null

    fun updateRates(ratesModel: RatesModelScheme): RatesViewState {
        lastRateModel = ratesModel
        return if (rates.isEmpty()) init(lastRateModel!!) else update(lastRateModel!!)
    }

    fun bringToTop(currency: String): RatesViewState {
        val index = rates.indexOfFirst { it.shortName == currency }
        if (index == -1 || index == 0) return RatesViewState.Rates(RatesData(rates, emptyList()))

        val diff = mutableListOf<DiffOperation>()
        rates.add(0, rates.removeAt(index))

        diff += DiffOperation.Move(index, 0)
        // All data should be also updated since move is a structural change
        for (i in 0 until rates.size) {
            diff += DiffOperation.Change(i)
        }


        return RatesViewState.Rates(RatesData(rates, diff))
    }

    fun updateTopCurrencyValue(value: BigDecimal) = lastRateModel?.let {
        val top = rates[0]
        rates[0] = Rate(getFlagDrawable(top.shortName), top.shortName, top.fullName, value)
        update(it)
    }

    private fun init(ratesModel: RatesModelScheme): RatesViewState {
        val diff = mutableListOf<DiffOperation>()

        var index = 0
        for ((shortName, rate) in ratesModel.rates) {

            rates += Rate(
                getFlagDrawable(shortName),
                shortName,
                Currency.getInstance(shortName).displayName,
                rate)
            diff += DiffOperation.Insert(index++)
        }

        return RatesViewState.Rates(RatesData(rates, diff))
    }

    private fun getFlagDrawable(currency: String) = currency.toLowerCase().let {
        val id = "flag_$it"
        if (id !in flags) null else flags[id]!!
    }

    private fun update(ratesModel: RatesModelScheme): RatesViewState {
        val mathContext = MathContext.DECIMAL128
        val exchanging = rates[0]

        val inBaseCurrency = if (exchanging.shortName !in ratesModel.rates) {
            return RatesViewState.Error
        } else {
            try {
                1.toBigDecimal()
                    .divide(ratesModel.rates[exchanging.shortName]!!, mathContext)
                    .multiply(exchanging.sum, mathContext)
            } catch (_: ArithmeticException) { return RatesViewState.Error }
        }

        val diff = mutableListOf<DiffOperation>()
        val newRates = mutableListOf<Rate>()

        newRates += exchanging

        for (i in 1 until rates.size) {
            val cur = rates[i]
            // If it's not in rates for some reason just skip it.
            // Also it may be a good idea to warn user that the currency is not supported anymore.
            if (cur.shortName in ratesModel.rates) {
                newRates += Rate(
                    getFlagDrawable(cur.shortName),
                    cur.shortName,
                    cur.fullName,
                    inBaseCurrency.multiply(ratesModel.rates[cur.shortName]!!, mathContext))
                diff += DiffOperation.Change(i)
            }
        }

        rates.clear()
        rates.addAll(newRates)
        return RatesViewState.Rates(RatesData(rates, diff))
    }

    fun saveState(outState: Bundle) = lastRateModel?.let {
        outState.putString(STATE_RATES_MODEL, moshi.adapter(RatesModelScheme::class.java).toJson(it))
        outState.putParcelableArrayList(STATE_RATES_LIST, ArrayList<Rate>().apply { addAll(rates) })
    }

    fun restoreState(savedState: Bundle?) = savedState?.let { state ->
        val model = state.getString(STATE_RATES_MODEL)?.let {
            moshi.adapter(RatesModelScheme::class.java).fromJson(it)
        }
        val ratesList = state.getParcelableArrayList<Rate>(STATE_RATES_LIST)

        if (model != null && ratesList != null) {
            rates.clear()
            rates.addAll(ratesList)
            lastRateModel = model
            return@let update(lastRateModel!!)
        } else {
            return@let RatesViewState.Empty
        }
    } ?: RatesViewState.Empty
}
