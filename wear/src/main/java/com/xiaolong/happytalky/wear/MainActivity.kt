package com.xiaolong.happytalky.wear

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.xiaolong.happytalky.core.CallDirection
import com.xiaolong.happytalky.core.CallHistoryEntry
import com.xiaolong.happytalky.core.CallOutcome
import com.xiaolong.happytalky.core.CallVisualState
import com.xiaolong.happytalky.core.HappyTalkyActivity
import com.xiaolong.happytalky.core.HappyTalkyUiState
import com.xiaolong.happytalky.core.PeerConnectionState
import com.xiaolong.happytalky.core.PeerRoute
import com.xiaolong.happytalky.core.Protocol
import com.xiaolong.happytalky.core.VoiceDirection
import com.xiaolong.happytalky.core.VoiceMessage
import kotlin.math.roundToInt

class MainActivity : HappyTalkyActivity() {
    private var openInboxRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openInboxRequested =
            intent?.getBooleanExtra(
                Protocol.EXTRA_OPEN_TALK_INBOX,
                false
            ) == true

        requestFullScreenCallAccessOnce()

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
                    onDelete = ::deleteMessages,
                    openInbox = openInboxRequested,
                    onInboxOpened = {
                        openInboxRequested = false
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openInboxRequested =
            intent.getBooleanExtra(
                Protocol.EXTRA_OPEN_TALK_INBOX,
                false
            )
    }

    private fun requestFullScreenCallAccessOnce() {
        if (Build.VERSION.SDK_INT < 34) {
            return
        }

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        if (
            manager?.canUseFullScreenIntent() ==
                true
        ) {
            return
        }

        val prefs =
            getSharedPreferences(
                "happytalky_wear_setup",
                Context.MODE_PRIVATE
            )

        if (
            prefs.getBoolean(
                "asked_full_screen_call",
                false
            )
        ) {
            return
        }

        prefs.edit()
            .putBoolean(
                "asked_full_screen_call",
                true
            )
            .apply()

        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse(
                        "package:$packageName"
                    )
                )
            )
        }
    }
}

@Composable
fun WearHome(
    state: HappyTalkyUiState,
    onCall: () -> Unit,
    onDecline: () -> Unit,
    onTalkStart: () -> Unit,
    onTalkFinish: () -> Unit,
    onTalkCancel: () -> Unit,
    onPlay: (VoiceMessage) -> Unit,
    onDelete: (Set<String>) -> Unit = {},
    openInbox: Boolean = false,
    onInboxOpened: () -> Unit = {},
) {
    var showInbox by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(openInbox) {
        if (openInbox) {
            showInbox = true
            onInboxOpened()
        }
    }

    if (
        state.callState ==
            CallVisualState.INCOMING
    ) {
        WearIncomingCallScreen(
            peerName = state.peerName,
            onAnswer = onCall,
            onDecline = onDecline,
        )
        return
    }

    if (showInbox) {
        WearTalkInbox(
            messages = state.messages,
            callHistory = state.callHistory,
            unreadCount =
                state.unreadVoiceCount,
            peerName = state.peerName,
            onBack = {
                showInbox = false
            },
            onPlay = onPlay,
            onDelete = onDelete,
        )
        return
    }

    WearHomePage(
        state = state,
        onCall = onCall,
        onTalkStart = onTalkStart,
        onTalkFinish = onTalkFinish,
        onTalkCancel = onTalkCancel,
        onOpenInbox = {
            showInbox = true
        },
    )
}

