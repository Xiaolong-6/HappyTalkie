package com.xiaolong.happytalkie.core

import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {
    @Test
    fun allDataLayerPathsStayInsideHappyTalkieNamespace() {
        assertTrue(Protocol.CALL_RING.startsWith(Protocol.BASE))
        assertTrue(Protocol.CALL_ANSWER.startsWith(Protocol.BASE))
        assertTrue(Protocol.CALL_DECLINE.startsWith(Protocol.BASE))
        assertTrue(Protocol.CALL_CANCEL.startsWith(Protocol.BASE))
        assertTrue(Protocol.CALL_BUSY.startsWith(Protocol.BASE))
        assertTrue(Protocol.CALL_END.startsWith(Protocol.BASE))
        assertTrue(Protocol.CAPABILITY_PHONE.isNotBlank())
        assertTrue(Protocol.CAPABILITY_WATCH.isNotBlank())
        assertTrue(Protocol.VOICE_PREFIX.startsWith(Protocol.BASE))
    }
}
