package com.xiaolong.happytalkie.core

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
import android.os.IBinder

class LiveCallService : Service() {
    private var receiverRegistered = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateFromState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, notification("Preparing call…"))
        registerStateReceiver()
        updateFromState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateFromState()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
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

        if (activeCall == null && outgoingCall == null) {
            stopSelf()
            return
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(
            NOTIFICATION_ID,
            notification(
                when {
                    activeCall != null && LiveCallAudio.isRunning() -> "Live call active"
                    activeCall != null -> "Connecting live audio…"
                    else -> "Calling…"
                }
            )
        )

        if (
            activeCall != null &&
            StateStore.callInitiator(this) &&
            !LiveCallAudio.isRunning() &&
            !LiveCallAudio.isStarting()
        ) {
            LiveCallAudio.startOutgoing(this, activeCall) { connected ->
                if (!connected && StateStore.activeCall(this) == activeCall) {
                    StateStore.clearCallState(this)
                    StateStore.setStatus(this, "Could not open live audio")
                    EventBus.notifyStateChanged(this)
                    stopSelf()
                }
            }
        }
    }

    private fun registerStateReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(Protocol.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(stateReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "HappyTalkie live calls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps an active HappyTalkie call connected"
                    setSound(null, null)
                }
            )
        }
    }

    private fun notification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            this,
            11,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("HappyTalkie")
            .setContentText(text)
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_CALL)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "happytalkie_live_call_v1"
        private const val NOTIFICATION_ID = 1201

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, LiveCallService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveCallService::class.java))
        }
    }
}
