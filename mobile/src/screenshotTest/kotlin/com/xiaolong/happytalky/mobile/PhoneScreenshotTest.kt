package com.xiaolong.happytalky.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.xiaolong.happytalky.core.CallMode
import com.xiaolong.happytalky.core.CallVisualState
import com.xiaolong.happytalky.core.HappyTalkyUiState
import com.xiaolong.happytalky.core.PeerConnectionState
import com.xiaolong.happytalky.core.PeerRoute
import com.xiaolong.happytalky.core.VoiceDirection
import com.xiaolong.happytalky.core.VoiceMessage
import java.io.File

private val sampleMessages =
    listOf(
        VoiceMessage(
            id = "in-1",
            createdAt = 1_760_000_000_000L,
            direction = VoiceDirection.INCOMING,
            file = File("/tmp/in-1.m4a"),
            durationMs = 8_000L,
        ),
        VoiceMessage(
            id = "out-1",
            createdAt = 1_760_000_060_000L,
            direction = VoiceDirection.OUTGOING,
            file = File("/tmp/out-1.m4a"),
            durationMs = 12_000L,
        ),
        VoiceMessage(
            id = "in-2",
            createdAt = 1_760_000_120_000L,
            direction = VoiceDirection.INCOMING,
            file = File("/tmp/in-2.m4a"),
            durationMs = 5_000L,
        ),
    )

private fun readyState(
    messages: List<VoiceMessage> = sampleMessages,
) =
    HappyTalkyUiState(
        status = "Ready",
        callState = CallVisualState.READY,
        callEnabled = true,
        talkEnabled = true,
        peerName = "Watch",
        peerConnection = PeerConnectionState.CONNECTED,
        peerRoute = PeerRoute.NEARBY_DIRECT,
        messages = messages,
    )

@Composable
private fun PhoneShot(
    state: HappyTalkyUiState,
    dark: Boolean,
) {
    HappyTalkyPhoneTheme(
        darkTheme = dark
    ) {
        HappyTalkyPhoneScreen(
            state = state,
            onCall = {},
            onDecline = {},
            onSpeakerToggle = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
            onPlay = {},
            onDelete = {},
            onClear = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Phone light conversation",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneLightConversationScreenshot() {
    PhoneShot(
        state = readyState(),
        dark = false,
    )
}

@PreviewTest
@Preview(
    name = "Phone dark conversation",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneDarkConversationScreenshot() {
    PhoneShot(
        state = readyState(),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone outgoing call",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneCallingScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Calling Watch…",
                callState = CallVisualState.OUTGOING,
                talkEnabled = false,
            ),
        dark = true,
    )
}


@PreviewTest
@Preview(
    name = "Phone priority offer",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhonePriorityOfferScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Ringing Watch…",
                callState = CallVisualState.OUTGOING,
                talkEnabled = false,
                priorityOfferAvailable = true,
                peerPriorityCallsAllowed = true,
            ),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone priority requested",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhonePriorityRequestedScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Priority call requested…",
                callState = CallVisualState.OUTGOING,
                talkEnabled = false,
                priorityOfferAvailable = false,
                peerPriorityCallsAllowed = true,
                callMode = CallMode.PRIORITY,
            ),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone incoming call",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneIncomingScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Watch is calling",
                callState = CallVisualState.INCOMING,
                talkEnabled = false,
            ),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone live speaker",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneLiveScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Live with Watch",
                callState = CallVisualState.LIVE,
                talkEnabled = false,
                speakerOn = true,
            ),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone recording",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneRecordingScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Recording TALK…",
                recording = true,
            ),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone offline",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneOfflineScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Watch offline · TALK recommended",
                callEnabled = false,
                peerConnection = PeerConnectionState.DISCONNECTED,
                peerRoute = PeerRoute.OFFLINE,
            ),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone reconnecting",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
)
@Composable
fun PhoneReconnectingScreenshot() {
    PhoneShot(
        state =
            readyState().copy(
                status = "Reconnecting…",
                callState = CallVisualState.RECONNECTING,
                talkEnabled = false,
                peerConnection = PeerConnectionState.RECONNECTING,
                peerRoute = PeerRoute.RECONNECTING,
            ),
        dark = true,
    )
}

@PreviewTest
@Preview(
    name = "Phone compact 360x800",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
@Composable
fun PhoneCompactScreenshot() {
    PhoneShot(
        state = readyState(),
        dark = false,
    )
}