@Composable
private fun WearHomePage(
    state: HappyTalkyUiState,
    onCall: () -> Unit,
    onTalkStart: () -> Unit,
    onTalkFinish: () -> Unit,
    onTalkCancel: () -> Unit,
    onOpenInbox: () -> Unit,
) {
    var horizontalDrag by remember {
        mutableFloatStateOf(0f)
    }
    val thresholdPx =
        with(LocalDensity.current) {
            42.dp.toPx()
        }

    AppScaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(
                        Alignment.TopCenter
                    )
                    .width(154.dp)
                    .padding(top = 18.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                horizontalDrag =
                                    0f
                            },
                            onHorizontalDrag = {
                                    change,
                                    amount ->
                                change.consume()
                                horizontalDrag +=
                                    amount
                            },
                            onDragEnd = {
                                if (
                                    horizontalDrag <
                                        -thresholdPx
                                ) {
                                    onOpenInbox()
                                }
                                horizontalDrag = 0f
                            },
                            onDragCancel = {
                                horizontalDrag = 0f
                            }
                        )
                    },
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.spacedBy(
                        5.dp
                    ),
            ) {
                RouteStatus(state)

                PrimaryCallAction(
                    state = state,
                    onCall = onCall,
                )

                TalkInboxButton(
                    unread =
                        state.unreadVoiceCount,
                    onClick =
                        onOpenInbox,
                )

                state.callHistory
                    .firstOrNull()
                    ?.let {
                        LastCallSummary(it)
                    }
            }

            TalkHoldButton(
                state = state,
                onStart = onTalkStart,
                onFinish = onTalkFinish,
                onCancel = onTalkCancel,
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .width(142.dp)
                    .padding(bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun WearIncomingCallScreen(
    peerName: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    AppScaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 18.dp,
                    vertical = 18.dp,
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "INCOMING CALL",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                    color =
                        Color(0xFF8CC0FF),
                    fontWeight =
                        FontWeight.Bold,
                )
                Spacer(
                    Modifier.height(6.dp)
                )
                Text(
                    text = peerName,
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold,
                    textAlign =
                        TextAlign.Center,
                )
            }

            Icon(
                imageVector =
                    Icons.Rounded.Call,
                contentDescription = null,
                modifier =
                    Modifier.size(42.dp),
                tint =
                    Color(0xFF76DCA5),
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    ),
            ) {
                CallCircleButton(
                    text = "NO",
                    icon =
                        Icons.Rounded
                            .CallEnd,
                    color =
                        Color(0xFFD9485E),
                    onClick = onDecline,
                )

                CallCircleButton(
                    text = "YES",
                    icon =
                        Icons.Rounded.Call,
                    color =
                        Color(0xFF1DAA6B),
                    onClick = onAnswer,
                )
            }
        }
    }
}

@Composable
private fun CallCircleButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier =
                        Modifier.size(19.dp),
                )
                Text(
                    text = text,
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    fontWeight =
                        FontWeight.Bold,
                )
            }
        },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White,
            ),
        modifier =
            Modifier.size(62.dp),
    )
}

@Composable
private fun RouteStatus(state: HappyTalkyUiState) {
    val routeColor =
        when (state.peerRoute) {
            PeerRoute.NEARBY_DIRECT -> Color(0xFF8CC0FF)
            PeerRoute.REMOTE_WIFI -> Color(0xFF71D7FF)
            PeerRoute.REMOTE_CELLULAR -> Color(0xFF8EE0A8)
            PeerRoute.REMOTE_INTERNET -> Color(0xFFC0CCE0)
            PeerRoute.RECONNECTING -> Color(0xFFFFD35A)
            PeerRoute.OFFLINE -> Color(0xFFFF7D8D)
            PeerRoute.UNKNOWN -> Color(0xFF8E9AAF)
        }

    Row(
        modifier = Modifier.width(136.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = wearRouteIcon(state.peerRoute),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = routeColor,
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = wearRouteLabel(state),
            style = MaterialTheme.typography.labelSmall,
            color = routeColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrimaryCallAction(
    state: HappyTalkyUiState,
    onCall: () -> Unit,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = wearCallLabel(state),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF1C293C),
            disabledContentColor = Color(0xFF7D8A9F),
        ),
        modifier = Modifier.size(66.dp),
    )
}

@Composable
private fun IncomingActions(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onDecline,
            label = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "No",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD9485E),
                contentColor = Color.White,
            ),
            modifier = Modifier.size(58.dp),
        )

        Button(
            onClick = onAnswer,
            label = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Yes",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1DAA6B),
                contentColor = Color.White,
            ),
            modifier = Modifier.size(58.dp),
        )
    }
}

