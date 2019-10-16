package com.syouth.revolut.rates.viewmodel

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.syouth.revolut.rates.model.BigDecimalAdapter
import com.syouth.revolut.rates.model.RatesModelScheme
import org.junit.Assert
import org.junit.Test
import java.math.MathContext
import java.math.RoundingMode
import java.util.Date

class RatesViewStateHolderTest {

    private val initialScheme = "{\"base\":\"EUR\",\"date\":\"2018-09-06\",\"rates\":{\"AUD\":1.6085,\"BGN\":1.9463,\"BRL\":4.7685,\"CAD\":1.5263,\"CHF\":1.122,\"CNY\":7.9065,\"CZK\":25.59,\"DKK\":7.4204,\"GBP\":0.89387,\"HKD\":9.088,\"HRK\":7.398,\"HUF\":324.9,\"IDR\":17239.0,\"ILS\":4.1503,\"INR\":83.31,\"ISK\":127.18,\"JPY\":128.92,\"KRW\":1298.4,\"MXN\":22.257,\"MYR\":4.7886,\"NOK\":9.7285,\"NZD\":1.7547,\"PHP\":62.288,\"PLN\":4.2973,\"RON\":4.6159,\"RUB\":79.188,\"SEK\":10.539,\"SGD\":1.5922,\"THB\":37.945,\"TRY\":7.5911,\"USD\":1.1577,\"ZAR\":17.737}}"
    private val nextScheme = "{\"base\":\"EUR\",\"date\":\"2018-09-06\",\"rates\":{\"AUD\":1.6223,\"BGN\":1.9629,\"BRL\":4.8093,\"CAD\":1.5394,\"CHF\":1.1316,\"CNY\":7.9741,\"CZK\":25.809,\"DKK\":7.4839,\"GBP\":0.90152,\"HKD\":9.1657,\"HRK\":7.4612,\"HUF\":327.68,\"IDR\":17387.0,\"ILS\":4.1858,\"INR\":84.023,\"ISK\":128.27,\"JPY\":130.02,\"KRW\":1309.5,\"MXN\":22.447,\"MYR\":4.8295,\"NOK\":9.8116,\"NZD\":1.7697,\"PHP\":62.82,\"PLN\":4.334,\"RON\":4.6554,\"RUB\":79.865,\"SEK\":10.629,\"SGD\":1.6058,\"THB\":38.269,\"TRY\":7.656,\"USD\":1.1676,\"ZAR\":17.888}}"
    private val conversionScheme = "{\"base\":\"EUR\",\"date\":\"2018-09-06\",\"rates\":{\"AUD\":1,\"BGN\":6, \"BRL\":0.5}}"
    private val conversionSchemeWithZero = "{\"base\":\"EUR\",\"date\":\"2018-09-06\",\"rates\":{\"AUD\":0,\"BGN\":6, \"BRL\":0.5}}"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(BigDecimalAdapter())
        .add(Date::class.java, Rfc3339DateJsonAdapter())
        .build()

    private val ratesViewStateHolder = RatesViewStateHolder(moshi)

    @Test
    fun testInit() {
        val initialScheme = moshi.adapter(RatesModelScheme::class.java).fromJson(initialScheme)!!

        val result = ratesViewStateHolder.updateRates(initialScheme) as RatesViewState.Rates

        Assert.assertEquals(32, result.ratesData.rates.size)
        result.ratesData.diff.forEach {
            Assert.assertEquals(DiffOperation.Insert::class, it::class)
        }
    }

    @Test
    fun testUpdate() {
        val initialScheme = moshi.adapter(RatesModelScheme::class.java).fromJson(initialScheme)!!
        val nextScheme = moshi.adapter(RatesModelScheme::class.java).fromJson(nextScheme)!!

        ratesViewStateHolder.updateRates(initialScheme)
        val result = ratesViewStateHolder.updateRates(nextScheme) as RatesViewState.Rates

        Assert.assertEquals(32, result.ratesData.rates.size)
        Assert.assertEquals(31, result.ratesData.diff.size)
        for (element in result.ratesData.diff) {
            Assert.assertEquals(DiffOperation.Change::class, element::class)
        }
    }

