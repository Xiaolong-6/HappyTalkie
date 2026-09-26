package com.xiaolong.happytalkie.core

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class HappyTalkieListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val callId = messageEvent.data.toString(Charsets.UTF_8)
        if (callId.isBlank()) return

        when (messageEvent.path) {
            Protocol.CALL_RING -> receiveRing(callId)
            Protocol.CALL_ANSWER -> receiveAnswer(callId)
            Protocol.CALL_END -> receiveEnd(callId)
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
            StateStore.clearCallState(this)
            StateStore.setStatus(this, "Call disconnected")
            EventBus.notifyStateChanged(this)
            LiveCallService.stop(this)
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
                val saved = try {
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
                AudioPlayer.play(this, saved.file, deleteAfter = false)
                EventBus.notifyStateChanged(this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to receive voice message", e)
            }
        }
    }

    private fun receiveRing(callId: String) {
        if (StateStore.activeCall(this) != null) return
        if (StateStore.incomingCall(this) == callId) return

        StateStore.setOutgoingCall(this, null)
        StateStore.setCallInitiator(this, false)
        StateStore.setIncomingCall(this, callId)
        StateStore.setStatus(this, "Incoming call")
        AlertController.startIncomingCall(this, callId)
        EventBus.notifyStateChanged(this)
    }

    private fun receiveAnswer(callId: String) {
        if (StateStore.outgoingCall(this) != callId) return

        StateStore.setIncomingCall(this, null)
        StateStore.setOutgoingCall(this, null)
        StateStore.setActiveCall(this, callId)
        StateStore.setStatus(this, "Connecting live audio…")
        AlertController.stop(this)
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
