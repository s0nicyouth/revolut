package com.syouth.revolut

import android.app.Application
import com.syouth.revolut.dagger.network.DaggerNetworkComponent
import com.syouth.revolut.dagger.network.NetworkComponent

class Application : Application() {

    companion object {
        lateinit var netComponent: NetworkComponent
    }

    override fun onCreate() {
        super.onCreate()

        netComponent = DaggerNetworkComponent.builder().build()
    }
}
