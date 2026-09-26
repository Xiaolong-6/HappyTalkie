package com.xiaolong.happytalkie.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_ANSWER -> answer(appContext)
            ACTION_DECLINE -> decline(appContext)
            ACTION_HANG_UP -> hangUp(appContext)
        }
    }

    private fun answer(context: Context) {
        val callId = StateStore.incomingCall(context) ?: return

        AlertController.stop(context)
        StateStore.setIncomingCall(context, null)
        StateStore.setCallInitiator(context, false)
        StateStore.setActiveCall(context, callId)
        StateStore.clearReconnectWindow(context)
        StateStore.setStatus(context, "Connecting live audio…")
        LiveCallService.start(context)
        EventBus.notifyStateChanged(context)

        DataLayerTransport(context)
            .sendSignal(Protocol.CALL_ANSWER, callId) { sent ->
                if (!sent && StateStore.activeCall(context) == callId) {
                    StateStore.clearCallState(context)
                    StateStore.setStatus(context, "Peer is unreachable")
                    LiveCallService.stop(context)
                    EventBus.notifyStateChanged(context)
                }
            }
    }

    private fun decline(context: Context) {
        val callId = StateStore.incomingCall(context) ?: return

        StateStore.clearCallState(context)
        StateStore.setStatus(context, "Call declined")
        AlertController.stop(context)
        EventBus.notifyStateChanged(context)

        DataLayerTransport(context)
            .sendSignal(Protocol.CALL_DECLINE, callId) { }
    }

    private fun hangUp(context: Context) {
        val active = StateStore.activeCall(context)
        val outgoing = StateStore.outgoingCall(context)
        val incoming = StateStore.incomingCall(context)
        val callId = active ?: outgoing ?: incoming ?: return

        val path = when {
            active != null -> Protocol.CALL_END
            outgoing != null -> Protocol.CALL_CANCEL
            else -> Protocol.CALL_DECLINE
        }

        StateStore.clearCallState(context)
        StateStore.setStatus(
            context,
            if (outgoing != null) "Call cancelled" else "Call ended"
        )
        AlertController.stop(context)
        LiveCallAudio.stop(context)
        LiveCallService.stop(context)
        EventBus.notifyStateChanged(context)

        DataLayerTransport(context)
            .sendSignal(path, callId) { }
    }

    companion object {
        const val ACTION_ANSWER =
            "com.xiaolong.happytalkie.action.ANSWER_CALL"
        const val ACTION_DECLINE =
            "com.xiaolong.happytalkie.action.DECLINE_CALL"
        const val ACTION_HANG_UP =
            "com.xiaolong.happytalkie.action.HANG_UP"
    }
}
