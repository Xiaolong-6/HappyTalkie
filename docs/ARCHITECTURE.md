# Architecture

## Product contract

HappyTalkie intentionally exposes only two primary communication actions:

- **CALL** = synchronous live two-way voice
- **TALK** = asynchronous persistent voice message

Text chat is intentionally out of scope.

## Modules

- `core`: shared Activity, call state, live audio, TALK storage, Data Layer transport, notifications and listener service.
- `mobile`: Android phone packaging.
- `wear`: Wear OS packaging.

Both app modules use application ID `com.xiaolong.happytalkie`.

## CALL

Signaling uses transient `MessageClient` messages:

- `/happytalkie/call/ring`
- `/happytalkie/call/answer`
- `/happytalkie/call/end`

After ANSWER, the caller opens:

- `/happytalkie/call/audio/<call-id>`

through `ChannelClient`.

Both sides then run simultaneously:

- microphone -> PCM16 -> channel output
- channel input -> PCM16 -> speaker

Audio parameters:

- 16 kHz
- mono
- 16-bit PCM
- `VOICE_COMMUNICATION` audio source
- acoustic echo cancellation and noise suppression when available

An Android foreground microphone service keeps the active call process alive.

## TALK

TALK records AAC/M4A:

- mono
- 16 kHz
- 32 kb/s
- maximum 60 s per recording

Every message gets a stable UUID and timestamp.

Local copies are stored under the app's private `voice-history` directory and are not deleted after playback.

Transfer uses a persistent DataItem + Asset:

- `/happytalkie/voice/<uuid>`

This keeps TALK independent from CALL and allows temporary offline synchronization.

The receiver saves its own local copy, deletes the synchronized DataItem after ingestion, notifies the user, and exposes the message in the conversation history.

## UI

The UI is deliberately compact but not bare:

- HappyTalkie brand mark
- status capsule
- large circular CALL/ANSWER/END control
- separate HOLD TO TALK control
- voice-message conversation history
- incoming and outgoing message bubbles
- tap any bubble to replay

The watch shows fewer recent messages than the phone to keep controls touch-friendly.

## Connectivity boundary

The app cannot communicate with a Wi-Fi-only Pixel Watch when the watch has neither:

- Bluetooth connectivity to its paired phone, nor
- usable Wi-Fi.

CALL fails as a live operation in that condition. TALK can remain locally available and Data Layer can synchronize queued state when connectivity returns.
