package com.xiaolong.happytalkie.core

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var role: EndpointRole
    private lateinit var transport: DataLayerTransport
    private lateinit var recorder: AudioRecorder
    private lateinit var statusView: TextView
    private lateinit var callButton: Button
    private lateinit var talkButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private var recording = false
    private var receiverRegistered = false
    private var recordingTimeout: Runnable? = null
    private var callTimeout: Runnable? = null

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        role = EndpointRole.fromContext(this)
        transport = DataLayerTransport(this)
        recorder = AudioRecorder(this)
        buildUi()
        requestNeededPermissions()
        refreshUi()
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
        refreshUi()
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

    private fun buildUi() {
        val isWatch = packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
        val padding = dp(if (isWatch) 12 else 28)
        val gap = dp(if (isWatch) 7 else 14)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.rgb(16, 16, 18))
        }

        val title = TextView(this).apply {
            text = "HappyTalkie"
            setTextColor(Color.WHITE)
            textSize = if (isWatch) 20f else 28f
            gravity = Gravity.CENTER
        }

        statusView = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = if (isWatch) 13f else 17f
            gravity = Gravity.CENTER
            setPadding(0, gap, 0, gap)
        }

        callButton = Button(this).apply {
            textSize = if (isWatch) 18f else 26f
            setAllCaps(false)
            setOnClickListener { handleCallButton() }
        }

        talkButton = Button(this).apply {
            textSize = if (isWatch) 18f else 26f
            setAllCaps(false)
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        beginRecording()
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        finishRecording()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        cancelRecording()
                        true
                    }
                    else -> true
                }
            }
        }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            statusView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            callButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { bottomMargin = gap }
        )
        root.addView(
            talkButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun handleCallButton() {
        val incoming = StateStore.incomingCall(this)
        val active = StateStore.activeCall(this)

        when {
            incoming != null -> answerCall(incoming)
            active != null -> endCall(active)
            StateStore.outgoingCall(this) == null -> startCall()
        }
    }

    private fun startCall() {
        val callId = UUID.randomUUID().toString()
        val peer = peerName()

        StateStore.setOutgoingCall(this, callId)
        StateStore.setStatus(this, "Calling " + peer + "…")
        refreshUi()

        transport.sendSignal(Protocol.CALL_RING, callId) { sent ->
            if (StateStore.outgoingCall(this) != callId) return@sendSignal

            if (!sent) {
                StateStore.setOutgoingCall(this, null)
                StateStore.setStatus(
                    this,
                    peer + " offline — hold TALK to leave a message"
                )
                refreshUi()
                return@sendSignal
            }

            StateStore.setStatus(this, "Ringing " + peer + "…")
            refreshUi()

            callTimeout?.let(handler::removeCallbacks)
            callTimeout = Runnable {
                if (
                    StateStore.outgoingCall(this) == callId &&
                    StateStore.activeCall(this) == null
                ) {
                    StateStore.setOutgoingCall(this, null)
                    StateStore.setStatus(
                        this,
                        "No answer — hold TALK to leave a message"
                    )
                    refreshUi()
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
        StateStore.setActiveCall(this, callId)
        StateStore.setStatus(this, "Connected — hold TALK to speak")
        refreshUi()

        transport.sendSignal(Protocol.CALL_ANSWER, callId) { sent ->
            if (!sent && StateStore.activeCall(this) == callId) {
                StateStore.setActiveCall(this, null)
                StateStore.setStatus(
                    this,
                    peerName() + " unreachable — hold TALK to leave a message"
                )
                refreshUi()
            }
        }
    }

    private fun endCall(callId: String) {
        StateStore.clearCallState(this)
        StateStore.setStatus(this, "Call ended")
        AlertController.stop(this)
        AudioPlayer.stop()
        refreshUi()
        transport.sendSignal(Protocol.CALL_END, callId) { }
    }

    private fun beginRecording() {
        if (recording) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_PERMISSIONS)
            StateStore.setStatus(this, "Microphone permission required")
            refreshUi()
            return
        }

        if (recorder.start()) {
            recording = true
            StateStore.setStatus(this, "Recording… release TALK to send")
            refreshUi()
            recordingTimeout?.let(handler::removeCallbacks)
            recordingTimeout = Runnable {
                recordingTimeout = null
                if (recording) finishRecording()
            }.also {
                handler.postDelayed(it, Protocol.MAX_RECORDING_MS.toLong())
            }
        } else {
            StateStore.setStatus(this, "Could not start microphone")
            refreshUi()
        }
    }

    private fun finishRecording() {
        if (!recording) return
        recording = false
        recordingTimeout?.let(handler::removeCallbacks)
        recordingTimeout = null

        val file = recorder.stop()
        if (file == null) {
            StateStore.setStatus(this, "Recording was too short — try again")
            refreshUi()
            return
        }

        val callId = StateStore.activeCall(this)
        StateStore.setStatus(
            this,
            if (callId != null) "Sending voice…" else "Queueing voice message…"
        )
        refreshUi()

        transport.queueVoice(file, callId, role) { queued ->
            StateStore.setStatus(
                this,
                when {
                    !queued -> "Could not queue voice — try again"
                    StateStore.activeCall(this) != null ->
                        "Sent — hold TALK when you want to speak"
                    else -> "Voice message queued for delivery"
                }
            )
            refreshUi()
        }
    }

    private fun cancelRecording() {
        if (!recording) return
        recording = false
        recordingTimeout?.let(handler::removeCallbacks)
        recordingTimeout = null
        recorder.cancel()
        StateStore.setStatus(this, "Recording cancelled")
        refreshUi()
    }

    private fun refreshUi() {
        if (!::statusView.isInitialized) return
        statusView.text = StateStore.status(this)

        callButton.text = when {
            StateStore.incomingCall(this) != null -> "🔔 ANSWER"
            StateStore.activeCall(this) != null -> "🔔 END"
            StateStore.outgoingCall(this) != null -> "🔔 CALLING…"
            else -> "🔔 CALL"
        }
        callButton.isEnabled = StateStore.outgoingCall(this) == null || 
            StateStore.activeCall(this) != null ||
            StateStore.incomingCall(this) != null

        talkButton.text =
            if (recording) "🎤 RELEASE TO SEND" else "🎤 TALK"
    }

    private fun requestNeededPermissions() {
        val missing = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_PERMISSIONS = 42
    }
}
