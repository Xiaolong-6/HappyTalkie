# Architecture

## Goal

HappyTalkie is deliberately narrower than a chat app. It exposes two child-friendly actions: CALL and TALK.

## Modules

- `core`: shared Activity, audio handling, Data Layer protocol, state, notifications and listener service.
- `mobile`: Android phone manifest and packaging.
- `wear`: Wear OS manifest and packaging.

Both app modules use application ID `com.xiaolong.happytalkie`.

## Data paths

Transient call signaling uses `MessageClient`:

- `/happytalkie/call/ring`
- `/happytalkie/call/answer`
- `/happytalkie/call/end`

Voice uses persistent DataItems with an Asset:

- `/happytalkie/voice/<uuid>`

Each voice DataItem carries:

- unique message ID
- origin role (phone/watch)
- optional active call ID
- timestamp
- AAC/M4A audio asset

A unique path prevents a new offline voice message from overwriting an older one.

## Why CALL and TALK use different transports

CALL is meaningful only in real time. MessageClient therefore reports failure when no paired node is currently reachable.

TALK must survive temporary disconnection. DataClient/DataItem is therefore used so the voice asset can remain pending and synchronize after reconnection.

## Audio model

V0.1 records mono AAC:

- MPEG-4 container
- AAC encoder
- 16 kHz sampling
- 32 kb/s
- maximum UI recording window: 60 seconds

This is intentionally optimized for speech and modest transfer size.

## Not VoIP yet

V0.1 does not maintain a continuous full-duplex media channel. A successful CALL creates a lightweight session; TALK remains press-to-talk.

If real-time full-duplex calling is later required, keep CALL signaling but move media to a dedicated VoIP transport such as WebRTC. That would also need an Internet signaling service and a wake/push strategy.
