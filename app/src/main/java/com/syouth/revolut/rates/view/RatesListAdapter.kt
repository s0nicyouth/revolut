package com.syouth.revolut.rates.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.syouth.revolut.R
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.rates.viewmodel.DiffOperation
import com.syouth.revolut.rates.viewmodel.Rate
import com.syouth.revolut.rates.viewmodel.RatesData
import com.syouth.revolut.rates.viewmodel.RatesViewModel
import com.syouth.revolut.utils.exhaustive
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import javax.inject.Inject

@ViewScope
class RatesListAdapter @Inject constructor(
    private val viewModel: RatesViewModel,
    private val context: Context,
    private val inputMethodManager: InputMethodManager)
    : RecyclerView.Adapter<RatesListAdapter.ViewHolder>() {

    private val rates = mutableListOf<Rate>()
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            try {
                NumberFormat.getInstance().parse(s.toString().trim())?.let {
                    viewModel.updateTopCurrencyValue(it.toString().toBigDecimal(MathContext.DECIMAL128))
                }
            } catch (_: Exception) {}
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    fun applyDiff(ratesData: RatesData) {
        rates.clear()
        rates.addAll(ratesData.rates)
        ratesData.diff.forEach {
            when (it) {
                is DiffOperation.Insert -> notifyItemInserted(it.to)
                is DiffOperation.Change -> notifyItemChanged(it.index, Unit)
                is DiffOperation.Move -> notifyItemMoved(it.from, it.to)
            }.exhaustive
        }
    }

    fun clear() {
        rates.clear()
        notifyItemRangeRemoved(0, rates.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LayoutInflater.from(parent.context).inflate(R.layout.rate_layout, parent, false).let {
            ViewHolder(
                it,
                it.findViewById(R.id.id_flag),
                it.findViewById(R.id.id_short_currency_name),
                it.findViewById(R.id.id_currency_name),
                it.findViewById(R.id.id_converted_rate))
        }

    override fun getItemCount() = rates.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        rates[position].let { rate ->
            holder.rate.removeTextChangedListener(textWatcher)

            rate.flag?.let { flagId ->
                holder.flag.setImageDrawable(ContextCompat.getDrawable(
                    context,
                    flagId))
            }
            holder.shortCurrencyName.text = rate.shortName
            holder.currencyName.text = rate.fullName
            holder.rate.setText(
                NumberFormat.getNumberInstance().format(
                    rate.sum.setScale(2, RoundingMode.HALF_UP).toDouble()))

            holder.root.setOnClickListener {
                holder.rate.requestFocus()
                inputMethodManager.showSoftInput(holder.rate, InputMethodManager.SHOW_IMPLICIT)
                viewModel.bringToTop(rate.shortName)
            }
            holder.rate.setOnFocusChangeListener { _, _ ->
                viewModel.bringToTop(rate.shortName)
            }

            if (position == 0) holder.rate.addTextChangedListener(textWatcher)
        }

    data class ViewHolder(
        val root: View,
        val flag: AppCompatImageView,
        val shortCurrencyName: TextView,
        val currencyName: TextView,
        val rate: EditText
    ) : RecyclerView.ViewHolder(root)
}
