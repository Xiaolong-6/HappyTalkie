package com.xiaolong.happytalkie.mobile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        enableEdgeToEdge()

        setContent {
            HappyTalkiePhoneTheme {
                HappyTalkiePhoneScreen(
                    state = uiState,
                    onCall = ::handleCallAction,
                    onDecline = ::declineIncomingCall,
                    onSpeakerToggle = ::toggleSpeaker,
                    onTalkStart = ::beginTalk,
                    onTalkFinish = ::finishTalk,
                    onTalkCancel = ::cancelTalk,
                    onPlay = ::playMessage,
                    onDelete = ::deleteMessages,
                    onClear = ::clearMessages,
                )
            }
        }
    }
}

private val PhoneColors = darkColorScheme(
    primary = Color(0xFF5EA8FF),
    onPrimary = Color(0xFF001B3D),
    primaryContainer = Color(0xFF153C74),
    onPrimaryContainer = Color(0xFFD7E6FF),
    secondary = Color(0xFF70D7FF),
    onSecondary = Color(0xFF003548),
    tertiary = Color(0xFFFFD35A),
    background = Color(0xFF050A16),
    onBackground = Color(0xFFF4F7FF),
    surface = Color(0xFF0E172A),
    onSurface = Color(0xFFF4F7FF),
    surfaceVariant = Color(0xFF17243C),
    onSurfaceVariant = Color(0xFFB9C6DA),
    error = Color(0xFFFF6B79),
)

@Composable
fun HappyTalkiePhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhoneColors,
        typography = Typography(),
        content = content,
    )
}

