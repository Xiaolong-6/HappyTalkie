package com.xiaolong.happytalkie.wear

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.xiaolong.happytalkie.core.CallVisualState
import com.xiaolong.happytalkie.core.HappyTalkieActivity
import com.xiaolong.happytalkie.core.HappyTalkieUiState
import com.xiaolong.happytalkie.core.VoiceMessage

class MainActivity : HappyTalkieActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WearHome(
                    state = uiState,
                    onCall = ::handleCallAction,
                    onTalkStart = ::beginTalk,
                    onTalkFinish = ::finishTalk,
                    onTalkCancel = ::cancelTalk,
                    onPlay = ::playMessage,
                )
            }
        }
    }
}

@Composable
private fun WearHome(
    state: HappyTalkieUiState,
    onCall: () -> Unit,
    onTalkStart: () -> Unit,
    onTalkFinish: () -> Unit,
    onTalkCancel: () -> Unit,
    onPlay: (VoiceMessage) -> Unit,
) {
    val scrollState = rememberScrollState()

    AppScaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
    ) {
        ScreenScaffold(
            scrollState = scrollState,
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 10.dp,
                top = 24.dp,
                bottom = 12.dp,
            ),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StatusLine(state)

                CallButton(
                    state = state,
                    onCall = onCall,
                )

                TalkButton(
                    state = state,
                    onStart = onTalkStart,
                    onFinish = onTalkFinish,
                    onCancel = onTalkCancel,
                )

                RecentMessages(
                    messages = state.messages.take(3),
                    peerName = state.peerName,
                    onPlay = onPlay,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(state: HappyTalkieUiState) {
    val dot = when (state.callState) {
        CallVisualState.LIVE, CallVisualState.INCOMING -> Color(0xFF6FE7A5)
        CallVisualState.OUTGOING, CallVisualState.CONNECTING -> Color(0xFFFFD35A)
        CallVisualState.READY ->
            if (state.status.contains("offline", true)) Color(0xFFFF6B79)
            else Color(0xFF6FE7A5)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dot, CircleShape)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = when (state.callState) {
                CallVisualState.LIVE -> "Live with ${state.peerName}"
                CallVisualState.INCOMING -> "${state.peerName} calling"
                CallVisualState.OUTGOING -> "Calling ${state.peerName}"
                CallVisualState.CONNECTING -> "Connecting"
                CallVisualState.READY ->
                    if (state.status.contains("offline", true)) "Phone offline"
                    else "Ready"
            },
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFCBD6E7),
            maxLines = 1,
        )
    }
}

@Composable
private fun CallButton(
    state: HappyTalkieUiState,
    onCall: () -> Unit,
) {
    val live = state.callState == CallVisualState.LIVE
    val incoming = state.callState == CallVisualState.INCOMING
    val container = when {
        live -> Color(0xFFD94359)
        incoming -> Color(0xFF1CA66A)
        state.callState == CallVisualState.READY -> Color(0xFF176DFF)
        else -> Color(0xFF344B70)
    }

    Button(
        onClick = onCall,
        enabled = state.callEnabled,
        label = {
            Text(
                when (state.callState) {
                    CallVisualState.LIVE -> "End call"
                    CallVisualState.INCOMING -> "Answer"
                    CallVisualState.OUTGOING -> "Calling…"
                    CallVisualState.CONNECTING -> "Connecting…"
                    CallVisualState.READY -> "Call phone"
                }
            )
        },
        secondaryLabel = {
            Text(
                when (state.callState) {
                    CallVisualState.LIVE -> "Live audio"
                    CallVisualState.INCOMING -> "Tap to talk live"
                    CallVisualState.OUTGOING -> "Waiting for answer"
                    CallVisualState.CONNECTING -> "Opening audio"
                    CallVisualState.READY -> "Live conversation"
                }
            )
        },
        icon = {
            Icon(
                imageVector = if (live) Icons.Rounded.Stop else Icons.Rounded.Call,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color.White,
            secondaryContentColor = Color.White.copy(alpha = 0.74f),
            iconColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TalkButton(
    state: HappyTalkieUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val recording = state.recording

    Button(
        onClick = {},
        enabled = state.talkEnabled,
        label = {
            Text(
                when {
                    !state.talkEnabled -> "TALK after call"
                    recording -> "Release to send"
                    else -> "Hold to TALK"
                }
            )
        },
        secondaryLabel = {
            Text(
                when {
                    !state.talkEnabled -> "Finish live audio first"
                    recording -> "Recording…"
                    else -> "Voice message"
                }
            )
        },
        icon = {
            Icon(
                imageVector = if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (recording) Color(0xFFE4475E) else Color(0xFF0F89FF),
            contentColor = Color.White,
            secondaryContentColor = Color.White.copy(alpha = 0.78f),
            iconColor = Color.White,
            disabledContainerColor = Color(0xFF1D293C),
            disabledContentColor = Color(0xFF7C899D),
            disabledSecondaryContentColor = Color(0xFF657185),
            disabledIconColor = Color(0xFF7C899D),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription =
                    if (recording) "Release to send TALK" else "Hold to record TALK"
            }
            .pointerInput(state.talkEnabled) {
                detectTapGestures(
                    onPress = {
                        if (state.talkEnabled) {
                            onStart()
                            val released = tryAwaitRelease()
                            if (released) onFinish() else onCancel()
                        }
                    }
                )
            },
    )
}

@Composable
private fun RecentMessages(
    messages: List<VoiceMessage>,
    peerName: String,
    onPlay: (VoiceMessage) -> Unit,
) {
    if (messages.isEmpty()) {
        Text(
            text = "No TALK messages",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7F8CA0),
            modifier = Modifier.padding(top = 2.dp),
        )
        return
    }

    Text(
        text = "Recent TALK",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF8D9BB0),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 2.dp),
    )

    messages.forEach { message ->
        Card(
            onClick = { onPlay(message) },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF121D2E),
                contentColor = Color.White,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play TALK",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF70C8FF),
                )
                Spacer(Modifier.size(7.dp))
                Column {
                    Text(
                        text = if (message.direction.name == "OUTGOING") "Me" else peerName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    Text(
                        text = message.displayTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF98A7BC),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
