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
            setBackgroundColor(PHONE_BACKGROUND)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(32))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titles.addView(textView("HappyTalkie", 27f, Color.WHITE, true))
        titles.addView(
            textView("Call live. Talk anytime.", 13f, TEXT_SECONDARY).apply {
                setPadding(0, dp(2), 0, 0)
            }
        )
        header.addView(brandView(54), LinearLayout.LayoutParams(dp(54), dp(54)))
        header.addView(
            titles,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        content.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        statusView = createStatusView(false)
        content.addView(
            statusView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
        )

        callButton = createCallButton(false)
        content.addView(
            modeCard(
                eyebrow = "CALL",
                heading = "Live conversation",
                helper = "Ring the Watch and talk in real time.",
                action = callButton,
                actionWidth = dp(150),
                actionHeight = dp(150)
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        )

        talkButton = createTalkButton(false)
        content.addView(
            modeCard(
                eyebrow = "TALK",
                heading = "Voice message",
                helper = "Hold to record. Release to send — even if the Watch is temporarily offline.",
                action = talkButton,
                actionWidth = ViewGroup.LayoutParams.MATCH_PARENT,
                actionHeight = dp(68)
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        )

        val historyHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        historyTitle = textView("Recent TALK", 16f, Color.WHITE, true)
        val historyHint = textView("Tap to play", 12f, TEXT_MUTED).apply {
            gravity = Gravity.END
        }
        historyHeader.addView(
            historyTitle,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        historyHeader.addView(historyHint)
        content.addView(
            historyHeader,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
                bottomMargin = dp(9)
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
            setBackgroundColor(WATCH_BACKGROUND)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(10), dp(8), dp(10), dp(14))
        }

        statusView = createStatusView(true)
        content.addView(
            statusView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        )

        callButton = createCallButton(true)
        content.addView(
            callButton,
            LinearLayout.LayoutParams(dp(88), dp(88)).apply {
                topMargin = dp(6)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        talkButton = createTalkButton(true)
        content.addView(
            talkButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply { topMargin = dp(6) }
        )

        historyTitle = textView("RECENT TALK", 10f, TEXT_MUTED, true).apply {
            gravity = Gravity.START
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

    private fun brandView(sizeDp: Int): ImageView =
        ImageView(this).apply {
            setImageResource(com.xiaolong.happytalkie.core.R.drawable.ic_happytalkie_brand)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "HappyTalkie"
            layoutParams = ViewGroup.LayoutParams(dp(sizeDp), dp(sizeDp))
        }

    private fun createStatusView(watch: Boolean): TextView =
        TextView(this).apply {
            setTextColor(TEXT_SECONDARY)
            textSize = if (watch) 11f else 14f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(
                dp(if (watch) 8 else 12),
                dp(if (watch) 6 else 9),
                dp(if (watch) 8 else 12),
                dp(if (watch) 6 else 9)
            )
            background = rounded(SURFACE, if (watch) 18 else 20)
        }

    private fun createCallButton(watch: Boolean): Button =
        Button(this).apply {
            textSize = if (watch) 17f else 24f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setAllCaps(false)
            gravity = Gravity.CENTER
            stateListAnimator = null
            elevation = dp(if (watch) 2 else 5).toFloat()
            backgroundTintList = null
            minWidth = 0
            minHeight = 0
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { handleCallButton() }
        }

    private fun createTalkButton(watch: Boolean): Button =
        Button(this).apply {
            textSize = if (watch) 14f else 19f
            setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setAllCaps(false)
            gravity = Gravity.CENTER
            stateListAnimator = null
            elevation = dp(if (watch) 1 else 2).toFloat()
            backgroundTintList = null
            minWidth = 0
            minHeight = 0
            background = roundedGradient(
                TALK_COLOR_LIGHT,
                TALK_COLOR,
                if (watch) 24 else 22
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

    private fun modeCard(
        eyebrow: String,
        heading: String,
        helper: String,
        action: View,
        actionWidth: Int,
        actionHeight: Int
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(17), dp(18), dp(18))
            background = rounded(SURFACE, 24)

            addView(
                textView(eyebrow, 11f, ACCENT_YELLOW, true).apply {
                    letterSpacing = 0.12f
                    gravity = Gravity.CENTER
                }
            )
            addView(
                textView(heading, 20f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(5), 0, 0)
                }
            )
            addView(
                textView(helper, 13f, TEXT_SECONDARY).apply {
                    gravity = Gravity.CENTER
                    setLineSpacing(0f, 1.08f)
                    setPadding(dp(4), dp(5), dp(4), 0)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                action,
                LinearLayout.LayoutParams(actionWidth, actionHeight).apply {
                    topMargin = dp(16)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            )
        }

    private fun textView(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
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
            active != null && live -> "●  LIVE WITH ${peerName().uppercase()}"
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
            else -> SURFACE_RAISED
        }
        statusView.background = rounded(statusColor, if (isWatch) 18 else 20)
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
                if (isWatch) "☎\nANSWER" else "☎  ANSWER",
                ANSWER_COLOR
            )
            active != null -> styleCallButton(
                if (isWatch) "■\nEND" else "■  END",
                END_COLOR
            )
            outgoing != null -> styleCallButton(
                if (isWatch) "☎\nCALLING" else "☎  CALLING",
                CALLING_COLOR
            )
            else -> styleCallButton(
                if (isWatch) "☎\nCALL" else "☎  CALL",
                CALL_COLOR
            )
        }

        callButton.isEnabled = outgoing == null || active != null || incoming != null
        callButton.alpha = if (callButton.isEnabled) 1f else 0.62f

        val busy = hasAnyCallState()
        talkButton.isEnabled = !busy
        talkButton.alpha = if (busy) 0.38f else 1f
        talkButton.text = when {
            active != null -> if (isWatch) "LIVE AUDIO" else "◉  LIVE AUDIO"
            outgoing != null || incoming != null ->
                if (isWatch) "TALK AFTER CALL" else "TALK available after call"
            recording -> "●  RELEASE TO SEND"
            else -> "●  HOLD TO TALK"
        }
        talkButton.background = when {
            busy -> rounded(SURFACE_RAISED, if (isWatch) 24 else 22)
            recording -> roundedGradient(
                RECORDING_LIGHT,
                RECORDING_COLOR,
                if (isWatch) 24 else 22
            )
            else -> roundedGradient(
                TALK_COLOR_LIGHT,
                TALK_COLOR,
                if (isWatch) 24 else 22
            )
        }

        if (isWatch) {
            historyTitle.alpha = if (recording || busy) 0.46f else 1f
            conversationList.alpha = if (recording || busy) 0.46f else 1f
        }

        renderConversation()
    }

    private fun renderConversation() {
        conversationList.removeAllViews()
        val messages = VoiceMessageStore
            .list(this, if (isWatch) 4 else 30)
            .reversed()

        if (messages.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No TALK messages yet"
                setTextColor(TEXT_MUTED)
                textSize = if (isWatch) 10f else 13f
                gravity = Gravity.CENTER
                setPadding(
                    dp(8),
                    dp(if (isWatch) 8 else 14),
                    dp(8),
                    dp(if (isWatch) 8 else 14)
                )
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
            textSize = if (isWatch) 11f else 14f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(if (isWatch) 9 else 13),
                dp(if (isWatch) 8 else 11),
                dp(if (isWatch) 9 else 13),
                dp(if (isWatch) 8 else 11)
            )
            background = rounded(
                if (outgoing) MESSAGE_OUT else MESSAGE_IN,
                if (isWatch) 17 else 19
            )
            setOnClickListener {
                AudioPlayer.play(this@MainActivity, message.file, deleteAfter = false)
            }
        }

        wrapper.addView(
            bubble,
            LinearLayout.LayoutParams(
                if (isWatch) ViewGroup.LayoutParams.MATCH_PARENT else dp(286),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        wrapper.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(if (isWatch) 5 else 8)
        }
        return wrapper
    }

    private fun styleCallButton(label: String, color: Int) {
        callButton.text = label
        callButton.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(lighten(color), color)
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(if (isWatch) 2 else 1), lighten(lighten(color)))
        }
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

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
        }

    private fun roundedGradient(
        startColor: Int,
        endColor: Int,
        radiusDp: Int
    ): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
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

        private val PHONE_BACKGROUND = Color.rgb(7, 13, 29)
        private val WATCH_BACKGROUND = Color.rgb(0, 0, 0)
        private val SURFACE = Color.rgb(18, 29, 55)
        private val SURFACE_RAISED = Color.rgb(28, 42, 70)
        private val TEXT_SECONDARY = Color.rgb(196, 207, 226)
        private val TEXT_MUTED = Color.rgb(126, 142, 170)
        private val ACCENT_YELLOW = Color.rgb(255, 205, 55)

        private val CALL_COLOR = Color.rgb(31, 111, 255)
        private val CALLING_COLOR = Color.rgb(67, 88, 138)
        private val ANSWER_COLOR = Color.rgb(24, 183, 111)
        private val END_COLOR = Color.rgb(235, 68, 83)
        private val LIVE_COLOR = Color.rgb(103, 237, 165)

        private val TALK_COLOR_LIGHT = Color.rgb(27, 205, 255)
        private val TALK_COLOR = Color.rgb(20, 125, 255)
        private val RECORDING_LIGHT = Color.rgb(255, 93, 107)
        private val RECORDING_COLOR = Color.rgb(225, 45, 65)

        private val LIVE_SURFACE = Color.rgb(20, 59, 47)
        private val INCOMING_SURFACE = Color.rgb(21, 67, 49)
        private val ERROR_SURFACE = Color.rgb(70, 31, 39)
        private val RECORDING_SURFACE = Color.rgb(74, 27, 39)
        private val RECORDING_TEXT = Color.rgb(255, 188, 197)
        private val ANSWER_TEXT = Color.rgb(158, 246, 197)

        private val MESSAGE_OUT = Color.rgb(36, 100, 219)
        private val MESSAGE_IN = Color.rgb(30, 39, 58)
    }
}
