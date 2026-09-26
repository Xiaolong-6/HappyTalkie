package com.xiaolong.happytalky.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityCallPolicyTest {
    @Test
    fun phoneOfferRequiresDelaySupportAndWatchOptIn() {
        assertTrue(
            PriorityCallPolicy.canOffer(
                localRole = EndpointRole.PHONE,
                outgoingCallPresent = true,
                elapsedMs =
                    Protocol.PRIORITY_OFFER_DELAY_MS,
                peerSupportsPriority = true,
                peerAllowsAutoAnswer = true
            )
        )

        assertFalse(
            PriorityCallPolicy.canOffer(
                localRole = EndpointRole.PHONE,
                outgoingCallPresent = true,
                elapsedMs =
                    Protocol.PRIORITY_OFFER_DELAY_MS -
                        1L,
                peerSupportsPriority = true,
                peerAllowsAutoAnswer = true
            )
        )

        assertFalse(
            PriorityCallPolicy.canOffer(
                localRole = EndpointRole.PHONE,
                outgoingCallPresent = true,
                elapsedMs =
                    Protocol.PRIORITY_OFFER_DELAY_MS,
                peerSupportsPriority = true,
                peerAllowsAutoAnswer = false
            )
        )
    }

    @Test
    fun watchAutoAnswerRequiresVisibleOptedInPriorityCall() {
        assertTrue(
            PriorityCallPolicy.canAutoAnswer(
                localRole = EndpointRole.WATCH,
                enabled = true,
                mode = CallMode.PRIORITY,
                incomingCallPresent = true,
                activityVisible = true
            )
        )

        assertFalse(
            PriorityCallPolicy.canAutoAnswer(
                localRole = EndpointRole.WATCH,
                enabled = true,
                mode = CallMode.PRIORITY,
                incomingCallPresent = true,
                activityVisible = false
            )
        )

        assertFalse(
            PriorityCallPolicy.canAutoAnswer(
                localRole = EndpointRole.WATCH,
                enabled = true,
                mode = CallMode.NORMAL,
                incomingCallPresent = true,
                activityVisible = true
            )
        )
    }
    @Test
    fun sameActiveCallIgnoresLatePriorityInsteadOfBusy() {
        assertEquals(
            PriorityRequestDisposition
                .IGNORE_ALREADY_ACTIVE,
            PriorityCallPolicy
                .requestDisposition(
                    requestedCallId = "call-1",
                    incomingCallId = null,
                    outgoingCallId = null,
                    activeCallId = "call-1"
                )
        )
    }

    @Test
    fun unrelatedActiveCallRejectsPriorityAsBusy() {
        assertEquals(
            PriorityRequestDisposition
                .REJECT_BUSY,
            PriorityCallPolicy
                .requestDisposition(
                    requestedCallId = "call-1",
                    incomingCallId = null,
                    outgoingCallId = null,
                    activeCallId = "call-2"
                )
        )
    }

}
