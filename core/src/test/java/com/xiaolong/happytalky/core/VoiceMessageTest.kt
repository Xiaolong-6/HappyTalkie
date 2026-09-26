package com.xiaolong.happytalky.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceMessageTest {
    @Test
    fun durationFormatsLikeVoiceMessageBubble() {
        val message =
            VoiceMessage(
                id = "sample",
                createdAt = 0L,
                direction =
                    VoiceDirection.INCOMING,
                file =
                    File("sample.m4a"),
                durationMs = 65_100L,
            )

        assertEquals(
            "1:05",
            message.displayDuration()
        )
    }

    @Test
    fun subSecondDurationRoundsToOneSecond() {
        val message =
            VoiceMessage(
                id = "sample",
                createdAt = 0L,
                direction =
                    VoiceDirection.OUTGOING,
                file =
                    File("sample.m4a"),
                durationMs = 700L,
            )

        assertEquals(
            "0:01",
            message.displayDuration()
        )
    }
}
