package com.xiaolong.happytalkie.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.MaterialTheme
import com.android.tools.screenshot.PreviewTest
import com.xiaolong.happytalkie.core.CallVisualState
import com.xiaolong.happytalkie.core.HappyTalkieUiState

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
                peerName = "Phone",
            ),
            onCall = {},
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
            state = HappyTalkieUiState(
                status = "Phone is calling",
                callState = CallVisualState.INCOMING,
                peerName = "Phone",
            ),
            onCall = {},
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
            state = HappyTalkieUiState(
                status = "Recording TALK…",
                callState = CallVisualState.READY,
                recording = true,
                peerName = "Phone",
            ),
            onCall = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
            onPlay = {},
        )
    }
}
