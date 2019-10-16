package com.syouth.revolut.dagger.network

import com.syouth.revolut.dagger.NetworkScope
import com.syouth.revolut.net.RequestRepeaterFactory
import dagger.Component

/**
 * Isolated network component.
 * Only public classes will be exposed here.
 */
@NetworkScope
@Component(modules = [NetworkModule::class])
interface NetworkComponent {
    val requestRepeaterFactory: RequestRepeaterFactory
}
