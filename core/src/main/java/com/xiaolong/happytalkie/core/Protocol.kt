package com.xiaolong.happytalkie.core

object Protocol {
    const val BASE = "/happytalkie"
    const val CALL_RING = "$BASE/call/ring"
    const val CALL_ANSWER = "$BASE/call/answer"
    const val CALL_DECLINE = "$BASE/call/decline"
    const val CALL_CANCEL = "$BASE/call/cancel"
    const val CALL_BUSY = "$BASE/call/busy"
    const val CALL_END = "$BASE/call/end"
    const val CALL_AUDIO_PREFIX = "$BASE/call/audio/"
    const val VOICE_PREFIX = "$BASE/voice/"

    const val KEY_ID = "id"
    const val KEY_ORIGIN = "origin"
    const val KEY_CREATED_AT = "createdAt"
    const val KEY_AUDIO = "audio"

    const val META_ROLE = "happytalkie.role"
    const val CAPABILITY_PHONE = "happytalkie_phone"
    const val CAPABILITY_WATCH = "happytalkie_watch"
    const val ACTION_STATE_CHANGED = "com.xiaolong.happytalkie.STATE_CHANGED"

    const val CALL_TIMEOUT_MS = 20_000L
    const val RECONNECT_GRACE_MS = 15_000L
    const val RECONNECT_RETRY_MS = 1_500L
    const val MAX_RECORDING_MS = 60_000

    const val AUDIO_SAMPLE_RATE = 16_000
}
