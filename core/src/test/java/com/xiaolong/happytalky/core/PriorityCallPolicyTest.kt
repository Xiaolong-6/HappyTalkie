package com.xiaolong.happytalky.core

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
}
