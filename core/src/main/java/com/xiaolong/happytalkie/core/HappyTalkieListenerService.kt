package com.xiaolong.happytalkie.core

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.io.File

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
                val voiceId = map.getString(Protocol.KEY_ID) ?: path.substringAfterLast('/')
                val callId = map.getString(Protocol.KEY_CALL_ID).orEmpty()
                val dir = File(filesDir, "received-voice").apply { mkdirs() }
                val output = File(dir, "voice-" + voiceId + ".m4a")

                val response = Tasks.await(dataClient.getFdForAsset(asset))
                try {
                    response.inputStream?.use { input ->
                        output.outputStream().use { destination ->
                            input.copyTo(destination)
                        }
                    } ?: return@forEach
                } finally {
                    response.release()
                }

                Tasks.await(dataClient.deleteDataItems(item.uri))
                deliverVoice(output, callId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to receive voice message", e)
            }
        }
    }

    private fun receiveRing(callId: String) {
        if (StateStore.activeCall(this) != null) return
        if (StateStore.incomingCall(this) == callId) return

        StateStore.setOutgoingCall(this, null)
        StateStore.setIncomingCall(this, callId)
        StateStore.setStatus(this, "Incoming call — tap ANSWER")
        AlertController.startIncomingCall(this, callId)
        EventBus.notifyStateChanged(this)
    }

    private fun receiveAnswer(callId: String) {
        StateStore.setIncomingCall(this, null)
        StateStore.setOutgoingCall(this, null)
        StateStore.setActiveCall(this, callId)
        StateStore.setStatus(this, "Connected — hold TALK to speak")
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
        AudioPlayer.stop()
        EventBus.notifyStateChanged(this)
    }

    private fun deliverVoice(file: File, callId: String) {
        val activeCall = StateStore.activeCall(this)
        val inCurrentCall = callId.isNotBlank() && activeCall == callId

        if (inCurrentCall) {
            StateStore.setStatus(this, "Connected — voice received")
        } else {
            if (StateStore.incomingCall(this) != null) {
                StateStore.setIncomingCall(this, null)
                AlertController.stop(this)
            }
            StateStore.setStatus(this, "Voice message received")
            AlertController.postVoiceNotification(this)
        }

        AudioPlayer.play(this, file, deleteAfter = true)
        EventBus.notifyStateChanged(this)
    }

    companion object {
        private const val TAG = "HappyTalkieListener"
    }
}
