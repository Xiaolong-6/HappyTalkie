package com.xiaolong.happytalkie.core

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

enum class CallVisualState {
    READY,
    INCOMING,
    OUTGOING,
    CONNECTING,
    LIVE
}

data class HappyTalkieUiState(
    val status: String = "Ready",
    val callState: CallVisualState = CallVisualState.READY,
    val recording: Boolean = false,
    val callEnabled: Boolean = true,
    val talkEnabled: Boolean = true,
    val peerName: String = "Watch",
    val messages: List<VoiceMessage> = emptyList()
)

abstract class HappyTalkieActivity : ComponentActivity() {
    private lateinit var role: EndpointRole
    private lateinit var transport: DataLayerTransport
    private lateinit var recorder: AudioRecorder

    protected var uiState by mutableStateOf(HappyTalkieUiState())
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var recording = false
    private var receiverRegistered = false
    private var recordingTimeout: Runnable? = null
    private var callTimeout: Runnable? = null

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshUiState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        role = EndpointRole.fromContext(this)
        transport = DataLayerTransport(this)
        recorder = AudioRecorder(this)

        requestNeededPermissions()
        refreshUiState()
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            val filter = IntentFilter(Protocol.ACTION_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(stateReceiver, filter)
            }
            receiverRegistered = true
        }
        refreshUiState()
    }

    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        recordingTimeout?.let(handler::removeCallbacks)
        callTimeout?.let(handler::removeCallbacks)
        recordingTimeout = null
        callTimeout = null
        if (recording) recorder.cancel()
        super.onDestroy()
    }

    protected fun handleCallAction() {
        val incoming = StateStore.incomingCall(this)
        val active = StateStore.activeCall(this)

        when {
            incoming != null -> answerCall(incoming)
            active != null -> endCall(active)
            StateStore.outgoingCall(this) == null -> startCall()
        }
    }

    protected fun beginTalk() {
        if (recording || hasAnyCallState()) return

        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_PERMISSIONS
            )
            StateStore.setStatus(this, "Microphone permission required")
            refreshUiState()
            return
        }

        if (recorder.start()) {
            recording = true
            StateStore.setStatus(this, "Recording TALK…")
            refreshUiState()

            recordingTimeout?.let(handler::removeCallbacks)
            recordingTimeout = Runnable {
                recordingTimeout = null
                if (recording) finishTalk()
            }.also {
                handler.postDelayed(it, Protocol.MAX_RECORDING_MS.toLong())
            }
        } else {
            StateStore.setStatus(this, "Could not start microphone")
            refreshUiState()
        }
    }

    protected fun finishTalk() {
        if (!recording) return

        recording = false
        recordingTimeout?.let(handler::removeCallbacks)
        recordingTimeout = null

        val temp = recorder.stop()
        if (temp == null) {
            StateStore.setStatus(this, "Recording was too short")
            refreshUiState()
            return
        }

        val saved = try {
            VoiceMessageStore.saveOutgoing(this, temp)
        } catch (_: Exception) {
            temp.delete()
            StateStore.setStatus(this, "Could not save voice message")
            refreshUiState()
            return
        }

        StateStore.setStatus(this, "Sending TALK…")
        refreshUiState()

        transport.queueVoice(saved, role) { queued ->
            StateStore.setStatus(
                this,
                if (queued) "TALK queued for delivery"
                else "Saved locally · delivery failed"
            )
            refreshUiState()
        }
    }

    protected fun cancelTalk() {
        if (!recording) return

        recording = false
        recordingTimeout?.let(handler::removeCallbacks)
        recordingTimeout = null
        recorder.cancel()
        StateStore.setStatus(this, "Recording cancelled")
        refreshUiState()
    }

    protected fun playMessage(message: VoiceMessage) {
        AudioPlayer.play(this, message.file, deleteAfter = false)
    }

    protected fun refreshNow() {
        refreshUiState()
    }

    private fun startCall() {
        val callId = UUID.randomUUID().toString()
        val peer = peerName()

        StateStore.setCallInitiator(this, true)
        StateStore.setOutgoingCall(this, callId)
        StateStore.setStatus(this, "Calling $peer…")
        LiveCallService.start(this)
        refreshUiState()

        transport.sendSignal(Protocol.CALL_RING, callId) { sent ->
            if (StateStore.outgoingCall(this) != callId) return@sendSignal

            if (!sent) {
                StateStore.clearCallState(this)
                StateStore.setStatus(this, "$peer is offline · leave a TALK")
                LiveCallService.stop(this)
                refreshUiState()
                return@sendSignal
            }

            StateStore.setStatus(this, "Ringing $peer…")
            refreshUiState()

            callTimeout?.let(handler::removeCallbacks)
            callTimeout = Runnable {
                if (
                    StateStore.outgoingCall(this) == callId &&
                    StateStore.activeCall(this) == null
                ) {
                    StateStore.clearCallState(this)
                    StateStore.setStatus(this, "No answer · leave a TALK")
                    LiveCallService.stop(this)
                    refreshUiState()
                }
                callTimeout = null
            }.also {
                handler.postDelayed(it, Protocol.CALL_TIMEOUT_MS)
            }
        }
    }

    private fun answerCall(callId: String) {
        AlertController.stop(this)
        StateStore.setIncomingCall(this, null)
        StateStore.setCallInitiator(this, false)
        StateStore.setActiveCall(this, callId)
        StateStore.setStatus(this, "Connecting live audio…")
        LiveCallService.start(this)
        EventBus.notifyStateChanged(this)
        refreshUiState()

        transport.sendSignal(Protocol.CALL_ANSWER, callId) { sent ->
            if (!sent && StateStore.activeCall(this) == callId) {
                StateStore.clearCallState(this)
                StateStore.setStatus(this, peerName() + " is unreachable")
                LiveCallService.stop(this)
                refreshUiState()
            }
        }
    }

    private fun endCall(callId: String) {
        StateStore.clearCallState(this)
        StateStore.setStatus(this, "Call ended")
        AlertController.stop(this)
        LiveCallAudio.stop(this)
        LiveCallService.stop(this)
        refreshUiState()
        transport.sendSignal(Protocol.CALL_END, callId) { }
    }

    private fun refreshUiState() {
        val incoming = StateStore.incomingCall(this)
        val outgoing = StateStore.outgoingCall(this)
        val active = StateStore.activeCall(this)
        val live = LiveCallAudio.isRunning()
        val peer = peerName()

        val visualState = when {
            active != null && live -> CallVisualState.LIVE
            active != null -> CallVisualState.CONNECTING
            incoming != null -> CallVisualState.INCOMING
            outgoing != null -> CallVisualState.OUTGOING
            else -> CallVisualState.READY
        }

        val status = when (visualState) {
            CallVisualState.LIVE -> "Live with $peer"
            CallVisualState.CONNECTING -> "Connecting live audio…"
            CallVisualState.INCOMING -> "$peer is calling"
            CallVisualState.OUTGOING -> "Calling $peer…"
            CallVisualState.READY -> StateStore.status(this)
        }

        uiState = HappyTalkieUiState(
            status = status,
            callState = visualState,
            recording = recording,
            callEnabled =
                outgoing == null || active != null || incoming != null,
            talkEnabled = !hasAnyCallState(),
            peerName = peer,
            messages = VoiceMessageStore.list(this, limit = 30)
        )
    }

    private fun hasAnyCallState(): Boolean =
        StateStore.incomingCall(this) != null ||
            StateStore.outgoingCall(this) != null ||
            StateStore.activeCall(this) != null

    private fun requestNeededPermissions() {
        val missing = mutableListOf<String>()

        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missing += Manifest.permission.RECORD_AUDIO
        }

        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missing += Manifest.permission.POST_NOTIFICATIONS
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun peerName(): String =
        if (role == EndpointRole.PHONE) "Watch" else "Phone"

    companion object {
        private const val REQUEST_PERMISSIONS = 42
    }
}
