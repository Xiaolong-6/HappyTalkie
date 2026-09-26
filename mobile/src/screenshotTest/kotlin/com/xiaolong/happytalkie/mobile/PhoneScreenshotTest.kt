package com.xiaolong.happytalkie.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.xiaolong.happytalkie.core.CallVisualState
import com.xiaolong.happytalkie.core.HappyTalkieUiState

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
                peerName = "Watch",
            ),
            onCall = {},
            onSpeakerToggle = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
            onPlay = {},
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
                callEnabled = false,
                talkEnabled = false,
                peerName = "Watch",
            ),
            onCall = {},
            onSpeakerToggle = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
            onPlay = {},
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
                talkEnabled = true,
                peerName = "Watch",
            ),
            onCall = {},
            onSpeakerToggle = {},
            onTalkStart = {},
            onTalkFinish = {},
            onTalkCancel = {},
            onPlay = {},
        )
    }
}
