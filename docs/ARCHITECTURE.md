# Architecture

## Product contract

HappyTalky keeps two primary child-facing voice actions:

- **CALL** = synchronous live two-way voice
- **TALK** = asynchronous persistent voice message

The conversation model includes **TEXT** as a secondary message type so text/emoji share the same timeline as TALK and CALL instead of creating a separate storage or history subsystem.

## Modules

- `core`: communication state machine, Data Layer transport, live audio, AEC/NS, reconnect policy, TALK storage, notifications, incoming-call actions, and shared observable UI state.
- `mobile`: Android phone Compose Material 3 activity and phone-specific interaction design.
- `wear`: Wear Compose Material 3 activity and round-screen interaction design.

Both application modules use application ID `com.xiaolong.happytalky` and must be signed identically.

## Conversation persistence

Phone and Watch each keep their own local Room database. They share the schema and protocol semantics, not one physical database.

The `conversation_items` table is the metadata source of truth for:

- TALK voice messages;
- CALL events;
- TEXT messages.

Every row carries a stable ID, direction, creation time, read time and delivery state, plus type-specific fields. CALL rows also retain call ID, outcome, mode, start/end timestamps and duration.

TALK audio remains in app-private `voice-history` files. The first database access imports legacy TALK filename/read-state metadata and legacy CALL JSONL entries once, preserving existing installs while new writes go to Room.

The current Store APIs remain synchronous during this migration so the existing UI/state machine does not change behavior. A later UI pass can expose Room as Flow without changing the persisted schema.

TEXT uses a stable UUID and is stored directly in Room. Outgoing text is initially `LOCAL`. If `DataClient.putDataItem()` accepts it while the peer is reachable, the UI may label the local send attempt `SENT`; if the peer is offline it remains `QUEUED` for later synchronization. Neither state claims remote receipt. HappyTalky does not label an outgoing message Delivered or Read without an explicit receiver acknowledgement. A reachable peer may send TEXT while device-info metadata is still refreshing; offline queueing requires previously confirmed `text_v1` support.

## Companion discovery

The phone and watch advertise different static Wear OS capabilities:

- phone: `happytalky_phone`
- watch: `happytalky_watch`

`CapabilityClient.FILTER_REACHABLE` is used for CALL readiness and signaling. This matters because `NodeClient` can report Android nodes even when the HappyTalky companion app is not installed or does not support the current protocol.

The preferred peer is a reachable nearby/direct capability node; otherwise HappyTalky uses one reachable remote capability node.

Each endpoint also publishes persistent metadata at:

`/happytalky/device-info/<stable-device-id>`

The payload contains the endpoint role, manufacturer/model, app version, protocol version, supported feature capabilities, and a publication timestamp. A stable app-scoped UUID identifies the endpoint across ordinary Data Layer reconnects. Peer metadata is accepted only when it is at least as fresh as the cached snapshot, so legacy persistent DataItems from an older install cannot overwrite current capabilities. `Node.displayName` is retained only as a human-readable fallback while the persistent device-info item has not arrived.

This lets presentation use labels such as `Watch · Pixel Watch 3` and lets later protocol features be gated by advertised capabilities instead of assuming both endpoints were upgraded simultaneously.

## CALL state machine

Signaling uses transient `MessageClient` paths:

- `/happytalky/call/ring`
- `/happytalky/call/answer`
- `/happytalky/call/decline`
- `/happytalky/call/cancel`
- `/happytalky/call/busy`
- `/happytalky/call/end`
- `/happytalky/call/disconnected`
- `/happytalky/call/priority` (legacy escalation compatibility)
- `/happytalky/call/priority-locked`

### Priority CALL

Locked Priority CALL is a separate immediate Phone-to-Watch request. It is not an escalation of an ordinary ringing CALL.

- New support is advertised through `priority_locked_call_v1`. The older `priority_call_v1` path is retained only for protocol compatibility with older builds.
- Phone can start locked Priority only from an idle, CALL-capable route and only after the Watch advertises the new capability.
- Starting Priority creates a fresh call ID and sends `/happytalky/call/priority-locked` immediately. It does not start a normal CALL first and has no delay.
- Watch does not expose an opt-out switch for locked Priority.
- A locked Priority incoming state has no Decline action and no ordinary missed-call timeout. The Watch call screen shows the locked state without NO/END controls.
- Once connected, the Watch cannot normally terminate a locked Priority call through Compose UI, notification actions, `CallActionReceiver`, or the foreground-service notification. The Phone remains allowed to cancel a pending request or end an active call.
- If Phone CANCEL races with Watch auto-answer, Watch accepts the Phone cancellation even after the local state has already moved from incoming to active.
- Route failure, process/system failure, or reconnect timeout can still terminate the call and persist `DISCONNECTED`.
- Android 14+ treats `RECORD_AUDIO` as a while-in-use permission. A Data Layer listener running while Watch is backgrounded therefore cannot lawfully start microphone capture by itself. When the Watch Activity is already resumed, locked Priority auto-answers immediately. Otherwise the locked incoming state and persistent high-priority notification are created immediately and auto-answer occurs when the Activity becomes foreground.
- CALL history stores `PRIORITY` mode so these calls remain auditable.

