package com.xiaolong.happytalkie.core

import android.content.Context

object StateStore {
    private const val PREFS = "happytalkie_state"
    private const val KEY_STATUS = "status"
    private const val KEY_INCOMING = "incoming_call"
    private const val KEY_OUTGOING = "outgoing_call"
    private const val KEY_ACTIVE = "active_call"
    private const val KEY_CALL_INITIATOR = "call_initiator"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun status(context: Context): String =
        prefs(context).getString(KEY_STATUS, null) ?: "Ready"

    fun setStatus(context: Context, value: String) {
        prefs(context).edit().putString(KEY_STATUS, value).apply()
    }

    fun incomingCall(context: Context): String? =
        prefs(context).getString(KEY_INCOMING, null)

    fun setIncomingCall(context: Context, callId: String?) {
        writeNullable(context, KEY_INCOMING, callId)
    }

    fun outgoingCall(context: Context): String? =
        prefs(context).getString(KEY_OUTGOING, null)

    fun setOutgoingCall(context: Context, callId: String?) {
        writeNullable(context, KEY_OUTGOING, callId)
    }

    fun activeCall(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE, null)

    fun setActiveCall(context: Context, callId: String?) {
        writeNullable(context, KEY_ACTIVE, callId)
    }

    fun callInitiator(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALL_INITIATOR, false)

    fun setCallInitiator(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALL_INITIATOR, value).apply()
    }

    fun clearCallState(context: Context) {
        prefs(context).edit()
            .remove(KEY_INCOMING)
            .remove(KEY_OUTGOING)
            .remove(KEY_ACTIVE)
            .remove(KEY_CALL_INITIATOR)
            .apply()
    }

    private fun writeNullable(context: Context, key: String, value: String?) {
        val editor = prefs(context).edit()
        if (value == null) editor.remove(key) else editor.putString(key, value)
        editor.apply()
    }
}
