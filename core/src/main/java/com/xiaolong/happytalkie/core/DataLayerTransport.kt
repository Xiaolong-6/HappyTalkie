package com.xiaolong.happytalkie.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class DataLayerTransport(context: Context) {
    private val appContext = context.applicationContext
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val dataClient = Wearable.getDataClient(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()

    fun refreshPeerConnection(
        callback: (PeerConnectionState, PeerRoute) -> Unit = { _, _ -> }
    ) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    updateConnection(
                        PeerConnectionState.DISCONNECTED,
                        PeerRoute.OFFLINE
                    )
                    mainHandler.post {
                        callback(
                            PeerConnectionState.DISCONNECTED,
                            PeerRoute.OFFLINE
                        )
                    }
                    return@addOnSuccessListener
                }

                val route =
                    if (nodes.any { it.isNearby }) {
                        PeerRoute.NEARBY_DIRECT
                    } else {
                        remoteRoute()
                    }

                updateConnection(
                    PeerConnectionState.CONNECTED,
                    route
                )
                mainHandler.post {
                    callback(PeerConnectionState.CONNECTED, route)
                }
            }
            .addOnFailureListener {
                updateConnection(
                    PeerConnectionState.DISCONNECTED,
                    PeerRoute.OFFLINE
                )
                mainHandler.post {
                    callback(
                        PeerConnectionState.DISCONNECTED,
                        PeerRoute.OFFLINE
                    )
                }
            }
    }

    fun sendSignal(
        path: String,
        callId: String,
        callback: (Boolean) -> Unit
    ) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    updateConnection(
                        PeerConnectionState.DISCONNECTED,
                        PeerRoute.OFFLINE
                    )
                    mainHandler.post { callback(false) }
                    return@addOnSuccessListener
                }

                val route =
                    if (nodes.any { it.isNearby }) {
                        PeerRoute.NEARBY_DIRECT
                    } else {
                        remoteRoute()
                    }

                val remaining = AtomicInteger(nodes.size)
                val anySuccess = AtomicBoolean(false)

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        path,
                        callId.toByteArray(Charsets.UTF_8)
                    ).addOnSuccessListener {
                        anySuccess.set(true)
                        if (remaining.decrementAndGet() == 0) {
                            if (anySuccess.get()) {
                                updateConnection(
                                    PeerConnectionState.CONNECTED,
                                    route
                                )
                            }
                            mainHandler.post {
                                callback(anySuccess.get())
                            }
                        }
                    }.addOnFailureListener {
                        if (remaining.decrementAndGet() == 0) {
                            if (anySuccess.get()) {
                                updateConnection(
                                    PeerConnectionState.CONNECTED,
                                    route
                                )
                            } else {
                                updateConnection(
                                    PeerConnectionState.DISCONNECTED,
                                    PeerRoute.OFFLINE
                                )
                            }
                            mainHandler.post {
                                callback(anySuccess.get())
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                updateConnection(
                    PeerConnectionState.DISCONNECTED,
                    PeerRoute.OFFLINE
                )
                mainHandler.post { callback(false) }
            }
    }

    fun queueVoice(
        message: VoiceMessage,
        role: EndpointRole,
        callback: (Boolean) -> Unit
    ) {
        io.execute {
            try {
                val bytes = message.file.readBytes()
                val mapRequest = PutDataMapRequest.create(
                    Protocol.VOICE_PREFIX + message.id
                )
                mapRequest.dataMap.putString(
                    Protocol.KEY_ID,
                    message.id
                )
                mapRequest.dataMap.putString(
                    Protocol.KEY_ORIGIN,
                    role.wireValue
                )
                mapRequest.dataMap.putLong(
                    Protocol.KEY_CREATED_AT,
                    message.createdAt
                )
                mapRequest.dataMap.putAsset(
                    Protocol.KEY_AUDIO,
                    Asset.createFromBytes(bytes)
                )

                dataClient
                    .putDataItem(
                        mapRequest
                            .asPutDataRequest()
                            .setUrgent()
                    )
                    .addOnSuccessListener {
                        mainHandler.post { callback(true) }
                    }
                    .addOnFailureListener {
                        mainHandler.post { callback(false) }
                    }
            } catch (_: Exception) {
                mainHandler.post { callback(false) }
            }
        }
    }

    private fun remoteRoute(): PeerRoute {
        val manager =
            appContext.getSystemService(
                ConnectivityManager::class.java
            ) ?: return PeerRoute.REMOTE_INTERNET

        val network = manager.activeNetwork
            ?: return PeerRoute.REMOTE_INTERNET
        val capabilities =
            manager.getNetworkCapabilities(network)
                ?: return PeerRoute.REMOTE_INTERNET

        return when {
            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) -> PeerRoute.REMOTE_WIFI

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) -> PeerRoute.REMOTE_CELLULAR

            else -> PeerRoute.REMOTE_INTERNET
        }
    }

    private fun updateConnection(
        state: PeerConnectionState,
        route: PeerRoute
    ) {
        StateStore.setPeerConnection(appContext, state)
        StateStore.setPeerRoute(appContext, route)
        EventBus.notifyStateChanged(appContext)
    }
}