The normal CALL lifecycle remains:

~~~text
READY
  -> OUTGOING_RINGING -> ANSWERED -> CONNECTING -> LIVE
  -> INCOMING_RINGING -> ANSWERED -> CONNECTING -> LIVE

OUTGOING_RINGING -> CANCELLED | DECLINED | BUSY | TIMEOUT
INCOMING_RINGING -> ANSWERED | DECLINED | CANCELLED | MISSED
LIVE -> RECONNECTING -> LIVE
LIVE/RECONNECTING -> ENDED | DISCONNECTED
~~~

Locked Priority adds a distinct path:

~~~text
READY
  -> PRIORITY_LOCKED_WAITING
  -> CONNECTING
  -> LIVE

PRIORITY_LOCKED_WAITING -> CANCELLED_BY_PHONE | BUSY | FAILED
LIVE -> ENDED_BY_PHONE
LIVE/CONNECTING -> RECONNECTING -> LIVE | DISCONNECTED
~~~

A normal CALL never becomes live merely because a RING arrived; the receiver must answer. Locked Priority is the explicit exception, subject to Android's foreground microphone rule above.

After ANSWER, the initiator opens:

`/happytalky/call/audio/<call-id>`

through `ChannelClient`.

Both sides then run simultaneously:

- microphone -> PCM16 -> channel output
- channel input -> PCM16 -> communication output

Audio parameters:

- 16 kHz
- mono
- 16-bit PCM
- `VOICE_COMMUNICATION` audio source
- acoustic echo cancellation when available
- noise suppression when available

Phone starts on the system-selected communication route. Speaker is an explicit user toggle. Wear requests its built-in communication speaker when available. Priority CALL uses the same audio transport once connected; the locked policy changes call initiation and local termination rights, not PCM transport.

A foreground service keeps outgoing/live-call state alive in the background. Normal calls expose Cancel/End from the ongoing notification. An active locked Priority call on Watch deliberately omits End. On Wear OS, active calls are also published as an `OngoingActivity` so the watch face/launcher can provide a one-tap return path.

## Reconnection

A transient channel or peer disconnect does not immediately destroy an active CALL.

HappyTalky:

1. marks the route `RECONNECTING`;
2. preserves the active call ID;
3. gives the route a short reconnect grace period;
4. retries the outgoing live channel from the original call initiator;
5. ends the call and recommends TALK only after the grace period expires;
6. signals `/happytalky/call/disconnected` so both endpoints persist `DISCONNECTED` rather than allowing the remote side to misclassify an abnormal drop as `COMPLETED`.

This is intended for ordinary Bluetooth/Wi-Fi handovers and brief network interruptions, not indefinite background calling.

## Route model

`PeerRoute` is deliberately conservative:

- `NEARBY_DIRECT`: Data Layer reports the peer as nearby/direct. This is the preferred CALL route.
- `REMOTE_WIFI`: peer reachable remotely and the local device's active network is Wi-Fi. CALL is allowed.
- `REMOTE_CELLULAR`: peer reachable remotely and the local device's active network is cellular. TALK is recommended; new CALL is disabled.
- `REMOTE_INTERNET`: peer reachable but local route type is not known strongly enough. TALK is recommended.
- `OFFLINE`: no reachable peer.
- `RECONNECTING`: temporary active-call recovery state.

The app does not infer the remote peer's exact last-mile transport from local network state.

## Incoming CALL presentation

Incoming CALL uses:

- ringtone and vibration;
- an importance-high `CATEGORY_CALL` notification;
- Answer and Decline notification actions;
- phone-only full-screen intent / lock-screen activity presentation where Android permits it;
- a dedicated Answer / Decline screen whenever the Wear activity is already foregrounded;
- the same Answer/Decline actions in the foreground Compose UI.

Wear OS does not support `setFullScreenIntent()` or the `USE_FULL_SCREEN_INTENT` permission, so the Wear build does not request that permission or attempt that notification path. A normal background incoming call uses a dedicated high-importance Wear notification with Answer / Decline actions and the normal ring timeout.

A locked Priority incoming call instead posts an ongoing high-priority notification with only an Open action. It has no local Decline action and no missed-call timeout. If HappyTalky is already foregrounded, the Watch immediately proceeds to auto-answer. If it is backgrounded, the request remains locked and visible until the Phone cancels, the user opens HappyTalky and auto-answer proceeds, or a genuine route/system failure terminates it. The app does not attempt to bypass Android's background microphone restrictions.

