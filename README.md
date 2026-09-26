# HappyTalky

HappyTalky is a deliberately simple Android + Wear OS companion app for a paired phone and Pixel Watch.

Its primary child-facing communication actions are:

- **CALL** — synchronous live two-way voice when the current phone/watch route is suitable for streaming.
- **TALK** — hold to record and release to send a persistent voice message. TALK remains the fallback when a live CALL is unavailable.

Secondary **TEXT** messaging is also supported. Text, TALK voice messages and CALL events share one local conversation timeline on each endpoint.

## Why two modes?

CALL is for synchronous conversation. Signaling uses Wear OS Data Layer messages; after the receiver explicitly answers, HappyTalky opens a bidirectional Data Layer channel and streams microphone audio in both directions.

TALK is asynchronous and privacy-preserving:

- received TALK messages are stored and notified, never auto-played;
- playback always requires a user action;
- Data Layer DataItems/Assets can queue while a peer is temporarily unavailable and synchronize after connectivity returns;
- saved TALK history can be replayed, selectively deleted, or cleared.

## TEXT

TEXT uses persistent Wear OS Data Layer DataItems, so a message can be written while the peer is temporarily unavailable and synchronize later. HappyTalky shows this state as **Queued**; it does not claim Delivered or Read until those states are actually acknowledged.

Phone uses a normal Material 3 composer. Wear shows a compact Message composer in Inbox and delegates real entry to the Wear OS system RemoteInput experience, including dictation, emoji, quick replies and the configured IME.

## Connectivity

HappyTalky exposes the route it can actually infer from Wear OS Data Layer and the local network:

- **Nearby · direct** — the paired peer is a nearby Data Layer node and can be reached directly. This is the preferred CALL route.
- **Remote · Wi-Fi** — the peer is reachable through the remote Data Layer path while this device has Wi-Fi. CALL is allowed, with TALK as the more tolerant fallback.
- **Remote · Cellular / Remote** — the peer is reachable remotely but the route is not suitable enough for this implementation's live ChannelClient audio. CALL is disabled and TALK is recommended.
- **Offline** — CALL is disabled; TALK can still be recorded and queued for later synchronization.
- **Reconnecting** — an active CALL keeps a short grace period while HappyTalky tries to restore the live channel after a route change.

The UI deliberately does not claim to know the remote peer's exact LTE/Wi-Fi leg when Wear OS does not expose it.

## CALL behavior

CALL has an explicit lifecycle:

1. CALL
2. ringing
3. answer / decline / cancel / busy / timeout
4. connecting
5. live
6. reconnecting after a transient route change, when needed
7. end

Incoming CALL uses an actionable high-priority call notification and lock-screen/full-screen presentation where Android permits it. The caller can cancel while ringing. Either side can end an active call.

A Watch can separately opt in to **Priority calls**. When enabled and advertised to the Phone, an unanswered ordinary call can expose a Priority action after 5 seconds. Priority never bypasses the Watch setting, and automatic answer is gated on the Watch Activity being visibly resumed before microphone capture starts.

Phone live calls include an explicit **Speaker** toggle. Phone audio does not force speaker mode by default. Wear uses its communication speaker route.

Live audio uses the `VOICE_COMMUNICATION` path with acoustic echo cancellation and noise suppression when the device provides them.

## UI

Phone and Watch share product state and communication logic, but not page layout:

- **Phone:** Jetpack Compose Material 3
- **Wear:** Wear Compose Material 3, designed independently for a small round screen

Wear prioritizes **route/status → CALL → TALK**, with a left-swipe unified Inbox for TEXT/TALK/CALL, explicit TALK playback/delete, crown scrolling, and a fixed system-input Message composer. Incoming calls use a dedicated full-screen wrist UI when the OS permits it.

Compose screenshot previews are rendered in CI so phone and 192 dp round-watch layouts can be visually reviewed before shipping APKs.

## Debug builds

Every successful CI build publishes phone and Wear APK artifacts. PR builds also maintain a rolling prerelease such as `debug-pr-4`; main maintains `debug-main`.

The phone and watch APKs from one build use the same package name and debug signing identity, which Wear OS Data Layer requires.

See [docs/DEPLOY.md](docs/DEPLOY.md).

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Package

`com.xiaolong.happytalky`

## License

MIT
