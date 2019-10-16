package com.syouth.revolut.rates.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.syouth.revolut.Application
import com.syouth.revolut.R
import com.syouth.revolut.dagger.rates.DaggerRatesComponent

class RatesActivity : AppCompatActivity() {

    private lateinit var ratesView: RatesView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rates_view)

        ratesView = DaggerRatesComponent.builder()
            .view(window.decorView.rootView)
            .activity(this)
            .savedState(savedInstanceState)
            .networkComponent(Application.netComponent)
            .build()
            .ratesView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        ratesView.saveState(outState)
    }
}
