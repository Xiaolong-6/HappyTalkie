package com.xiaolong.happytalkie.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.xiaolong.happytalkie.core.CallVisualState
import com.xiaolong.happytalkie.core.HappyTalkieUiState
import com.xiaolong.happytalkie.core.PeerConnectionState
import com.xiaolong.happytalkie.core.PeerRoute

private const val PHONE_BACKGROUND = 0xFF050A16

@PreviewTest
@Preview(
    name = "Phone ready",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneReadyScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Ready",
                callState = CallVisualState.READY,
                callEnabled = true,
                peerName = "Watch",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
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
    name = "Phone calling",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneCallingScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Calling Watch…",
                callState = CallVisualState.OUTGOING,
                callEnabled = true,
                talkEnabled = false,
                peerName = "Watch",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
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
    name = "Phone recording",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneRecordingScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Recording TALK…",
                callState = CallVisualState.READY,
                recording = true,
                callEnabled = true,
                talkEnabled = true,
                peerName = "Watch",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
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
    name = "Phone offline",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneOfflineScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Watch offline · TALK recommended",
                callState = CallVisualState.READY,
                callEnabled = false,
                talkEnabled = true,
                peerName = "Watch",
                peerConnection = PeerConnectionState.DISCONNECTED,
                peerRoute = PeerRoute.OFFLINE,
            ),
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
    name = "Phone live",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneLiveScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Live with Watch",
                callState = CallVisualState.LIVE,
                callEnabled = true,
                talkEnabled = false,
                speakerOn = true,
                peerName = "Watch",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
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
    name = "Phone incoming",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneIncomingScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Watch is calling",
                callState = CallVisualState.INCOMING,
                callEnabled = true,
                talkEnabled = false,
                peerName = "Watch",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
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
    name = "Phone reconnecting",
    widthDp = 412,
    heightDp = 915,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneReconnectingScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Reconnecting…",
                callState = CallVisualState.RECONNECTING,
                callEnabled = true,
                talkEnabled = false,
                peerName = "Watch",
                peerConnection = PeerConnectionState.RECONNECTING,
                peerRoute = PeerRoute.RECONNECTING,
            ),
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
    name = "Phone compact 360x800",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    backgroundColor = PHONE_BACKGROUND,
)
@Composable
fun PhoneCompactScreenshot() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(
                status = "Ready",
                callState = CallVisualState.READY,
                callEnabled = true,
                talkEnabled = true,
                peerName = "Watch",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
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
