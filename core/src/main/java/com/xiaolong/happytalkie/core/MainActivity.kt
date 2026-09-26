package com.xiaolong.happytalkie.core

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var role: EndpointRole
    private lateinit var transport: DataLayerTransport
    private lateinit var recorder: AudioRecorder

    private lateinit var statusView: TextView
    private lateinit var callButton: Button
    private lateinit var talkButton: Button
    private lateinit var conversationList: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var recording = false
    private var receiverRegistered = false
    private var recordingTimeout: Runnable? = null
    private var callTimeout: Runnable? = null
    private var isWatch = false

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
        isWatch = packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

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
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(BACKGROUND)
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(if (isWatch) 12 else 24),
                dp(if (isWatch) 10 else 26),
                dp(if (isWatch) 12 else 24),
                dp(if (isWatch) 18 else 30)
            )
        }

        val brand = ImageView(this).apply {
            setImageResource(com.xiaolong.happytalkie.core.R.drawable.ic_happytalkie_brand)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "HappyTalkie"
        }

        val title = TextView(this).apply {
            text = "HappyTalkie"
            setTextColor(Color.WHITE)
            textSize = if (isWatch) 18f else 28f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        statusView = TextView(this).apply {
            setTextColor(TEXT_SECONDARY)
            textSize = if (isWatch) 12f else 15f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(7), dp(12), dp(7))
            background = rounded(SURFACE, if (isWatch) 18 else 22)
        }

        callButton = Button(this).apply {
            textSize = if (isWatch) 18f else 27f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setAllCaps(false)
            stateListAnimator = null
            elevation = dp(if (isWatch) 2 else 5).toFloat()
            backgroundTintList = null
            setOnClickListener { handleCallButton() }
        }

        talkButton = Button(this).apply {
            textSize = if (isWatch) 16f else 20f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setAllCaps(false)
            stateListAnimator = null
            elevation = 0f
            backgroundTintList = null
            background = rounded(SURFACE_RAISED, 22)
            setOnTouchListener { _, event ->
                if (!isEnabled) return@setOnTouchListener true
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

        val historyTitle = TextView(this).apply {
            text = if (isWatch) "VOICE MESSAGES" else "Voice messages"
            setTextColor(TEXT_SECONDARY)
            textSize = if (isWatch) 10f else 14f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.START
        }

        conversationList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(
            brand,
            LinearLayout.LayoutParams(
                dp(if (isWatch) 52 else 76),
                dp(if (isWatch) 52 else 76)
            )
        )

        content.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(if (isWatch) 2 else 4)
            }
        )

        content.addView(
            statusView,
            LinearLayout.LayoutParams(
                if (isWatch) ViewGroup.LayoutParams.MATCH_PARENT else dp(300),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(if (isWatch) 7 else 12)
            }
        )

        content.addView(
            callButton,
            LinearLayout.LayoutParams(
                dp(if (isWatch) 122 else 188),
                dp(if (isWatch) 122 else 188)
            ).apply {
                topMargin = dp(if (isWatch) 12 else 24)
            }
        )

        content.addView(
            talkButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(if (isWatch) 52 else 64)
            ).apply {
                topMargin = dp(if (isWatch) 8 else 16)
            }
        )

        content.addView(
            historyTitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(if (isWatch) 15 else 26)
                bottomMargin = dp(7)
            }
        )

        content.addView(
            conversationList,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        scroll.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(scroll)
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

        StateStore.setCallInitiator(this, true)
        StateStore.setOutgoingCall(this, callId)
        StateStore.setStatus(this, "Calling $peer…")
        LiveCallService.start(this)
        refreshUi()

        transport.sendSignal(Protocol.CALL_RING, callId) { sent ->
            if (StateStore.outgoingCall(this) != callId) return@sendSignal

            if (!sent) {
                StateStore.clearCallState(this)
                StateStore.setStatus(this, "$peer is offline · leave a TALK")
                LiveCallService.stop(this)
                refreshUi()
                return@sendSignal
            }

            StateStore.setStatus(this, "Ringing $peer…")
            refreshUi()

            callTimeout?.let(handler::removeCallbacks)
            callTimeout = Runnable {
                if (
                    StateStore.outgoingCall(this) == callId &&
                    StateStore.activeCall(this) == null
                ) {
                    StateStore.clearCallState(this)
                    StateStore.setStatus(this, "No answer · leave a TALK")
                    LiveCallService.stop(this)
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
        StateStore.setCallInitiator(this, false)
        StateStore.setActiveCall(this, callId)
        StateStore.setStatus(this, "Connecting live audio…")
        LiveCallService.start(this)
        EventBus.notifyStateChanged(this)
        refreshUi()

        transport.sendSignal(Protocol.CALL_ANSWER, callId) { sent ->
            if (!sent && StateStore.activeCall(this) == callId) {
                StateStore.clearCallState(this)
                StateStore.setStatus(this, peerName() + " is unreachable")
                LiveCallService.stop(this)
                refreshUi()
            }
        }
    }

    private fun endCall(callId: String) {
        StateStore.clearCallState(this)
        StateStore.setStatus(this, "Call ended")
        AlertController.stop(this)
        LiveCallAudio.stop(this)
        LiveCallService.stop(this)
        refreshUi()
        transport.sendSignal(Protocol.CALL_END, callId) { }
    }

    private fun beginRecording() {
        if (recording || hasAnyCallState()) return

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_PERMISSIONS
            )
            StateStore.setStatus(this, "Microphone permission required")
            refreshUi()
            return
        }

        if (recorder.start()) {
            recording = true
            StateStore.setStatus(this, "Recording TALK…")
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

        val temp = recorder.stop()
        if (temp == null) {
            StateStore.setStatus(this, "Recording was too short")
            refreshUi()
            return
        }

        val saved = try {
            VoiceMessageStore.saveOutgoing(this, temp)
        } catch (_: Exception) {
            temp.delete()
            StateStore.setStatus(this, "Could not save voice message")
            refreshUi()
            return
        }

        StateStore.setStatus(this, "Sending TALK…")
        refreshUi()

        transport.queueVoice(saved, role) { queued ->
            StateStore.setStatus(
                this,
                if (queued) "TALK queued for delivery"
                else "Saved locally · delivery failed"
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

        val incoming = StateStore.incomingCall(this)
        val outgoing = StateStore.outgoingCall(this)
        val active = StateStore.activeCall(this)
        val live = LiveCallAudio.isRunning()

        statusView.text = when {
            active != null && live -> "●  LIVE AUDIO"
            active != null -> "Connecting live audio…"
            incoming != null -> "Incoming call"
            outgoing != null -> "Calling " + peerName() + "…"
            else -> StateStore.status(this)
        }
        statusView.setTextColor(
            if (active != null && live) LIVE_COLOR else TEXT_SECONDARY
        )

        when {
            incoming != null -> styleCallButton("☎  ANSWER", ANSWER_COLOR)
            active != null -> styleCallButton("■  END", END_COLOR)
            outgoing != null -> styleCallButton("☎  CALLING", CALLING_COLOR)
            else -> styleCallButton("☎  CALL", CALL_COLOR)
        }

        callButton.isEnabled = outgoing == null || active != null || incoming != null
        callButton.alpha = if (callButton.isEnabled) 1f else 0.72f

        val busy = hasAnyCallState()
        talkButton.isEnabled = !busy
        talkButton.alpha = if (busy) 0.42f else 1f
        talkButton.text = when {
            active != null -> "◉  LIVE AUDIO"
            outgoing != null || incoming != null -> "TALK available after call"
            recording -> "●  RELEASE TO SEND"
            else -> "●  HOLD TO TALK"
        }

        renderConversation()
    }

    private fun renderConversation() {
        conversationList.removeAllViews()
        val messages = VoiceMessageStore
            .list(this, if (isWatch) 8 else 30)
            .reversed()

        if (messages.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No voice messages yet"
                setTextColor(TEXT_MUTED)
                textSize = if (isWatch) 11f else 14f
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(14), dp(8), dp(14))
            }
            conversationList.addView(
                empty,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            return
        }

        messages.forEach { message ->
            conversationList.addView(messageBubble(message))
        }
    }

    private fun messageBubble(message: VoiceMessage): View {
        val outgoing = message.direction == VoiceDirection.OUTGOING

        val wrapper = LinearLayout(this).apply {
            gravity = if (outgoing) Gravity.END else Gravity.START
            orientation = LinearLayout.VERTICAL
        }

        val bubble = TextView(this).apply {
            text = buildString {
                append("▶  ")
                append(if (outgoing) "Me" else peerName())
                append("  ·  ")
                append(message.displayTime())
            }
            setTextColor(Color.WHITE)
            textSize = if (isWatch) 12f else 15f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(if (isWatch) 10 else 14),
                dp(if (isWatch) 9 else 12),
                dp(if (isWatch) 10 else 14),
                dp(if (isWatch) 9 else 12)
            )
            background = rounded(
                if (outgoing) MESSAGE_OUT else MESSAGE_IN,
                if (isWatch) 18 else 20
            )
            setOnClickListener {
                AudioPlayer.play(this@MainActivity, message.file, deleteAfter = false)
            }
        }

        wrapper.addView(
            bubble,
            LinearLayout.LayoutParams(
                if (isWatch) ViewGroup.LayoutParams.MATCH_PARENT else dp(270),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        wrapper.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(if (isWatch) 6 else 9)
        }
        return wrapper
    }

    private fun styleCallButton(label: String, color: Int) {
        callButton.text = label
        callButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dp(1), lighten(color))
        }
    }

    private fun hasAnyCallState(): Boolean =
        StateStore.incomingCall(this) != null ||
            StateStore.outgoingCall(this) != null ||
            StateStore.activeCall(this) != null

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

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }

    private fun lighten(color: Int): Int {
        val factor = 1.16f
        return Color.rgb(
            (Color.red(color) * factor).toInt().coerceAtMost(255),
            (Color.green(color) * factor).toInt().coerceAtMost(255),
            (Color.blue(color) * factor).toInt().coerceAtMost(255)
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_PERMISSIONS = 42

        private val BACKGROUND = Color.rgb(12, 15, 20)
        private val SURFACE = Color.rgb(27, 32, 40)
        private val SURFACE_RAISED = Color.rgb(38, 44, 54)
        private val TEXT_SECONDARY = Color.rgb(184, 193, 207)
        private val TEXT_MUTED = Color.rgb(123, 133, 149)

        private val CALL_COLOR = Color.rgb(53, 105, 255)
        private val CALLING_COLOR = Color.rgb(66, 83, 125)
        private val ANSWER_COLOR = Color.rgb(35, 168, 100)
        private val END_COLOR = Color.rgb(222, 67, 76)
        private val LIVE_COLOR = Color.rgb(101, 226, 158)

        private val MESSAGE_OUT = Color.rgb(47, 91, 190)
        private val MESSAGE_IN = Color.rgb(43, 49, 60)
    }
}
