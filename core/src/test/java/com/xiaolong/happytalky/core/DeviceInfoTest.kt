package com.xiaolong.happytalky.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceInfoTest {
    @Test
    fun watchLabelIncludesModel() {
        val info =
            DeviceInfo(
                deviceId = "watch-1",
                role = EndpointRole.WATCH,
                manufacturer = "Google",
                model = "Pixel Watch 3",
                appVersion = "0.3.0",
                protocolVersion = 1,
                capabilities =
                    setOf(
                        Protocol.CAPABILITY_TALK_V1,
                        Protocol.CAPABILITY_CALL_V1
                    )
            )

        assertEquals(
            "Watch · Pixel Watch 3",
            info.displayLabel()
        )
    }

    @Test
    fun genericRoleNameIsNotRepeated() {
        val info =
            DeviceInfo(
                deviceId = "phone-1",
                role = EndpointRole.PHONE,
                manufacturer = "",
                model = "Phone",
                appVersion = "0.3.0",
                protocolVersion = 1,
                capabilities = emptySet()
            )

        assertEquals(
            "Phone",
            info.displayLabel()
        )
    }
}
