# Architecture

## Product contract

HappyTalky keeps two primary child-facing voice actions:

- **CALL** = synchronous live two-way voice
- **TALK** = asynchronous persistent voice message

The conversation model also reserves **TEXT** as a secondary message type so text/emoji can join the same timeline without creating a separate storage or history subsystem.

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
- future TEXT messages.

Every row carries a stable ID, direction, creation time, read time and delivery state, plus type-specific fields. CALL rows also retain call ID, outcome, mode, start/end timestamps and duration.

TALK audio remains in app-private `voice-history` files. The first database access imports legacy TALK filename/read-state metadata and legacy CALL JSONL entries once, preserving existing installs while new writes go to Room.

The current Store APIs remain synchronous during this migration so the existing UI/state machine does not change behavior. A later UI pass can expose Room as Flow without changing the persisted schema.

## Companion discovery

The phone and watch advertise different static Wear OS capabilities:

- phone: `happytalky_phone`
- watch: `happytalky_watch`

`CapabilityClient.FILTER_REACHABLE` is used for CALL readiness and signaling. This matters because `NodeClient` can report Android nodes even when the HappyTalky companion app is not installed or does not support the current protocol.

The preferred peer is a reachable nearby/direct capability node; otherwise HappyTalky uses one reachable remote capability node.

Each endpoint also publishes persistent metadata at:

`/happytalky/device-info/<stable-device-id>`

The payload contains the endpoint role, manufacturer/model, app version, protocol version and supported feature capabilities. A stable app-scoped UUID identifies the endpoint across ordinary Data Layer reconnects. `Node.displayName` is retained only as a human-readable fallback while the persistent device-info item has not arrived.

This lets presentation use labels such as `Watch · Pixel Watch 3` and lets later protocol features be gated by advertised capabilities instead of assuming both endpoints were upgraded simultaneously.

## CALL state machine

Signaling uses transient `MessageClient` paths:

- `/happytalky/call/ring`
- `/happytalky/call/answer`
- `/happytalky/call/decline`
- `/happytalky/call/cancel`
- `/happytalky/call/busy`
- `/happytalky/call/end`

The intended lifecycle is:

~~~text
READY
  -> OUTGOING_RINGING -> ANSWERED -> CONNECTING -> LIVE
  -> INCOMING_RINGING -> ANSWERED -> CONNECTING -> LIVE

OUTGOING_RINGING -> CANCELLED | DECLINED | BUSY | TIMEOUT
INCOMING_RINGING -> ANSWERED | DECLINED | CANCELLED | MISSED
LIVE -> RECONNECTING -> LIVE
LIVE/RECONNECTING -> ENDED
~~~

A live call never begins merely because a RING arrived. The receiving user must answer.

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

Phone starts on the system-selected communication route. Speaker is an explicit user toggle. Wear requests its built-in communication speaker when available.

A foreground service keeps ringing/live-call state alive in the background and exposes Cancel/End from the ongoing notification.

## Reconnection

A transient channel or peer disconnect does not immediately destroy an active CALL.

HappyTalky:

1. marks the route `RECONNECTING`;
2. preserves the active call ID;
3. gives the route a short reconnect grace period;
4. retries the outgoing live channel from the original call initiator;
5. ends the call and recommends TALK only after the grace period expires.

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
- an importance-high CATEGORY_CALL notification;
- Answer and Decline notification actions;
- full-screen intent / lock-screen activity presentation when Android permits it;
- a dedicated full-screen Answer / Decline screen on Wear;
- the same Answer/Decline actions in the foreground Compose UI.

On Android 14+ the Wear app checks whether full-screen-intent access is available and sends the user once to the system permission page when it is not. The Watch activity is also allowed to wake and show over the lock screen.

If the user does nothing, the ring times out rather than remaining active indefinitely. Final CALL outcomes are persisted locally, including completed duration, declined, missed/no-answer, cancelled, busy, failed, and disconnected cases.

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
- chronological TALK timeline as the main screen content;
- incoming TALK bubbles on the left and outgoing TALK bubbles on the right;
- tap to play, with duration and timestamp shown in the bubble;
- long-press any TALK to enter multi-selection; the temporary top bar provides select-all and delete;
- current CALL state appears inside the conversation timeline instead of occupying a permanent dashboard card;
- bottom action bar owns the two primary actions: CALL and press-and-hold TALK;
- incoming CALL temporarily replaces the bottom actions with Decline / Answer;
- live CALL replaces the right action with the Speaker toggle;
- light/dark ColorSchemes follow the Android system theme while keeping the HappyTalky brand blue stable.

### Wear

Wear Material 3 presents a shorter wrist-first loop:

- route/action cue;
- central CALL / END / CANCEL control;
- a dedicated full-screen incoming CALL screen;
- a direct hold/release TALK surface at the bottom, with the press gesture owned by the surface itself rather than a disabled child button;
- swipe left from the home screen to enter Inbox;
- Inbox supports touch scrolling and the watch rotary/crown;
- unread incoming TALK is counted and bold/highlighted in chronological history, and loses emphasis after playback completes;
- each TALK row can be swiped left to reveal Delete;
- recent CALL history is shown below TALK history, and the latest CALL is summarized on the home screen.

Bulk history management and secondary explanation stay on the phone.

## Visual regression

CI renders Compose screenshot previews for:

- ready;
- outgoing call;
- incoming call;
- recording;
- offline;
- live;
- reconnecting.

Phone previews use a 412 x 915 dp surface. Wear previews use a 192 dp round device specification.
