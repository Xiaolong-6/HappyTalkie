package com.xiaolong.happytalkie.core

import android.content.Context
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

    fun refreshPeerConnection(callback: (PeerConnectionState) -> Unit = {}) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                val state =
                    if (nodes.isEmpty()) PeerConnectionState.DISCONNECTED
                    else PeerConnectionState.CONNECTED
                StateStore.setPeerConnection(appContext, state)
                EventBus.notifyStateChanged(appContext)
                mainHandler.post { callback(state) }
            }
            .addOnFailureListener {
                StateStore.setPeerConnection(
                    appContext,
                    PeerConnectionState.DISCONNECTED
                )
                EventBus.notifyStateChanged(appContext)
                mainHandler.post {
                    callback(PeerConnectionState.DISCONNECTED)
                }
            }
    }

    fun sendSignal(path: String, callId: String, callback: (Boolean) -> Unit) {
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    markDisconnected()
                    mainHandler.post { callback(false) }
                    return@addOnSuccessListener
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
                            if (anySuccess.get()) markConnected()
                            mainHandler.post { callback(anySuccess.get()) }
                        }
                    }.addOnFailureListener {
                        if (remaining.decrementAndGet() == 0) {
                            if (anySuccess.get()) markConnected()
                            else markDisconnected()
                            mainHandler.post { callback(anySuccess.get()) }
                        }
                    }
                }
            }
            .addOnFailureListener {
                markDisconnected()
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
                mapRequest.dataMap.putString(Protocol.KEY_ID, message.id)
                mapRequest.dataMap.putString(Protocol.KEY_ORIGIN, role.wireValue)
                mapRequest.dataMap.putLong(Protocol.KEY_CREATED_AT, message.createdAt)
                mapRequest.dataMap.putAsset(
                    Protocol.KEY_AUDIO,
                    Asset.createFromBytes(bytes)
                )

                dataClient.putDataItem(mapRequest.asPutDataRequest().setUrgent())
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

    private fun markConnected() {
        StateStore.setPeerConnection(
            appContext,
            PeerConnectionState.CONNECTED
        )
        EventBus.notifyStateChanged(appContext)
    }

    private fun markDisconnected() {
        val state =
            if (StateStore.activeCall(appContext) != null)
                PeerConnectionState.RECONNECTING
            else PeerConnectionState.DISCONNECTED
        StateStore.setPeerConnection(appContext, state)
        EventBus.notifyStateChanged(appContext)
    }
}
