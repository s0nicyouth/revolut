package com.syouth.revolut.rates.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Date

class BigDecimalAdapter {
    @FromJson
    fun fromJson(value: String) = BigDecimal(value, MathContext.DECIMAL128)

    @ToJson
    fun toJson(value: BigDecimal): String = value.setScale(12, RoundingMode.HALF_UP).toString()
}

@JsonClass(generateAdapter = true)
data class RatesModelScheme(
    @Json(name = "base")
    val base : String,
    @Json(name = "date")
    val date: Date,
    @Json(name = "rates")
    val rates: Map<String, BigDecimal>
)
