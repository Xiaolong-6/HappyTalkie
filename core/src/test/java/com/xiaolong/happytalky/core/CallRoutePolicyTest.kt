package com.xiaolong.happytalky.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRoutePolicyTest {
    @Test
    fun nearbyDirectAllowsCallWhenConnected() {
        assertTrue(
            CallRoutePolicy.canStartCall(
                PeerConnectionState.CONNECTED,
                PeerRoute.NEARBY_DIRECT
            )
        )
    }

    @Test
    fun remoteWifiAllowsCallWhenConnected() {
        assertTrue(
            CallRoutePolicy.canStartCall(
                PeerConnectionState.CONNECTED,
                PeerRoute.REMOTE_WIFI
            )
        )
    }

    @Test
    fun cellularPrefersTalk() {
        assertFalse(
            CallRoutePolicy.canStartCall(
                PeerConnectionState.CONNECTED,
                PeerRoute.REMOTE_CELLULAR
            )
        )
        assertTrue(
            CallRoutePolicy.preferTalk(
                PeerRoute.REMOTE_CELLULAR
            )
        )
    }

    @Test
    fun uncertainRemotePrefersTalk() {
        assertFalse(
            CallRoutePolicy.canStartCall(
                PeerConnectionState.CONNECTED,
                PeerRoute.REMOTE_INTERNET
            )
        )
        assertTrue(
            CallRoutePolicy.preferTalk(
                PeerRoute.REMOTE_INTERNET
            )
        )
    }

    @Test
    fun disconnectedNeverStartsCall() {
        assertFalse(
            CallRoutePolicy.canStartCall(
                PeerConnectionState.DISCONNECTED,
                PeerRoute.NEARBY_DIRECT
            )
        )
    }
}