Final CALL outcomes are persisted locally, including completed duration, declined, missed/no-answer, cancelled, busy, failed, and disconnected cases.

## TEXT

TEXT transfer uses a persistent DataItem:

`/happytalky/message/<uuid>`

The payload carries stable ID, origin role, creation time and UTF-8 text. Text is capped at 500 characters for the first protocol version.

A receiving endpoint performs idempotent insert-by-ID, consumes the synchronized DataItem, updates the shared timeline, and posts the same message notification family used by TALK. Replayed DataItems are deleted without producing duplicate user notifications.

Phone uses a Material 3 single-line composer with IME Send and a quick emoji affordance.

Wear keeps only a compact fixed composer in Inbox. Tapping it launches the system Wear RemoteInput flow through `androidx.wear:wear-input`, enabling dictation, emoji, predefined choices and the system IME. HappyTalky does not attempt to render a phone-style keyboard on a 192 dp round screen.

Opening the Watch Inbox marks incoming TEXT as read locally. TALK retains its stricter playback-completes-read rule.

## TALK

TALK records AAC/M4A:

- mono
- 16 kHz
- 32 kb/s
- maximum 60 s per recording

Every message gets a stable UUID and timestamp. Its metadata is stored in the unified conversation database.

Local audio copies are stored under the app-private `voice-history` directory.

Transfer uses a persistent DataItem + Asset:

`/happytalky/voice/<uuid>`

This makes TALK independent from CALL and lets a local write synchronize after a temporary disconnect.

On receive, HappyTalky:

1. saves its own local copy;
2. removes the synchronized DataItem after ingestion;
3. posts a notification;
4. updates history;
5. **does not play audio automatically**.

Incoming TALK keeps an unread flag on each device until playback completes. Unread TALK drives the notification count and Watch Inbox emphasis; deleting a message also removes its unread state.

The phone history supports tap-to-play, selective deletion, select-all, and clear-all.

## UI contract

Phone and Watch share `HappyTalkyUiState`, not presentation code.

### Phone

Compose Material 3 follows a voice-messenger information architecture:

- compact conversation header with peer reachability and CALL/TALK availability;
- one chronological conversation timeline containing TEXT, TALK and persisted CALL events;
- incoming TALK bubbles on the left and outgoing TALK bubbles on the right;
- tap to play, with duration and timestamp shown in the bubble;
- long-press any TALK to enter multi-selection; the temporary top bar provides select-all and delete;
- current CALL state appears inside the conversation timeline instead of occupying a permanent dashboard card;
- bottom action area keeps CALL and press-and-hold TALK on the first row and a Message composer beneath them;
- the conversation header exposes **Priority call** as a direct action when the connected Watch advertises locked-Priority support; it creates a separate immediate request rather than escalating a normal CALL;
- incoming CALL temporarily replaces the bottom actions with Decline / Answer;
- live CALL replaces the right action with the Speaker toggle;
- light/dark ColorSchemes follow the Android system theme while keeping the HappyTalky brand blue stable.

### Wear

Wear Material 3 presents a shorter wrist-first loop:

- route/action cue;
- central CALL / END / CANCEL control;
- a dedicated full-screen incoming CALL screen;
- a direct hold/release TALK surface at the bottom, with the press gesture owned by the surface itself rather than a disabled child button;
- swipe left from the home screen to enter the unified Inbox;
- Inbox supports touch scrolling and the watch rotary/crown;
- unread incoming TALK is counted and bold/highlighted in chronological history, and loses emphasis after playback completes;
- TEXT, TALK and CALL rows are interleaved by timestamp; every persisted row can be swiped left to reveal Delete, with a short type-specific confirmation. Deletion is local history management; TEXT/CALL deletion is not a remote recall operation, while TALK deletion also removes its local audio file;
- persisted CALL events are interleaved with TEXT/TALK by timestamp, while the latest CALL is still summarized on the home screen;
- locked Priority incoming presentation is visually distinct, exposes no Decline control, and auto-answers as soon as the Watch Activity is legally able to start microphone capture;
- during an active locked Priority call the Watch shows the Priority state but no local END control;
- a fixed compact Message composer launches the system RemoteInput/IME with emoji and dictation support.

Bulk history management and secondary explanation stay on the phone.

## Visual regression

CI renders Compose screenshot previews for:

- ready;
- outgoing call;
- incoming call;
- recording;
- offline;
- live;
- reconnecting;
- phone direct Priority ready/requested states and dialog;
- Watch locked Priority incoming and locked live states;
- Phone mixed TEXT/TALK conversation;
- Watch mixed TEXT/TALK/CALL Inbox.

Phone previews use a 412 x 915 dp surface. Wear previews use a 192 dp round device specification.
