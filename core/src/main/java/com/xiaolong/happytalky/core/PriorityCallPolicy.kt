package com.xiaolong.happytalky.core

object PriorityCallPolicy {
    fun canOffer(
        localRole: EndpointRole,
        outgoingCallPresent: Boolean,
        elapsedMs: Long,
        peerSupportsPriority: Boolean,
        peerAllowsAutoAnswer: Boolean
    ): Boolean =
        localRole == EndpointRole.PHONE &&
            outgoingCallPresent &&
            elapsedMs >=
                Protocol.PRIORITY_OFFER_DELAY_MS &&
            peerSupportsPriority &&
            peerAllowsAutoAnswer

    fun canAutoAnswer(
        localRole: EndpointRole,
        enabled: Boolean,
        mode: CallMode,
        incomingCallPresent: Boolean,
        activityVisible: Boolean
    ): Boolean =
        localRole == EndpointRole.WATCH &&
            enabled &&
            mode == CallMode.PRIORITY &&
            incomingCallPresent &&
            activityVisible
}
