package com.xiaolong.happytalky.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class LiveCallService : Service() {
    private var receiverRegistered = false
    private val handler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    private var ringTimeoutRunnable: Runnable? = null

    private val stateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                updateFromState()
            }
        }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(
            NOTIFICATION_ID,
            notification("Preparing call…")
        )
        registerStateReceiver()
        updateFromState()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        updateFromState()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cancelRetry()
        cancelRingTimeout()

        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }

        LiveCallAudio.stop(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateFromState() {
        val activeCall = StateStore.activeCall(this)
        val outgoingCall = StateStore.outgoingCall(this)

        if (
            activeCall == null &&
            outgoingCall == null
        ) {
            stopSelf()
            return
        }

        val reconnecting =
            activeCall != null &&
                StateStore.reconnectUntil(this) > 0L &&
                !LiveCallAudio.isRunning()

        val manager =
            getSystemService(NotificationManager::class.java)

        manager?.notify(
            NOTIFICATION_ID,
            notification(
                when {
                    activeCall != null &&
                        LiveCallAudio.isRunning() ->
                        "Live call active"

                    reconnecting ->
                        "Reconnecting live audio…"

                    activeCall != null ->
                        "Connecting live audio…"

                    else ->
                        "Calling…"
                }
            )
        )

        if (activeCall == null) {
            cancelRetry()

            if (outgoingCall != null) {
                ensureRingTimeout(outgoingCall)
            } else {
                cancelRingTimeout()
            }
            return
        }

        cancelRingTimeout()

        if (LiveCallAudio.isRunning()) {
            StateStore.clearReconnectWindow(this)
            StateStore.setPeerConnection(
                this,
                PeerConnectionState.CONNECTED
            )
            cancelRetry()
            return
        }

        ensureReconnectWindow()

        if (
            StateStore.callInitiator(this) &&
            !LiveCallAudio.isStarting()
        ) {
            attemptOutgoingConnection(activeCall)
        } else {
            scheduleDeadlineCheck(activeCall)
        }
    }

    private fun ensureReconnectWindow() {
        if (StateStore.reconnectUntil(this) <= 0L) {
            StateStore.beginReconnectWindow(this)
        }
    }

    private fun attemptOutgoingConnection(
        callId: String
    ) {
        if (reconnectExpired()) {
            failDisconnectedCall(callId)
            return
        }

        StateStore.setStatus(
            this,
            "Reconnecting live audio…"
        )
        StateStore.setPeerConnection(
            this,
            PeerConnectionState.RECONNECTING
        )
        StateStore.setPeerRoute(
            this,
            PeerRoute.RECONNECTING
        )
        EventBus.notifyStateChanged(this)

        LiveCallAudio.startOutgoing(
            this,
            callId
        ) { connected ->
            handler.post {
                if (
                    StateStore.activeCall(this) !=
                        callId
                ) {
                    cancelRetry()
                    return@post
                }

                if (connected) {
                    StateStore.clearReconnectWindow(this)
                    StateStore.setPeerConnection(
                        this,
                        PeerConnectionState.CONNECTED
                    )
                    StateStore.setStatus(
                        this,
                        "Live call"
                    )
                    EventBus.notifyStateChanged(this)
                    cancelRetry()
                } else if (reconnectExpired()) {
                    failDisconnectedCall(callId)
                } else {
                    scheduleRetry(callId)
                }
            }
        }
    }

    private fun scheduleRetry(callId: String) {
        cancelRetry()

        retryRunnable =
            Runnable {
                retryRunnable = null

                if (
                    StateStore.activeCall(this) ==
                        callId
                ) {
                    updateFromState()
                }
            }.also {
                handler.postDelayed(
                    it,
                    Protocol.RECONNECT_RETRY_MS
                )
            }
    }

    private fun scheduleDeadlineCheck(
        callId: String
    ) {
        cancelRetry()

        val remaining =
            StateStore.reconnectUntil(this) -
                System.currentTimeMillis()

        if (remaining <= 0L) {
            failDisconnectedCall(callId)
            return
        }

        retryRunnable =
            Runnable {
                retryRunnable = null

                if (
                    StateStore.activeCall(this) !=
                        callId
                ) {
                    return@Runnable
                }

                if (LiveCallAudio.isRunning()) {
                    StateStore.clearReconnectWindow(this)
                    return@Runnable
                }

                if (reconnectExpired()) {
                    failDisconnectedCall(callId)
                } else {
                    updateFromState()
                }
            }.also {
                handler.postDelayed(
                    it,
                    minOf(
                        Protocol.RECONNECT_RETRY_MS,
                        remaining
                    )
                )
            }
    }

    private fun reconnectExpired(): Boolean {
        val deadline =
            StateStore.reconnectUntil(this)

        return deadline > 0L &&
            System.currentTimeMillis() >= deadline
    }

    private fun failDisconnectedCall(
        callId: String
    ) {
        cancelRetry()
        LiveCallAudio.stop(this)

        StateStore.clearCallState(this)
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
            "Call disconnected · TALK recommended"
        )
        EventBus.notifyStateChanged(this)

        DataLayerTransport(this)
            .sendSignal(
                Protocol.CALL_END,
                callId
            ) { }

        stopSelf()
    }

    private fun ensureRingTimeout(callId: String) {
        if (ringTimeoutRunnable != null) return

        ringTimeoutRunnable =
            Runnable {
                ringTimeoutRunnable = null

                if (
                    StateStore.outgoingCall(this) != callId ||
                    StateStore.activeCall(this) != null
                ) {
                    return@Runnable
                }

                StateStore.clearCallState(this)
                StateStore.setStatus(
                    this,
                    "No answer · TALK recommended"
                )
                EventBus.notifyStateChanged(this)

                DataLayerTransport(this)
                    .sendSignal(
                        Protocol.CALL_CANCEL,
                        callId
                    ) { }

                stopSelf()
            }.also {
                handler.postDelayed(
                    it,
                    Protocol.CALL_TIMEOUT_MS
                )
            }
    }

    private fun cancelRingTimeout() {
        ringTimeoutRunnable?.let(handler::removeCallbacks)
        ringTimeoutRunnable = null
    }

    private fun cancelRetry() {
        retryRunnable?.let(handler::removeCallbacks)
        retryRunnable = null
    }

    private fun registerStateReceiver() {
        if (receiverRegistered) return

        val filter =
            IntentFilter(
                Protocol.ACTION_STATE_CHANGED
            )

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                stateReceiver,
                filter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(
                stateReceiver,
                filter
            )
        }

        receiverRegistered = true
    }

    private fun ensureChannel() {
        val manager =
            getSystemService(
                NotificationManager::class.java
            ) ?: return

        if (
            manager.getNotificationChannel(
                CHANNEL_ID
            ) == null
        ) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "HappyTalky live calls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description =
                        "Keeps an active HappyTalky call connected"
                    setSound(null, null)
                }
            )
        }
    }

    private fun notification(text: String): Notification {
        val openIntent =
            packageManager
                .getLaunchIntentForPackage(
                    packageName
                )
                ?.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                ?: Intent(Intent.ACTION_MAIN)
                    .setPackage(packageName)

        val pending =
            PendingIntent.getActivity(
                this,
                11,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val hangUpIntent =
            Intent(
                this,
                CallActionReceiver::class.java
            ).setAction(
                CallActionReceiver.ACTION_HANG_UP
            )

        val hangUpPending =
            PendingIntent.getBroadcast(
                this,
                12,
                hangUpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        return Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.sym_call_incoming
            )
            .setContentTitle("HappyTalky")
            .setContentText(text)
            .setContentIntent(pending)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.sym_call_missed,
                    if (
                        StateStore.outgoingCall(this) != null &&
                        StateStore.activeCall(this) == null
                    ) {
                        "Cancel"
                    } else {
                        "End"
                    },
                    hangUpPending
                ).build()
            )
            .setOngoing(true)
            .setCategory(
                Notification.CATEGORY_CALL
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID =
            "happytalky_live_call_v1"
        private const val NOTIFICATION_ID = 1201

        fun start(context: Context) {
            context.startForegroundService(
                Intent(
                    context,
                    LiveCallService::class.java
                )
            )
        }

        fun stop(context: Context) {
            context.stopService(
                Intent(
                    context,
                    LiveCallService::class.java
                )
            )
        }
    }
}
