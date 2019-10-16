package com.syouth.revolut.rates.viewmodel

import android.os.Parcel
import android.os.Parcelable
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class Rate(
    val flag: Int?,
    val shortName: String,
    val fullName: String,
    val sum: BigDecimal) : Parcelable {

    companion object CREATOR : Parcelable.Creator<Rate> {
        override fun createFromParcel(parcel: Parcel): Rate {
            val flag = parcel.readInt()
            return Rate(
                if (flag != -1) flag else null,
                parcel.readString()!!,
                parcel.readString()!!,
                parcel.readString()!!.toBigDecimal(MathContext.DECIMAL128))
        }

        override fun newArray(size: Int): Array<Rate?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(flag ?: -1)
        dest.writeString(shortName)
        dest.writeString(fullName)
        dest.writeString(sum.setScale(12, RoundingMode.HALF_UP).toString())
    }

    override fun describeContents() = 0
}

sealed class DiffOperation {
    data class Insert(val to: Int): DiffOperation()
    data class Move(val from: Int, val to: Int): DiffOperation()
    data class Change(val index: Int) : DiffOperation()
}

data class RatesData(
    val rates: List<Rate>,
    val diff: List<DiffOperation>)

sealed class RatesViewState {
    object Empty : RatesViewState()
    object Error : RatesViewState()
    data class Rates(val ratesData: RatesData) : RatesViewState()
}
