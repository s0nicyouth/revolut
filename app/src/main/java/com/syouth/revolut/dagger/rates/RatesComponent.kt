package com.syouth.revolut.dagger.rates

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.syouth.revolut.dagger.ViewScope
import com.syouth.revolut.dagger.network.NetworkComponent
import com.syouth.revolut.rates.view.RatesView
import dagger.BindsInstance
import dagger.Component

@ViewScope
@Component(modules = [RatesModule::class], dependencies = [NetworkComponent::class])
interface RatesComponent {

    val ratesView: RatesView

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun activity(activity: AppCompatActivity): Builder

        @BindsInstance
        fun view(view: View): Builder

        @BindsInstance
        fun savedState(savedState: Bundle?): Builder

        fun networkComponent(networkComponent: NetworkComponent): Builder

        fun build(): RatesComponent
    }
}