@Composable
fun HappyTalkiePhoneScreen(
    state: HappyTalkieUiState,
    onCall: () -> Unit,
    onDecline: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onTalkStart: () -> Unit,
    onTalkFinish: () -> Unit,
    onTalkCancel: () -> Unit,
    onPlay: (VoiceMessage) -> Unit,
    onDelete: (Set<String>) -> Unit,
    onClear: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF071023),
                            Color(0xFF050A16),
                            Color(0xFF030711),
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(insets)
                    .padding(horizontal = 22.dp, vertical = 18.dp),
            ) {
                AppHeader(state)
                Spacer(Modifier.height(14.dp))
                ConnectionStrip(state)
                Spacer(Modifier.height(18.dp))
                CallRow(
                    state = state,
                    onCall = onCall,
                    onDecline = onDecline,
                    onSpeakerToggle = onSpeakerToggle,
                )
                Spacer(Modifier.height(18.dp))
                TalkHero(
                    state = state,
                    onStart = onTalkStart,
                    onFinish = onTalkFinish,
                    onCancel = onTalkCancel,
                )
                Spacer(Modifier.height(28.dp))
                RecentTalk(
                    messages = state.messages.take(12),
                    peerName = state.peerName,
                    onPlay = onPlay,
                    onDelete = onDelete,
                    onClear = onClear,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AppHeader(state: HappyTalkieUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(17.dp),
            color = Color(0xFF1767F4),
        ) {
            Image(
                painter = painterResource(
                    com.xiaolong.happytalkie.core.R.drawable.ic_happytalkie_brand
                ),
                contentDescription = "HappyTalkie",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = 1.20f, scaleY = 1.20f)
                    .clip(RoundedCornerShape(17.dp)),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HappyTalkie",
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Phone ↔ ${state.peerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        StatusPill(state)
    }
}

@Composable
private fun StatusPill(state: HappyTalkieUiState) {
    val (dot, container) = when (state.callState) {
        CallVisualState.LIVE,
        CallVisualState.INCOMING ->
            Color(0xFF67E6A0) to Color(0xFF123B2D)

        CallVisualState.OUTGOING,
        CallVisualState.CONNECTING,
        CallVisualState.RECONNECTING ->
            Color(0xFFFFD35A) to Color(0xFF423719)

        CallVisualState.READY ->
            when (state.peerConnection) {
                PeerConnectionState.CONNECTED ->
                    Color(0xFF67E6A0) to Color(0xFF13253A)
                PeerConnectionState.RECONNECTING ->
                    Color(0xFFFFD35A) to Color(0xFF423719)
                PeerConnectionState.DISCONNECTED ->
                    Color(0xFFFF6B79) to Color(0xFF451E29)
                PeerConnectionState.UNKNOWN ->
                    Color(0xFF8C9BB0) to Color(0xFF202B3E)
            }
    }

    Surface(
        shape = CircleShape,
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 11.dp,
                vertical = 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dot, CircleShape)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = statusShortLabel(state),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun statusShortLabel(state: HappyTalkieUiState): String =
    when (state.callState) {
        CallVisualState.LIVE -> "Live"
        CallVisualState.INCOMING -> "Incoming"
        CallVisualState.OUTGOING -> "Calling"
        CallVisualState.CONNECTING -> "Connecting"
        CallVisualState.RECONNECTING -> "Reconnecting"
        CallVisualState.READY ->
            when (state.peerConnection) {
                PeerConnectionState.CONNECTED -> "Ready"
                PeerConnectionState.RECONNECTING -> "Reconnecting"
                PeerConnectionState.DISCONNECTED -> "Offline"
                PeerConnectionState.UNKNOWN -> "Checking"
            }
    }

@Composable
private fun ConnectionStrip(state: HappyTalkieUiState) {
    val routeColor =
        when (state.peerRoute) {
            PeerRoute.NEARBY_DIRECT -> Color(0xFF83B9FF)
            PeerRoute.REMOTE_WIFI -> Color(0xFF70D7FF)
            PeerRoute.REMOTE_CELLULAR -> Color(0xFF8DD7A7)
            PeerRoute.REMOTE_INTERNET -> Color(0xFFB8C7DD)
            PeerRoute.RECONNECTING -> Color(0xFFFFD35A)
            PeerRoute.OFFLINE -> Color(0xFFFF8190)
            PeerRoute.UNKNOWN -> Color(0xFF8C9BB0)
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(
            1.dp,
            routeColor.copy(alpha = 0.22f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 11.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = routeIcon(state.peerRoute),
                contentDescription = null,
                tint = routeColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routeLabel(state.peerRoute),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = routeRecommendation(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun routeIcon(route: PeerRoute): ImageVector =
    when (route) {
        PeerRoute.NEARBY_DIRECT -> Icons.Rounded.Link
        PeerRoute.REMOTE_WIFI -> Icons.Rounded.Wifi
        PeerRoute.REMOTE_CELLULAR -> Icons.Rounded.NetworkCell
        PeerRoute.REMOTE_INTERNET -> Icons.Rounded.Cloud
        PeerRoute.RECONNECTING -> Icons.Rounded.Cloud
        PeerRoute.OFFLINE -> Icons.Rounded.CloudOff
        PeerRoute.UNKNOWN -> Icons.Rounded.Cloud
    }

private fun routeLabel(route: PeerRoute): String =
    when (route) {
        PeerRoute.NEARBY_DIRECT -> "Nearby · direct"
        PeerRoute.REMOTE_WIFI -> "Remote · Wi‑Fi"
        PeerRoute.REMOTE_CELLULAR -> "Remote · Cellular"
        PeerRoute.REMOTE_INTERNET -> "Remote connection"
        PeerRoute.RECONNECTING -> "Reconnecting"
        PeerRoute.OFFLINE -> "Offline"
        PeerRoute.UNKNOWN -> "Checking connection"
    }

private fun routeRecommendation(state: HappyTalkieUiState): String =
    when {
        state.callState == CallVisualState.RECONNECTING ->
            "Keeping the call alive while the route changes"

        state.peerConnection == PeerConnectionState.DISCONNECTED ->
            "CALL unavailable · TALK will wait and deliver later"

        state.peerRoute == PeerRoute.NEARBY_DIRECT ->
            "Direct phone/watch link · best route for CALL"

        state.peerConnection == PeerConnectionState.CONNECTED ->
            "CALL available · TALK is safer on an unstable link"

        else ->
            "Checking whether CALL is available"
    }

@Composable
private fun CallRow(
    state: HappyTalkieUiState,
    onCall: () -> Unit,
    onDecline: () -> Unit,
    onSpeakerToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callEyebrow(state),
                    style = MaterialTheme.typography.labelLarge,
                    color = callAccent(state),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = callTitle(state),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = callSupportingText(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (state.callState == CallVisualState.LIVE) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        onClick = onSpeakerToggle,
                        shape = CircleShape,
                        color =
                            if (state.speakerOn) Color(0xFF244D80)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 7.dp,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector =
                                    if (state.speakerOn) Icons.Rounded.VolumeUp
                                    else Icons.Rounded.VolumeOff,
                                contentDescription =
                                    if (state.speakerOn) "Turn speaker off"
                                    else "Turn speaker on",
                                modifier = Modifier.size(17.dp),
                                tint =
                                    if (state.speakerOn) Color(0xFF8FCCFF)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text =
                                    if (state.speakerOn) "Speaker on"
                                    else "Speaker",
                                style = MaterialTheme.typography.labelLarge,
                                color =
                                    if (state.speakerOn) Color(0xFFCBE5FF)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onCall,
                    enabled = state.callEnabled,
                    modifier = Modifier.size(78.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = callButtonColor(state),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF263752),
                        disabledContentColor = Color(0xFF8492AA),
                    ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = callIcon(state),
                            contentDescription = callActionLabel(state),
                            modifier = Modifier.size(25.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = callActionLabel(state),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (state.callState == CallVisualState.INCOMING) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onDecline,
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 4.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF7585),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Decline",
                            color = Color(0xFFFF9CA7),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TalkHero(
    state: HappyTalkieUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val recording = state.recording
    val container by animateColorAsState(
        targetValue = if (recording) Color(0xFFE43E57) else Color(0xFF1676FF),
        label = "talkContainer",
    )
    val scale by animateFloatAsState(
        targetValue = if (recording) 0.985f else 1f,
        label = "talkScale",
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TALK",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF76DBFF),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Voice message",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = if (recording) "Release to send" else "Hold",
                style = MaterialTheme.typography.labelLarge,
                color = if (recording) Color(0xFFFFB3BE)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .semantics {
                    role = Role.Button
                    contentDescription =
                        if (recording) "Release to send TALK"
                        else "Hold to record TALK"
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
            shape = RoundedCornerShape(32.dp),
            color = if (state.talkEnabled) container else Color(0xFF1B2941),
            contentColor = Color.White,
            shadowElevation = if (state.talkEnabled) 8.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = Color.White.copy(
                        alpha = if (state.talkEnabled) 0.16f else 0.08f
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (recording) Icons.Rounded.Stop
                            else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(31.dp),
                            tint = if (state.talkEnabled) Color.White
                            else Color(0xFF7C8AA2),
                        )
                    }
                }

                Spacer(Modifier.width(17.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !state.talkEnabled -> "TALK unavailable during call"
                            recording -> "Recording…"
                            else -> "Hold to talk"
                        },
                        fontSize = 22.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.talkEnabled) Color.White
                        else Color(0xFF8795AC),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = when {
                            !state.talkEnabled -> "Finish the live call first"
                            recording -> "Release when you’re done"
                            else -> "Stored and delivered when the Watch is reachable"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.talkEnabled) Color.White.copy(alpha = 0.78f)
                        else Color(0xFF6E7C93),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTalk(
    messages: List<VoiceMessage>,
    peerName: String,
    onPlay: (VoiceMessage) -> Unit,
    onDelete: (Set<String>) -> Unit,
    onClear: () -> Unit,
) {
    var managing by remember { mutableStateOf(false) }
    var selectedIds by remember(messages) {
        mutableStateOf(emptySet<String>())
    }
    var confirmClear by remember {
        mutableStateOf(false)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = {
                Text("Clear TALK history?")
            },
            text = {
                Text(
                    "This removes all saved voice messages from this device."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        managing = false
                        selectedIds = emptySet()
                        onClear()
                    }
                ) {
                    Text(
                        "Clear all",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmClear = false }
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                if (managing) "Manage TALK"
                else "Recent TALK",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )

        if (messages.isNotEmpty()) {
            TextButton(
                onClick = {
                    managing = !managing
                    if (!managing) {
                        selectedIds = emptySet()
                    }
                },
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                ),
            ) {
                Text(
                    text = if (managing) "Done" else "Manage",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (managing && messages.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(
                onClick = {
                    selectedIds =
                        if (selectedIds.size == messages.size) {
                            emptySet()
                        } else {
                            messages.map { it.id }.toSet()
                        }
                }
            ) {
                Text(
                    if (selectedIds.size == messages.size) {
                        "Deselect all"
                    } else {
                        "Select all"
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            if (selectedIds.isNotEmpty()) {
                TextButton(
                    onClick = {
                        val ids = selectedIds
                        selectedIds = emptySet()
                        onDelete(ids)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Delete ${selectedIds.size}")
                }
            }

            TextButton(
                onClick = { confirmClear = true }
            ) {
                Text(
                    "Clear all",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    } else if (!managing) {
        Text(
            text = "Tap a message to play · audio never auto-plays",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(10.dp))

    if (messages.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Incoming TALK waits here until you choose to play it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.forEach { message ->
                MessageRow(
                    message = message,
                    peerName = peerName,
                    managing = managing,
                    selected = message.id in selectedIds,
                    onClick = {
                        if (managing) {
                            selectedIds =
                                if (message.id in selectedIds) {
                                    selectedIds - message.id
                                } else {
                                    selectedIds + message.id
                                }
                        } else {
                            onPlay(message)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: VoiceMessage,
    peerName: String,
    managing: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val outgoing =
        message.direction ==
            VoiceDirection.OUTGOING

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color =
            if (selected) {
                Color(0xFF163860)
            } else {
                MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.72f
                )
            },
        border =
            if (selected) {
                BorderStroke(
                    1.dp,
                    Color(0xFF6DB5FF)
                )
            } else {
                null
            },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color =
                    if (outgoing) {
                        Color(0xFF1A6DF0)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            if (managing) {
                                if (selected) {
                                    Icons.Rounded.Done
                                } else {
                                    Icons.Rounded.PlayArrow
                                }
                            } else {
                                Icons.Rounded.PlayArrow
                            },
                        contentDescription =
                            if (managing) {
                                if (selected) {
                                    "Selected"
                                } else {
                                    "Select message"
                                }
                            } else {
                                "Play TALK"
                            },
                        tint = Color.White,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text =
                        if (outgoing) "Me"
                        else peerName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        "Voice message · ${message.displayTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text =
                    when {
                        managing && selected -> "Selected"
                        managing -> "Select"
                        else -> "Play"
                    },
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (selected) {
                        Color(0xFF90C8FF)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }
    }
}

private fun callEyebrow(state: HappyTalkieUiState): String =
    when (state.callState) {
        CallVisualState.LIVE -> "LIVE NOW"
        CallVisualState.INCOMING -> "INCOMING CALL"
        CallVisualState.OUTGOING -> "RINGING"
        CallVisualState.CONNECTING -> "CONNECTING"
        CallVisualState.RECONNECTING -> "RECONNECTING"
        CallVisualState.READY -> "LIVE CALL"
    }

private fun callTitle(state: HappyTalkieUiState): String =
    when (state.callState) {
        CallVisualState.LIVE -> "You’re connected"
        CallVisualState.INCOMING -> "${state.peerName} is calling"
        CallVisualState.OUTGOING -> "Calling ${state.peerName}"
        CallVisualState.CONNECTING -> "Opening live audio"
        CallVisualState.RECONNECTING -> "Keeping the call alive"
        CallVisualState.READY ->
            if (state.callEnabled) "Talk now" else "CALL unavailable"
    }

private fun callSupportingText(state: HappyTalkieUiState): String =
    when (state.callState) {
        CallVisualState.LIVE ->
            "Live audio · use Speaker when you want hands-free sound"

        CallVisualState.INCOMING ->
            "Answer or decline — nothing starts until you choose"

        CallVisualState.OUTGOING ->
            "Waiting for ${state.peerName} · tap CANCEL to stop ringing"

        CallVisualState.CONNECTING ->
            "Setting up the live audio channel"

        CallVisualState.RECONNECTING ->
            "Route changed · retrying before the call is ended"

        CallVisualState.READY ->
            if (state.callEnabled) {
                "Ring ${state.peerName} and speak in real time"
            } else {
                "TALK is still available and will deliver later"
            }
    }

private fun callActionLabel(state: HappyTalkieUiState): String =
    when (state.callState) {
        CallVisualState.LIVE -> "END"
        CallVisualState.INCOMING -> "ANSWER"
        CallVisualState.OUTGOING -> "CANCEL"
        CallVisualState.CONNECTING -> "END"
        CallVisualState.RECONNECTING -> "END"
        CallVisualState.READY -> "CALL"
    }

private fun callIcon(state: HappyTalkieUiState): ImageVector =
    when (state.callState) {
        CallVisualState.LIVE,
        CallVisualState.OUTGOING,
        CallVisualState.CONNECTING,
        CallVisualState.RECONNECTING ->
            Icons.Rounded.CallEnd

        CallVisualState.INCOMING,
        CallVisualState.READY ->
            Icons.Rounded.Call
    }

private fun callAccent(state: HappyTalkieUiState): Color =
    when (state.callState) {
        CallVisualState.LIVE,
        CallVisualState.INCOMING ->
            Color(0xFF67E6A0)

        CallVisualState.OUTGOING,
        CallVisualState.CONNECTING,
        CallVisualState.RECONNECTING ->
            Color(0xFFFFD35A)

        CallVisualState.READY ->
            if (state.callEnabled) {
                Color(0xFFFFD35A)
            } else {
                Color(0xFF8C9BB0)
            }
    }

private fun callButtonColor(state: HappyTalkieUiState): Color =
    when (state.callState) {
        CallVisualState.LIVE,
        CallVisualState.OUTGOING,
        CallVisualState.CONNECTING,
        CallVisualState.RECONNECTING ->
            Color(0xFFE6475D)

        CallVisualState.INCOMING ->
            Color(0xFF22B66F)

        CallVisualState.READY ->
            Color(0xFF1877FF)
    }

@Preview(
    showBackground = true,
    backgroundColor = 0xFF050A16,
    widthDp = 412,
    heightDp = 915,
)
@Composable
private fun PhonePreview() {
    HappyTalkiePhoneTheme {
        HappyTalkiePhoneScreen(
            state = HappyTalkieUiState(),
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
