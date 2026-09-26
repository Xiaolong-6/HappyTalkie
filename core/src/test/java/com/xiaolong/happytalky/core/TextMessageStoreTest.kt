package com.xiaolong.happytalky.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextMessageStoreTest {
    @Test
    fun normalizeTrimsAndKeepsEmoji() {
        assertEquals(
            "hello 😊",
            TextMessageStore.normalize(
                "  hello 😊  "
            )
        )
    }

    @Test
    fun normalizeRejectsBlankText() {
        assertNull(
            TextMessageStore.normalize(
                "   "
            )
        )
    }

    @Test
    fun normalizeCapsWireLength() {
        val raw =
            "x".repeat(
                Protocol.MAX_TEXT_LENGTH +
                    25
            )

        assertEquals(
            Protocol.MAX_TEXT_LENGTH,
            TextMessageStore.normalize(
                raw
            )?.length
        )
    }
}