    @Test
    fun tesBringToTop() {
        val initialScheme = moshi.adapter(RatesModelScheme::class.java).fromJson(initialScheme)!!

        ratesViewStateHolder.updateRates(initialScheme)
        val result = ratesViewStateHolder.bringToTop("RUB") as RatesViewState.Rates

        Assert.assertEquals(32, result.ratesData.rates.size)
        Assert.assertEquals("RUB", result.ratesData.rates[0].shortName)
        Assert.assertEquals(DiffOperation.Move::class, result.ratesData.diff[0]::class)
        for (i in 1 until result.ratesData.diff.size) {
            val cur = result.ratesData.diff[i]
            Assert.assertEquals(DiffOperation.Change::class, cur::class)
        }
    }

    @Test
    fun testUpdateTopCurrency() {
        val initialScheme = moshi.adapter(RatesModelScheme::class.java).fromJson(initialScheme)!!

        ratesViewStateHolder.updateRates(initialScheme)
        val result = ratesViewStateHolder.updateTopCurrencyValue(10.10.toBigDecimal(MathContext.DECIMAL128)) as RatesViewState.Rates

        Assert.assertEquals(32, result.ratesData.rates.size)
        Assert.assertEquals(10.10.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[0].sum)
        Assert.assertEquals(31, result.ratesData.diff.size)
        for (element in result.ratesData.diff) {
            Assert.assertEquals(DiffOperation.Change::class, element::class)
        }
    }

    @Test
    fun testConversion() {
        val conversionScheme = moshi.adapter(RatesModelScheme::class.java).fromJson(conversionScheme)!!
        val conversionSchemeWithZero = moshi.adapter(RatesModelScheme::class.java).fromJson(conversionSchemeWithZero)!!

        run {
            val ratesStateHolderZeroDivision = RatesViewStateHolder(moshi)
            ratesStateHolderZeroDivision.updateRates(conversionSchemeWithZero) as RatesViewState.Rates
            val result = ratesStateHolderZeroDivision.updateTopCurrencyValue(10.toBigDecimal(MathContext.DECIMAL128)) as RatesViewState.Error
            Assert.assertEquals(RatesViewState.Error::class, result::class)
        }

        run {
            val result = ratesViewStateHolder.updateRates(conversionScheme) as RatesViewState.Rates

            Assert.assertEquals(3, result.ratesData.rates.size)
            Assert.assertEquals(1.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[0].sum)
            Assert.assertEquals(6.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[1].sum)
            Assert.assertEquals(0.5.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[2].sum)
        }

        run {
            val result = ratesViewStateHolder.updateTopCurrencyValue(2.toBigDecimal(MathContext.DECIMAL128)) as RatesViewState.Rates

            Assert.assertEquals(2.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[0].sum)
            Assert.assertEquals(12.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[1].sum)
            Assert.assertEquals(1.0.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[2].sum)
        }

        run {
            val result = ratesViewStateHolder.updateTopCurrencyValue(0.5.toBigDecimal(MathContext.DECIMAL128)) as RatesViewState.Rates

            Assert.assertEquals(0.5.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[0].sum)
            Assert.assertEquals(3.0.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[1].sum)
            Assert.assertEquals(0.25.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[2].sum)
        }

        run {
            ratesViewStateHolder.bringToTop("BGN")
            val result = ratesViewStateHolder.updateTopCurrencyValue(3.toBigDecimal(MathContext.DECIMAL128)) as RatesViewState.Rates

            Assert.assertEquals(3.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[0].sum)
            Assert.assertEquals(0.5.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[1].sum.setScale(1, RoundingMode.HALF_UP))
            Assert.assertEquals(0.25.toBigDecimal(MathContext.DECIMAL128), result.ratesData.rates[2].sum.setScale(2, RoundingMode.HALF_UP))
        }
    }

}
