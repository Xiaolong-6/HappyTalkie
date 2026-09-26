package com.xiaolong.happytalkie.core

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var role: EndpointRole
    private lateinit var transport: DataLayerTransport
    private lateinit var recorder: AudioRecorder

    private lateinit var statusShell: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var statusPeerView: TextView
    private lateinit var callButton: TextView
    private lateinit var talkButton: TextView
    private lateinit var conversationList: LinearLayout
    private lateinit var historyTitle: TextView

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
        if (isWatch) buildWatchUi() else buildPhoneUi()
    }

    private fun buildPhoneUi() {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(PHONE_BACKGROUND)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(36))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            brandBadge(48),
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, 0, 0)
        }
        titles.addView(textView("HappyTalkie", 29f, Color.WHITE, true))
        titles.addView(
            textView("Phone ↔ Watch", 13f, TEXT_MUTED).apply {
                setPadding(0, dp(2), 0, 0)
            }
        )
        header.addView(
            titles,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        content.addView(header)

        createStatusShell(watch = false)
        content.addView(
            statusShell,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(17) }
        )

        val callSection = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val callCopy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        callCopy.addView(sectionLabel("LIVE CALL", ACCENT_YELLOW))
        callCopy.addView(
            textView("Talk now", 25f, Color.WHITE, true).apply {
                setPadding(0, dp(5), 0, 0)
            }
        )
        callCopy.addView(
            textView(
                "Ring the Watch and speak live",
                14f,
                TEXT_SECONDARY
            ).apply {
                setPadding(0, dp(4), dp(8), 0)
            }
        )
        callSection.addView(
            callCopy,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        callButton = createCallButton(watch = false)
        callSection.addView(
            callButton,
            LinearLayout.LayoutParams(dp(86), dp(86))
        )
        content.addView(
            callSection,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(27) }
        )

        content.addView(
            divider(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply { topMargin = dp(27) }
        )

        val talkHeader = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        talkHeader.addView(sectionLabel("TALK", ACCENT_CYAN))
        talkHeader.addView(
            textView("Voice message", 25f, Color.WHITE, true).apply {
                setPadding(0, dp(5), 0, 0)
            }
        )
        talkHeader.addView(
            textView(
                "Hold to record • sends when connection returns",
                14f,
                TEXT_SECONDARY
            ).apply {
                setPadding(0, dp(4), 0, 0)
            }
        )
        content.addView(
            talkHeader,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(24) }
        )

        talkButton = createTalkButton(watch = false)
        content.addView(
            talkButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(64)
            ).apply { topMargin = dp(16) }
        )

        val historyHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        historyTitle = textView("Recent TALK", 20f, Color.WHITE, true)
        val historyHint = textView("Tap to play", 12f, TEXT_MUTED).apply {
            gravity = Gravity.END
        }
        historyHeader.addView(
            historyTitle,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        historyHeader.addView(historyHint)
        content.addView(
            historyHeader,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(31)
                bottomMargin = dp(10)
            }
        )

        conversationList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
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

    private fun buildWatchUi() {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(WATCH_BACKGROUND)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(9), dp(8), dp(9), dp(12))
        }

        createStatusShell(watch = true)
        statusPeerView.visibility = View.GONE
        content.addView(
            statusShell,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(34)
            )
        )

        callButton = createCallButton(watch = true)
        content.addView(
            callButton,
            LinearLayout.LayoutParams(dp(76), dp(76)).apply {
                topMargin = dp(7)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        talkButton = createTalkButton(watch = true)
        content.addView(
            talkButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply { topMargin = dp(7) }
        )

        historyTitle = textView("RECENT TALK", 10f, TEXT_MUTED, true).apply {
            gravity = Gravity.START
            letterSpacing = 0.10f
        }
        content.addView(
            historyTitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(5)
            }
        )

        conversationList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
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

    private fun createStatusShell(watch: Boolean) {
        statusShell = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(if (watch) 10 else 14),
                0,
                dp(if (watch) 10 else 14),
                0
            )
            background = rounded(SURFACE, if (watch) 17 else 22)
        }

        val dot = View(this).apply {
            background = oval(READY_DOT)
        }
        statusShell.addView(
            dot,
            LinearLayout.LayoutParams(
                dp(if (watch) 8 else 9),
                dp(if (watch) 8 else 9)
            ).apply {
                rightMargin = dp(if (watch) 7 else 9)
            }
        )

        statusView = textView(
            "Ready",
            if (watch) 11f else 14f,
            TEXT_SECONDARY,
            true
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        statusShell.addView(
            statusView,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        statusPeerView = textView(
            if (role == EndpointRole.PHONE) "Watch" else "Phone",
            if (watch) 10f else 12f,
            TEXT_MUTED
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        statusShell.addView(
            statusPeerView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun brandBadge(sizeDp: Int): FrameLayout =
        FrameLayout(this).apply {
            background = rounded(CALL_COLOR, 14)
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND

            val image = ImageView(this@MainActivity).apply {
                setImageResource(
                    com.xiaolong.happytalkie.core.R.drawable.ic_happytalkie_brand
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "HappyTalkie"
            }
            addView(
                image,
                FrameLayout.LayoutParams(
                    dp(sizeDp + 12),
                    dp(sizeDp + 12),
                    Gravity.CENTER
                )
            )
        }

    private fun sectionLabel(label: String, color: Int): TextView =
        textView(label, 11f, color, true).apply {
            letterSpacing = 0.13f
        }

    private fun divider(): View =
        View(this).apply {
            setBackgroundColor(DIVIDER)
        }

    private fun createCallButton(watch: Boolean): TextView =
        TextView(this).apply {
            textSize = if (watch) 12f else 13f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            elevation = dp(if (watch) 2 else 4).toFloat()
            isClickable = true
            isFocusable = true
            drawablePadding = dp(if (watch) 1 else 3)
            setOnClickListener { handleCallButton() }
        }

    private fun createTalkButton(watch: Boolean): TextView =
        TextView(this).apply {
            textSize = if (watch) 13f else 18f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(18), 0, dp(18), 0)
            elevation = dp(if (watch) 1 else 3).toFloat()
            isClickable = true
            isFocusable = true
            drawablePadding = dp(if (watch) 7 else 10)
            background = roundedGradient(
                TALK_COLOR_LIGHT,
                TALK_COLOR,
                if (watch) 23 else 24
            )
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

    private fun textView(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) {
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }
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

        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
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

        val status = when {
            active != null && live -> "Live with ${peerName()}"
            active != null -> "Connecting live audio…"
            incoming != null -> "${peerName()} is calling"
            outgoing != null -> "Calling ${peerName()}…"
            else -> StateStore.status(this)
        }
        statusView.text = status

        val statusColor = when {
            recording -> RECORDING_SURFACE
            active != null && live -> LIVE_SURFACE
            incoming != null -> INCOMING_SURFACE
            status.contains("offline", ignoreCase = true) ||
                status.contains("unreachable", ignoreCase = true) ||
                status.contains("failed", ignoreCase = true) -> ERROR_SURFACE
            else -> SURFACE
        }
        statusShell.background = rounded(
            statusColor,
            if (isWatch) 17 else 22
        )
        statusView.setTextColor(
            when {
                active != null && live -> LIVE_COLOR
                recording -> RECORDING_TEXT
                incoming != null -> ANSWER_TEXT
                else -> TEXT_SECONDARY
            }
        )

        when {
            incoming != null -> styleCallButton(
                "ANSWER",
                ANSWER_COLOR,
                GlyphKind.PHONE
            )
            active != null -> styleCallButton(
                "END",
                END_COLOR,
                GlyphKind.STOP
            )
            outgoing != null -> styleCallButton(
                "CALLING",
                CALLING_COLOR,
                GlyphKind.PHONE
            )
            else -> styleCallButton(
                "CALL",
                CALL_COLOR,
                GlyphKind.PHONE
            )
        }

        callButton.isEnabled =
            outgoing == null || active != null || incoming != null
        callButton.alpha = if (callButton.isEnabled) 1f else 0.58f

        val busy = hasAnyCallState()
        talkButton.isEnabled = !busy
        talkButton.alpha = if (busy) 0.40f else 1f

        talkButton.text = when {
            active != null -> "LIVE AUDIO"
            outgoing != null || incoming != null -> "TALK AFTER CALL"
            recording -> "RELEASE TO SEND"
            else -> "HOLD TO TALK"
        }
        talkButton.background = when {
            busy -> rounded(
                SURFACE_RAISED,
                if (isWatch) 23 else 24
            )
            recording -> roundedGradient(
                RECORDING_LIGHT,
                RECORDING_COLOR,
                if (isWatch) 23 else 24
            )
            else -> roundedGradient(
                TALK_COLOR_LIGHT,
                TALK_COLOR,
                if (isWatch) 23 else 24
            )
        }
        setTalkGlyph(
            if (recording) GlyphKind.STOP else GlyphKind.MIC
        )

        if (isWatch) {
            historyTitle.alpha = if (recording || busy) 0.50f else 1f
            conversationList.alpha = if (recording || busy) 0.50f else 1f
        }

        renderConversation()
    }

    private fun setTalkGlyph(kind: GlyphKind) {
        val size = dp(if (isWatch) 19 else 24)
        val icon = GlyphDrawable(kind, Color.WHITE).apply {
            setBounds(0, 0, size, size)
        }
        talkButton.setCompoundDrawables(icon, null, null, null)
    }

    private fun renderConversation() {
        conversationList.removeAllViews()
        val messages = VoiceMessageStore
            .list(this, if (isWatch) 4 else 30)
            .reversed()

        if (messages.isEmpty()) {
            conversationList.addView(emptyHistoryRow())
            return
        }

        messages.forEach { voice ->
            conversationList.addView(messageRow(voice))
        }
    }

    private fun emptyHistoryRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(if (isWatch) 8 else 10),
                dp(if (isWatch) 7 else 10),
                dp(if (isWatch) 8 else 10),
                dp(if (isWatch) 7 else 10)
            )
            background = rounded(
                HISTORY_SURFACE,
                if (isWatch) 16 else 20
            )
        }

        val icon = TextView(this).apply {
            gravity = Gravity.CENTER
            background = oval(SURFACE_RAISED)
            val drawable = GlyphDrawable(
                GlyphKind.PLAY,
                TEXT_MUTED
            ).apply {
                val s = dp(if (isWatch) 13 else 17)
                setBounds(0, 0, s, s)
            }
            setCompoundDrawables(drawable, null, null, null)
        }
        row.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(if (isWatch) 30 else 38),
                dp(if (isWatch) 30 else 38)
            )
        )

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(if (isWatch) 8 else 11), 0, 0, 0)
        }
        copy.addView(
            textView(
                "No messages yet",
                if (isWatch) 10f else 14f,
                TEXT_SECONDARY,
                true
            )
        )
        if (!isWatch) {
            copy.addView(
                textView(
                    "Your TALK history will appear here",
                    12f,
                    TEXT_MUTED
                ).apply {
                    setPadding(0, dp(3), 0, 0)
                }
            )
        }
        row.addView(
            copy,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        return row
    }

    private fun messageRow(voice: VoiceMessage): View {
        val outgoing = voice.direction == VoiceDirection.OUTGOING
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(if (isWatch) 8 else 10),
                dp(if (isWatch) 7 else 9),
                dp(if (isWatch) 8 else 10),
                dp(if (isWatch) 7 else 9)
            )
            background = rounded(
                HISTORY_SURFACE,
                if (isWatch) 16 else 18
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                AudioPlayer.play(
                    this@MainActivity,
                    voice.file,
                    deleteAfter = false
                )
            }
        }

        val play = TextView(this).apply {
            gravity = Gravity.CENTER
            background = oval(
                if (outgoing) MESSAGE_OUT else MESSAGE_IN
            )
            val drawable = GlyphDrawable(
                GlyphKind.PLAY,
                Color.WHITE
            ).apply {
                val s = dp(if (isWatch) 13 else 16)
                setBounds(0, 0, s, s)
            }
            setCompoundDrawables(drawable, null, null, null)
        }
        row.addView(
            play,
            LinearLayout.LayoutParams(
                dp(if (isWatch) 30 else 38),
                dp(if (isWatch) 30 else 38)
            )
        )

        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(if (isWatch) 8 else 11), 0, 0, 0)
        }
        copy.addView(
            textView(
                if (outgoing) "Me" else peerName(),
                if (isWatch) 10f else 14f,
                Color.WHITE,
                true
            )
        )
        copy.addView(
            textView(
                "TALK  ·  ${voice.displayTime()}",
                if (isWatch) 9f else 12f,
                TEXT_MUTED
            ).apply {
                setPadding(0, dp(2), 0, 0)
            }
        )
        row.addView(
            copy,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        if (!isWatch) {
            row.addView(
                textView("Play", 12f, TEXT_MUTED, true).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.END
                }
            )
        }

        row.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(if (isWatch) 5 else 7)
        }
        return row
    }

    private fun styleCallButton(
        label: String,
        color: Int,
        kind: GlyphKind
    ) {
        callButton.text = label
        callButton.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(lighten(color), color)
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(
                dp(if (isWatch) 1 else 2),
                withAlpha(lighten(color), 150)
            )
        }

        val size = dp(if (isWatch) 23 else 26)
        val icon = GlyphDrawable(kind, Color.WHITE).apply {
            setBounds(0, 0, size, size)
        }
        callButton.setCompoundDrawables(
            null,
            icon,
            null,
            null
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
            requestPermissions(
                missing.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun peerName(): String =
        if (role == EndpointRole.PHONE) "Watch" else "Phone"

    private fun rounded(
        color: Int,
        radiusDp: Int
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }

    private fun oval(color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }

    private fun roundedGradient(
        startColor: Int,
        endColor: Int,
        radiusDp: Int
    ): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun lighten(color: Int): Int {
        val factor = 1.14f
        return Color.rgb(
            (Color.red(color) * factor).toInt().coerceAtMost(255),
            (Color.green(color) * factor).toInt().coerceAtMost(255),
            (Color.blue(color) * factor).toInt().coerceAtMost(255)
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private enum class GlyphKind {
        PHONE,
        MIC,
        PLAY,
        STOP
    }

    private class GlyphDrawable(
        private val kind: GlyphKind,
        private val color: Int
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = this@GlyphDrawable.color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val size = minOf(b.width(), b.height()).toFloat()
            if (size <= 0f) return

            val left = b.left + (b.width() - size) / 2f
            val top = b.top + (b.height() - size) / 2f
            canvas.save()
            canvas.translate(left, top)

            when (kind) {
                GlyphKind.PHONE -> drawPhone(canvas, size)
                GlyphKind.MIC -> drawMic(canvas, size)
                GlyphKind.PLAY -> drawPlay(canvas, size)
                GlyphKind.STOP -> drawStop(canvas, size)
            }

            canvas.restore()
        }

        private fun drawPhone(canvas: Canvas, s: Float) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = s * 0.105f

            val p = Path().apply {
                moveTo(s * 0.24f, s * 0.22f)
                cubicTo(
                    s * 0.17f,
                    s * 0.35f,
                    s * 0.31f,
                    s * 0.57f,
                    s * 0.49f,
                    s * 0.72f
                )
                cubicTo(
                    s * 0.64f,
                    s * 0.84f,
                    s * 0.78f,
                    s * 0.84f,
                    s * 0.86f,
                    s * 0.73f
                )
            }
            canvas.drawPath(p, paint)
            canvas.drawLine(
                s * 0.20f,
                s * 0.22f,
                s * 0.33f,
                s * 0.30f,
                paint
            )
            canvas.drawLine(
                s * 0.73f,
                s * 0.67f,
                s * 0.86f,
                s * 0.74f,
                paint
            )
        }

        private fun drawMic(canvas: Canvas, s: Float) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = s * 0.095f

            val capsule = RectF(
                s * 0.34f,
                s * 0.12f,
                s * 0.66f,
                s * 0.58f
            )
            canvas.drawRoundRect(
                capsule,
                s * 0.16f,
                s * 0.16f,
                paint
            )
            canvas.drawArc(
                RectF(
                    s * 0.23f,
                    s * 0.30f,
                    s * 0.77f,
                    s * 0.78f
                ),
                0f,
                180f,
                false,
                paint
            )
            canvas.drawLine(
                s * 0.50f,
                s * 0.77f,
                s * 0.50f,
                s * 0.91f,
                paint
            )
            canvas.drawLine(
                s * 0.36f,
                s * 0.91f,
                s * 0.64f,
                s * 0.91f,
                paint
            )
        }

        private fun drawPlay(canvas: Canvas, s: Float) {
            paint.style = Paint.Style.FILL
            val p = Path().apply {
                moveTo(s * 0.34f, s * 0.23f)
                lineTo(s * 0.76f, s * 0.50f)
                lineTo(s * 0.34f, s * 0.77f)
                close()
            }
            canvas.drawPath(p, paint)
        }

        private fun drawStop(canvas: Canvas, s: Float) {
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(
                RectF(
                    s * 0.28f,
                    s * 0.28f,
                    s * 0.72f,
                    s * 0.72f
                ),
                s * 0.08f,
                s * 0.08f,
                paint
            )
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 42

        private val PHONE_BACKGROUND = Color.rgb(5, 10, 22)
        private val WATCH_BACKGROUND = Color.rgb(0, 0, 0)
        private val SURFACE = Color.rgb(18, 29, 55)
        private val SURFACE_RAISED = Color.rgb(31, 46, 76)
        private val HISTORY_SURFACE = Color.rgb(12, 20, 38)
        private val DIVIDER = Color.rgb(29, 43, 70)

        private val TEXT_SECONDARY = Color.rgb(196, 207, 226)
        private val TEXT_MUTED = Color.rgb(126, 142, 170)

        private val ACCENT_YELLOW = Color.rgb(255, 205, 55)
        private val ACCENT_CYAN = Color.rgb(83, 201, 255)
        private val READY_DOT = Color.rgb(91, 224, 146)

        private val CALL_COLOR = Color.rgb(31, 111, 255)
        private val CALLING_COLOR = Color.rgb(73, 97, 153)
        private val ANSWER_COLOR = Color.rgb(25, 184, 111)
        private val END_COLOR = Color.rgb(235, 68, 83)
        private val LIVE_COLOR = Color.rgb(112, 240, 171)

        private val TALK_COLOR_LIGHT = Color.rgb(28, 202, 250)
        private val TALK_COLOR = Color.rgb(27, 126, 255)
        private val RECORDING_LIGHT = Color.rgb(255, 92, 110)
        private val RECORDING_COLOR = Color.rgb(224, 44, 64)

        private val LIVE_SURFACE = Color.rgb(19, 57, 45)
        private val INCOMING_SURFACE = Color.rgb(19, 65, 47)
        private val ERROR_SURFACE = Color.rgb(72, 31, 40)
        private val RECORDING_SURFACE = Color.rgb(75, 29, 42)
        private val RECORDING_TEXT = Color.rgb(255, 190, 198)
        private val ANSWER_TEXT = Color.rgb(159, 247, 198)

        private val MESSAGE_OUT = Color.rgb(38, 104, 224)
        private val MESSAGE_IN = Color.rgb(49, 62, 87)
    }
}
