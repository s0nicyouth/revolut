package com.syouth.revolut.rates.view

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.syouth.revolut.R
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.rates.viewmodel.RatesViewModel
import com.syouth.revolut.rates.viewmodel.RatesViewState
import javax.inject.Inject

@ViewScope
class RatesView @Inject constructor(
    rootView: View,
    context: Context,
    private val ratesViewModel: RatesViewModel,
    lifecycleOwner: LifecycleOwner,
    private val adapter: RatesListAdapter) {

    init {
        ratesViewModel.ratesObservable.observe(lifecycleOwner, Observer { render(it) })
    }

    private val recyclerView = rootView.findViewById<RecyclerView>(R.id.id_rates_list).apply {
        layoutManager = LinearLayoutManager(context).apply {
            setHasFixedSize(true)
        }
        adapter = this@RatesView.adapter
    }
    private val progress = rootView.findViewById<ProgressBar>(R.id.id_progress)
    private val error = rootView.findViewById<TextView>(R.id.id_error).apply {
        visibility = View.GONE
    }

    private fun render(viewState: RatesViewState) = when (viewState) {
        is RatesViewState.Empty -> {
            adapter.clear()
            recyclerView.visibility = View.GONE
            progress.visibility = View.VISIBLE
            error.visibility = View.GONE
        }
        is RatesViewState.Rates -> {
            adapter.applyDiff(viewState.ratesData)
            recyclerView.visibility = View.VISIBLE
            progress.visibility = View.GONE
            error.visibility = View.GONE
        }
        is RatesViewState.Error -> {
            recyclerView.visibility = View.GONE
            progress.visibility = View.GONE
            error.visibility = View.VISIBLE
        }
    }

    fun saveState(outState: Bundle) = ratesViewModel.saveState(outState)
}

