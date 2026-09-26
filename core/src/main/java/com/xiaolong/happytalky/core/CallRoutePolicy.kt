package com.xiaolong.happytalky.core

object CallRoutePolicy {
    fun canStartCall(
        connection: PeerConnectionState,
        route: PeerRoute
    ): Boolean =
        connection == PeerConnectionState.CONNECTED &&
            (
                route == PeerRoute.NEARBY_DIRECT ||
                    route == PeerRoute.REMOTE_WIFI
            )

    fun preferTalk(route: PeerRoute): Boolean =
        route == PeerRoute.REMOTE_CELLULAR ||
            route == PeerRoute.REMOTE_INTERNET ||
            route == PeerRoute.OFFLINE ||
            route == PeerRoute.UNKNOWN
}