@Composable
private fun TalkInboxButton(
    count: Int,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = count > 0,
        modifier = Modifier
            .width(136.dp)
            .height(28.dp),
    ) {
        Text(
            text =
                if (count > 0) {
                    "Inbox · $count"
                } else {
                    "Inbox · 0"
                },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun TalkEdgeButton(
    state: HappyTalkyUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording = state.recording
    val activeContainer =
        if (recording) {
            Color(0xFFE4475E)
        } else {
            Color(0xFF0F89FF)
        }
    val container =
        if (state.talkEnabled) {
            activeContainer
        } else {
            Color(0xFF182638)
        }
    val content =
        if (state.talkEnabled) {
            Color.White
        } else {
            Color(0xFF75849A)
        }

    Box(
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
    ) {
        EdgeButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = container,
                disabledContentColor = content,
            ),
        ) {
            if (recording) {
                Text(
                    text = "SEND",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "TALK",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun WearTalkInbox(
    messages: List<VoiceMessage>,
    peerName: String,
    onBack: () -> Unit,
    onPlay: (VoiceMessage) -> Unit,
) {
    AppScaffold(
        containerColor = Color.Black,
        contentColor = Color.White,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 30.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.width(132.dp),
                ) {
                    Text(
                        "‹ TALK inbox",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (messages.isEmpty()) {
                item {
                    Text(
                        text = "No saved TALK",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8C9AAF),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(
                    items = messages,
                    key = { it.id },
                ) { message ->
                    WearTalkMessage(
                        message = message,
                        peerName = peerName,
                        onPlay = onPlay,
                    )
                }
            }
        }
    }
}

@Composable
private fun WearTalkMessage(
    message: VoiceMessage,
    peerName: String,
    onPlay: (VoiceMessage) -> Unit,
) {
    val sender =
        if (message.direction == VoiceDirection.OUTGOING) {
            "Me"
        } else {
            peerName
        }

    Card(
        onClick = { onPlay(message) },
        modifier = Modifier.width(138.dp),
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
            Spacer(Modifier.size(6.dp))
            Column {
                Text(
                    text = sender,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text =
                        if (message.durationMs > 0L) {
                            "${message.displayDuration()} · ${message.displayTime()}"
                        } else {
                            message.displayTime()
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF98A7BC),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun wearRouteIcon(route: PeerRoute): ImageVector =
    when (route) {
        PeerRoute.NEARBY_DIRECT -> Icons.Rounded.Link
        PeerRoute.REMOTE_WIFI -> Icons.Rounded.Wifi
        PeerRoute.REMOTE_CELLULAR -> Icons.Rounded.NetworkCell
        PeerRoute.REMOTE_INTERNET -> Icons.Rounded.Cloud
        PeerRoute.RECONNECTING -> Icons.Rounded.Cloud
        PeerRoute.OFFLINE -> Icons.Rounded.CloudOff
        PeerRoute.UNKNOWN -> Icons.Rounded.Cloud
    }

private fun wearRouteLabel(state: HappyTalkyUiState): String =
    when {
        state.callState == CallVisualState.RECONNECTING ->
            "Reconnecting"

        state.peerConnection == PeerConnectionState.DISCONNECTED ->
            "Offline · TALK"

        state.peerRoute == PeerRoute.NEARBY_DIRECT ->
            "Nearby · CALL"

        state.peerRoute == PeerRoute.REMOTE_WIFI ->
            "Wi-Fi · CALL"

        state.peerRoute == PeerRoute.REMOTE_CELLULAR ->
            "Cell · TALK"

        state.peerRoute == PeerRoute.REMOTE_INTERNET ->
            "Remote · TALK"

        else ->
            "Checking"
    }

private fun wearCallLabel(state: HappyTalkyUiState): String =
    when (state.callState) {
        CallVisualState.LIVE -> "END"
        CallVisualState.INCOMING -> "ANSWER"
        CallVisualState.OUTGOING -> "CANCEL"
        CallVisualState.CONNECTING -> "END"
        CallVisualState.RECONNECTING -> "END"
        CallVisualState.READY -> "CALL"
    }
