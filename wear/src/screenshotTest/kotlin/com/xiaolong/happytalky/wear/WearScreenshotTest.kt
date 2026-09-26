package com.xiaolong.happytalky.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.MaterialTheme
import com.android.tools.screenshot.PreviewTest
import com.xiaolong.happytalky.core.CallDirection
import com.xiaolong.happytalky.core.CallHistoryEntry
import com.xiaolong.happytalky.core.CallOutcome
import com.xiaolong.happytalky.core.CallVisualState
import com.xiaolong.happytalky.core.HappyTalkyUiState
import com.xiaolong.happytalky.core.PeerConnectionState
import com.xiaolong.happytalky.core.PeerRoute
import com.xiaolong.happytalky.core.VoiceDirection
import com.xiaolong.happytalky.core.VoiceMessage
import java.io.File

private const val WATCH_DEVICE =
    "spec:width=192dp,height=192dp,dpi=320,isRound=true"
private const val WATCH_BACKGROUND = 0xFF000000

private val watchCallHistory =
    listOf(
        CallHistoryEntry(
            id = "call-1",
            callId = "call-id-1",
            occurredAt = 1_760_000_180_000L,
            direction = CallDirection.INCOMING,
            outcome = CallOutcome.DECLINED_BY_ME,
        ),
        CallHistoryEntry(
            id = "call-2",
            callId = "call-id-2",
            occurredAt = 1_760_000_240_000L,
            direction = CallDirection.OUTGOING,
            outcome = CallOutcome.COMPLETED,
            durationMs = 74_000L,
        ),
    )

private val watchMessages =
    listOf(
        VoiceMessage(
            id = "watch-in-1",
            createdAt = 1_760_000_000_000L,
            direction = VoiceDirection.INCOMING,
            file = File("/tmp/watch-in-1.m4a"),
            durationMs = 8_000L,
        ),
        VoiceMessage(
            id = "watch-out-1",
            createdAt = 1_760_000_060_000L,
            direction = VoiceDirection.OUTGOING,
            file = File("/tmp/watch-out-1.m4a"),
            durationMs = 5_000L,
        ),
    )

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
            state = HappyTalkyUiState(
                status = "Ready",
                callState = CallVisualState.READY,
                callEnabled = true,
                peerName = "Phone",
                peerConnection = PeerConnectionState.CONNECTED,
                peerRoute = PeerRoute.NEARBY_DIRECT,
                messages = watchMessages,
                unreadVoiceCount = 1,
                callHistory = watchCallHistory,
            ),
            onCall = {},
            onDecline = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
            onPlay = {},
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
            state = HappyTalkyUiState(
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
            onPlay = {},
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
            state = HappyTalkyUiState(
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
            onPlay = {},
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
            state = HappyTalkyUiState(
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
            onPlay = {},
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
            state = HappyTalkyUiState(
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
            onPlay = {},
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
            state = HappyTalkyUiState(
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
            onPlay = {},
        )
    }
}


@PreviewTest
@Preview(
    name = "Watch TALK inbox",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchTalkInboxScreenshot() {
    MaterialTheme {
        WearTalkInbox(
            messages = watchMessages,
            callHistory = watchCallHistory,
            unreadCount = 1,
            peerName = "Phone",
            onBack = {},
            onPlay = {},
        )
    }
}


@PreviewTest
@Preview(
    name = "Watch CALL history",
    device = WATCH_DEVICE,
    showBackground = true,
    backgroundColor = WATCH_BACKGROUND,
)
@Composable
fun WatchCallHistoryScreenshot() {
    MaterialTheme {
        WearTalkInbox(
            messages = emptyList(),
            callHistory = watchCallHistory,
            unreadCount = 0,
            peerName = "Phone",
            onBack = {},
            onPlay = {},
        )
    }
}
