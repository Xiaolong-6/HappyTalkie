package com.xiaolong.happytalkie.core

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class HappyTalkieListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val callId = messageEvent.data.toString(Charsets.UTF_8)
        if (callId.isBlank()) return

        when (messageEvent.path) {
            Protocol.CALL_RING -> receiveRing(callId)
            Protocol.CALL_ANSWER -> receiveAnswer(callId)
            Protocol.CALL_DECLINE -> receiveDecline(callId)
            Protocol.CALL_CANCEL -> receiveCancel(callId)
            Protocol.CALL_BUSY -> receiveBusy(callId)
            Protocol.CALL_END -> receiveEnd(callId)
        }
    }

    override fun onPeerConnected(peer: Node) {
        DataLayerTransport(this)
            .refreshPeerConnection { state, _ ->
                if (
                    state == PeerConnectionState.CONNECTED &&
                    StateStore.activeCall(this) != null &&
                    !LiveCallAudio.isRunning()
                ) {
                    StateStore.setStatus(
                        this,
                        "Reconnecting live audio…"
                    )
                    StateStore.beginReconnectWindow(this)
                    LiveCallService.start(this)
                }

                EventBus.notifyStateChanged(this)
            }
    }

    override fun onPeerDisconnected(peer: Node) {
        // NodeClient reports every Android node on the Wear network, not
        // only the HappyTalkie companion. Re-check the advertised
        // capability before changing product state.
        DataLayerTransport(this)
            .refreshPeerConnection { state, _ ->
                if (state == PeerConnectionState.CONNECTED) {
                    EventBus.notifyStateChanged(this)
                    return@refreshPeerConnection
                }

                if (StateStore.activeCall(this) != null) {
                    StateStore.setPeerConnection(
                        this,
                        PeerConnectionState.RECONNECTING
                    )
                    StateStore.setPeerRoute(
                        this,
                        PeerRoute.RECONNECTING
                    )
                    StateStore.setStatus(
                        this,
                        "Reconnecting…"
                    )
                    StateStore.beginReconnectWindow(this)
                    LiveCallAudio.stop(
                        this,
                        closeChannel = false
                    )
                    LiveCallService.start(this)
                } else {
                    StateStore.setPeerConnection(
                        this,
                        PeerConnectionState.DISCONNECTED
                    )
                    StateStore.setPeerRoute(
                        this,
                        PeerRoute.OFFLINE
                    )
                    StateStore.setStatus(
                        this,
                        "Peer offline · TALK recommended"
                    )
                }

                EventBus.notifyStateChanged(this)
            }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val path = channel.path
        if (!path.startsWith(Protocol.CALL_AUDIO_PREFIX)) return

        val callId = path.removePrefix(Protocol.CALL_AUDIO_PREFIX)
        if (callId.isBlank() || StateStore.activeCall(this) != callId) {
            Wearable.getChannelClient(this).close(channel)
            return
        }

        LiveCallAudio.attachIncoming(this, channel)
    }

    override fun onChannelClosed(
        channel: ChannelClient.Channel,
        closeReason: Int,
        appSpecificErrorCode: Int
    ) {
        val path = channel.path
        if (!path.startsWith(Protocol.CALL_AUDIO_PREFIX)) return
        val callId = path.removePrefix(Protocol.CALL_AUDIO_PREFIX)

        if (StateStore.activeCall(this) == callId) {
            LiveCallAudio.stop(this, closeChannel = false)
            StateStore.setPeerConnection(
                this,
                PeerConnectionState.RECONNECTING
            )
            StateStore.setPeerRoute(
                this,
                PeerRoute.RECONNECTING
            )
            StateStore.setStatus(this, "Reconnecting…")
            StateStore.beginReconnectWindow(this)
            EventBus.notifyStateChanged(this)
            LiveCallService.start(this)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val role = EndpointRole.fromContext(this)
        val dataClient = Wearable.getDataClient(this)

        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            val path = item.uri.path ?: return@forEach
            if (!path.startsWith(Protocol.VOICE_PREFIX)) return@forEach

            try {
                val map = DataMapItem.fromDataItem(item).dataMap
                val origin = map.getString(Protocol.KEY_ORIGIN)
                if (origin == role.wireValue) return@forEach

                val asset = map.getAsset(Protocol.KEY_AUDIO) ?: return@forEach
                val voiceId =
                    map.getString(Protocol.KEY_ID) ?: path.substringAfterLast('/')
                val createdAt = map.getLong(Protocol.KEY_CREATED_AT)

                val response = Tasks.await(dataClient.getFdForAsset(asset))
                try {
                    val stream = response.inputStream ?: return@forEach
                    stream.use {
                        VoiceMessageStore.saveIncoming(
                            this,
                            voiceId,
                            createdAt,
                            it
                        )
                    }
                } finally {
                    response.release()
                }

                Tasks.await(dataClient.deleteDataItems(item.uri))
                StateStore.setStatus(this, "New voice message")
                AlertController.postVoiceNotification(this)

                // TALK messages are intentionally never auto-played.
                // The user explicitly chooses when audio is played.
                EventBus.notifyStateChanged(this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to receive voice message", e)
            }
        }
    }

    private fun receiveRing(callId: String) {
        if (StateStore.incomingCall(this) == callId) return

        val busy =
            StateStore.activeCall(this) != null ||
            StateStore.outgoingCall(this) != null ||
            StateStore.incomingCall(this) != null

        if (busy) {
            DataLayerTransport(this)
                .sendSignal(Protocol.CALL_BUSY, callId) { }
            return
        }

        StateStore.setCallInitiator(this, false)
        StateStore.setIncomingCall(this, callId)
        StateStore.setPeerConnection(
            this,
            PeerConnectionState.CONNECTED
        )
        DataLayerTransport(this).refreshPeerConnection()
        StateStore.setStatus(this, "Incoming call")
        AlertController.startIncomingCall(this, callId)
        EventBus.notifyStateChanged(this)
    }

    private fun receiveAnswer(callId: String) {
        if (StateStore.outgoingCall(this) != callId) return

        StateStore.setIncomingCall(this, null)
        StateStore.setOutgoingCall(this, null)
        StateStore.setActiveCall(this, callId)
        StateStore.clearReconnectWindow(this)
        StateStore.setPeerConnection(
            this,
            PeerConnectionState.CONNECTED
        )
        DataLayerTransport(this).refreshPeerConnection()
        StateStore.setStatus(this, "Connecting live audio…")
        AlertController.stop(this)
        LiveCallService.start(this)
        EventBus.notifyStateChanged(this)
    }

    private fun receiveDecline(callId: String) {
        if (StateStore.outgoingCall(this) != callId) return

        StateStore.clearCallState(this)
        StateStore.setStatus(this, "Call declined")
        AlertController.stop(this)
        LiveCallAudio.stop(this)
        LiveCallService.stop(this)
        EventBus.notifyStateChanged(this)
    }

    private fun receiveCancel(callId: String) {
        if (StateStore.incomingCall(this) != callId) return

        StateStore.clearCallState(this)
        StateStore.setStatus(this, "Call cancelled")
        AlertController.stop(this)
        LiveCallAudio.stop(this)
        LiveCallService.stop(this)
        EventBus.notifyStateChanged(this)
    }

    private fun receiveBusy(callId: String) {
        if (StateStore.outgoingCall(this) != callId) return

        StateStore.clearCallState(this)
        StateStore.setStatus(this, "Peer is busy")
        AlertController.stop(this)
        LiveCallAudio.stop(this)
        LiveCallService.stop(this)
        EventBus.notifyStateChanged(this)
    }

    private fun receiveEnd(callId: String) {
        val relevant =
            StateStore.activeCall(this) == callId ||
            StateStore.incomingCall(this) == callId ||
            StateStore.outgoingCall(this) == callId

        if (!relevant) return

        StateStore.clearCallState(this)
        StateStore.setStatus(this, "Call ended")
        AlertController.stop(this)
        LiveCallAudio.stop(this)
        LiveCallService.stop(this)
        EventBus.notifyStateChanged(this)
    }

    companion object {
        private const val TAG = "HappyTalkieListener"
    }
}
