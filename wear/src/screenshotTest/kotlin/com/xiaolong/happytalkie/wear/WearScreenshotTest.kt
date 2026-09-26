package com.xiaolong.happytalkie.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.MaterialTheme
import com.android.tools.screenshot.PreviewTest
import com.xiaolong.happytalkie.core.CallVisualState
import com.xiaolong.happytalkie.core.HappyTalkieUiState
import com.xiaolong.happytalkie.core.PeerConnectionState
import com.xiaolong.happytalkie.core.PeerRoute

private const val WATCH_DEVICE =
    "spec:width=192dp,height=192dp,dpi=320,isRound=true"
private const val WATCH_BACKGROUND = 0xFF000000

@PreviewTest
@Preview(
    name = "Watch ready",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchReadyScreenshot() {
    MaterialTheme {
        WearHome(
            state = HappyTalkieUiState(
                status = "Ready",
                callState = CallVisualState.READY,
                callEnabled = true,
                peerName = "Phone",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
            onCall = {},
            onDecline = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Watch incoming",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchIncomingScreenshot() {
    MaterialTheme {
        WearHome(
            state = HappyTalkieUiState(
                status = "Phone is calling",
                callState = CallVisualState.INCOMING,
                peerName = "Phone",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
            onCall = {},
            onDecline = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Watch recording",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchRecordingScreenshot() {
    MaterialTheme {
        WearHome(
            state = HappyTalkieUiState(
                status = "Recording TALK…",
                callState = CallVisualState.READY,
                recording = true,
                callEnabled = true,
                peerName = "Phone",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
            onCall = {},
            onDecline = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
        )
    }
}


@PreviewTest
@Preview(
    name = "Watch offline",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchOfflineScreenshot() {
    MaterialTheme {
        WearHome(
            state = HappyTalkieUiState(
                status = "Phone offline · TALK recommended",
                callState = CallVisualState.READY,
                callEnabled = false,
                talkEnabled = true,
                peerName = "Phone",
                peerConnection = PeerConnectionState.DISCONNECTED,
                peerRoute = PeerRoute.OFFLINE,
            ),
            onCall = {},
            onDecline = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Watch live",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchLiveScreenshot() {
    MaterialTheme {
        WearHome(
            state = HappyTalkieUiState(
                status = "Live with Phone",
                callState = CallVisualState.LIVE,
                callEnabled = true,
                talkEnabled = false,
                peerName = "Phone",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
            ),
            onCall = {},
            onDecline = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
        )
    }
}


@PreviewTest
@Preview(
    name = "Watch reconnecting",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchReconnectingScreenshot() {
    MaterialTheme {
        WearHome(
            state = HappyTalkieUiState(
                status = "Reconnecting…",
                callState = CallVisualState.RECONNECTING,
                callEnabled = true,
                talkEnabled = false,
                peerName = "Phone",
                peerConnection = PeerConnectionState.RECONNECTING,
                peerRoute = PeerRoute.RECONNECTING,
            ),
            onCall = {},
            onDecline = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
        )
    }
}
