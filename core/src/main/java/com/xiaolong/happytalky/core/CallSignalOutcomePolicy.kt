package com.xiaolong.happytalky.core

object CallSignalOutcomePolicy {
    fun remoteTerminalOutcome(
        wasActive: Boolean,
        disconnected: Boolean
    ): CallOutcome =
        when {
            disconnected ->
                CallOutcome.DISCONNECTED

            wasActive ->
                CallOutcome.COMPLETED

            else ->
                CallOutcome.CANCELLED_BY_PEER
        }
}
