package com.xiaolong.happytalky.core

enum class PriorityRequestDisposition {
    APPLY,
    IGNORE_ALREADY_ACTIVE,
    REJECT_BUSY
}

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

    fun requestDisposition(
        requestedCallId: String,
        incomingCallId: String?,
        outgoingCallId: String?,
        activeCallId: String?
    ): PriorityRequestDisposition =
        when {
            activeCallId ==
                requestedCallId ->
                PriorityRequestDisposition
                    .IGNORE_ALREADY_ACTIVE

            activeCallId != null ||
                outgoingCallId != null ||
                (
                    incomingCallId != null &&
                    incomingCallId !=
                        requestedCallId
                ) ->
                PriorityRequestDisposition
                    .REJECT_BUSY

            else ->
                PriorityRequestDisposition.APPLY
        }

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
