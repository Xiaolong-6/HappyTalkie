package com.xiaolong.happytalkie.wear

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.xiaolong.happytalkie.core.CallVisualState
import com.xiaolong.happytalkie.core.HappyTalkieActivity
import com.xiaolong.happytalkie.core.HappyTalkieUiState
import com.xiaolong.happytalkie.core.PeerConnectionState
import com.xiaolong.happytalkie.core.PeerRoute
import com.xiaolong.happytalkie.core.VoiceDirection
import com.xiaolong.happytalkie.core.VoiceMessage

class MainActivity : HappyTalkieActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WearHome(
                    state = uiState,
                    onCall = ::handleCallAction,
                    onDecline = ::declineIncomingCall,
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
fun WearHome(
    state: HappyTalkieUiState,
    onCall: () -> Unit,
    onDecline: () -> Unit,
    onTalkStart: () -> Unit,
    onTalkFinish: () -> Unit,
    onTalkCancel: () -> Unit,
    onPlay: (VoiceMessage) -> Unit,
) {
    AppScaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 13.dp,
                        end = 13.dp,
                        top = 24.dp,
                        bottom = 58.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                RouteStatus(state)

                CallControl(
                    state = state,
                    onCall = onCall,
                    onDecline = onDecline,
                )

                state.messages.firstOrNull()?.let { latest ->
                    LatestTalk(
                        message = latest,
                        peerName = state.peerName,
                        onPlay = onPlay,
                    )
                }
            }

            TalkEdgeButton(
                state = state,
                onStart = onTalkStart,
                onFinish = onTalkFinish,
                onCancel = onTalkCancel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RouteStatus(state: HappyTalkieUiState) {
    val routeColor =
        when (state.peerRoute) {
            PeerRoute.NEARBY_BLUETOOTH ->
                Color(0xFF8CC0FF)
            PeerRoute.REMOTE_WIFI ->
                Color(0xFF71D7FF)
            PeerRoute.REMOTE_CELLULAR ->
                Color(0xFF8EE0A8)
            PeerRoute.REMOTE_INTERNET ->
                Color(0xFFC0CCE0)
            PeerRoute.RECONNECTING ->
                Color(0xFFFFD35A)
            PeerRoute.OFFLINE ->
                Color(0xFFFF7D8D)
            PeerRoute.UNKNOWN ->
                Color(0xFF8E9AAF)
        }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = wearRouteIcon(state.peerRoute),
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = routeColor,
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = wearRouteLabel(state),
            style = MaterialTheme.typography.labelSmall,
            color = routeColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CallControl(
    state: HappyTalkieUiState,
    onCall: () -> Unit,
    onDecline: () -> Unit,
) {
    val container =
        when (state.callState) {
            CallVisualState.LIVE,
            CallVisualState.OUTGOING,
            CallVisualState.CONNECTING,
            CallVisualState.RECONNECTING ->
                Color(0xFFD9485E)

            CallVisualState.INCOMING ->
                Color(0xFF1DAA6B)

            CallVisualState.READY ->
                Color(0xFF176DFF)
        }

    Button(
        onClick = onCall,
        enabled = state.callEnabled,
        label = {
            Text(
                text = wearCallLabel(state),
                fontWeight = FontWeight.SemiBold,
            )
        },
        secondaryLabel = {
            Text(
                text = wearCallSecondary(state),
                maxLines = 1,
            )
        },
        icon = {
            Icon(
                imageVector =
                    when (state.callState) {
                        CallVisualState.LIVE,
                        CallVisualState.OUTGOING,
                        CallVisualState.CONNECTING,
                        CallVisualState.RECONNECTING ->
                            Icons.Rounded.CallEnd

                        CallVisualState.INCOMING,
                        CallVisualState.READY ->
                            Icons.Rounded.Call
                    },
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color.White,
            secondaryContentColor = Color.White.copy(alpha = 0.76f),
            iconColor = Color.White,
            disabledContainerColor = Color(0xFF1C293C),
            disabledContentColor = Color(0xFF7D8A9F),
            disabledSecondaryContentColor = Color(0xFF667387),
            disabledIconColor = Color(0xFF7D8A9F),
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.callState == CallVisualState.INCOMING) {
        TextButton(
            onClick = onDecline,
            modifier = Modifier.height(30.dp),
        ) {
            Text(
                "Decline",
                color = Color(0xFFFF8D9A),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun LatestTalk(
    message: VoiceMessage,
    peerName: String,
    onPlay: (VoiceMessage) -> Unit,
) {
    Card(
        onClick = { onPlay(message) },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF111C2D),
            contentColor = Color.White,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play latest TALK",
                modifier = Modifier.size(17.dp),
                tint = Color(0xFF70C8FF),
            )
            Spacer(Modifier.size(6.dp))
            Column {
                Text(
                    text =
                        if (
                            message.direction ==
                                VoiceDirection.OUTGOING
                        ) {
                            "My TALK"
                        } else {
                            "$peerName TALK"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    text = "${message.displayTime()} · tap to play",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF95A5BA),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TalkEdgeButton(
    state: HappyTalkieUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording = state.recording

    EdgeButton(
        onClick = {},
        enabled = state.talkEnabled,
        modifier = modifier
            .semantics {
                role = Role.Button
                contentDescription =
                    if (recording) {
                        "Release to send TALK"
                    } else {
                        "Hold to record TALK"
                    }
            }
            .pointerInput(state.talkEnabled) {
                detectTapGestures(
                    onPress = {
                        if (state.talkEnabled) {
                            onStart()
                            val released = tryAwaitRelease()
                            if (released) {
                                onFinish()
                            } else {
                                onCancel()
                            }
                        }
                    }
                )
            },
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (recording) {
                    Color(0xFFE4475E)
                } else {
                    Color(0xFF0F89FF)
                },
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF182638),
            disabledContentColor = Color(0xFF75849A),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector =
                    if (recording) {
                        Icons.Rounded.Stop
                    } else {
                        Icons.Rounded.Mic
                    },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(5.dp))
            Text(
                text =
                    when {
                        !state.talkEnabled ->
                            "TALK after call"
                        recording ->
                            "Release to send"
                        else ->
                            "Hold to TALK"
                    },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun wearRouteIcon(route: PeerRoute): ImageVector =
    when (route) {
        PeerRoute.NEARBY_BLUETOOTH ->
            Icons.Rounded.Bluetooth
        PeerRoute.REMOTE_WIFI ->
            Icons.Rounded.Wifi
        PeerRoute.REMOTE_CELLULAR ->
            Icons.Rounded.NetworkCell
        PeerRoute.REMOTE_INTERNET ->
            Icons.Rounded.Cloud
        PeerRoute.RECONNECTING ->
            Icons.Rounded.Cloud
        PeerRoute.OFFLINE ->
            Icons.Rounded.CloudOff
        PeerRoute.UNKNOWN ->
            Icons.Rounded.Cloud
    }

private fun wearRouteLabel(state: HappyTalkieUiState): String =
    when {
        state.callState == CallVisualState.RECONNECTING ->
            "Reconnecting…"

        state.peerConnection ==
            PeerConnectionState.DISCONNECTED ->
            "Offline · TALK ready"

        state.peerRoute ==
            PeerRoute.NEARBY_BLUETOOTH ->
            "Bluetooth · CALL ready"

        state.peerRoute ==
            PeerRoute.REMOTE_WIFI ->
            "Remote Wi‑Fi · CALL"

        state.peerRoute ==
            PeerRoute.REMOTE_CELLULAR ->
            "Remote cellular · CALL"

        state.peerRoute ==
            PeerRoute.REMOTE_INTERNET ->
            "Remote · CALL"

        else ->
            "Checking connection"
    }

private fun wearCallLabel(state: HappyTalkieUiState): String =
    when (state.callState) {
        CallVisualState.LIVE -> "End call"
        CallVisualState.INCOMING -> "Answer phone"
        CallVisualState.OUTGOING -> "Cancel call"
        CallVisualState.CONNECTING -> "End call"
        CallVisualState.RECONNECTING -> "End call"
        CallVisualState.READY ->
            if (state.callEnabled) {
                "Call phone"
            } else {
                "CALL unavailable"
            }
    }

private fun wearCallSecondary(state: HappyTalkieUiState): String =
    when (state.callState) {
        CallVisualState.LIVE -> "Live audio"
        CallVisualState.INCOMING -> "You choose whether to answer"
        CallVisualState.OUTGOING -> "Waiting for answer"
        CallVisualState.CONNECTING -> "Opening audio"
        CallVisualState.RECONNECTING -> "Trying a new route"
        CallVisualState.READY ->
            if (state.callEnabled) {
                "Live conversation"
            } else {
                "Use TALK instead"
            }
    }
