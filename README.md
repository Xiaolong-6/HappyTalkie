# HappyTalkie

A deliberately simple Android + Wear OS companion app for a paired phone and Pixel Watch.

## What it does

HappyTalkie has only two primary controls:

- **CALL** — ring the paired device, answer an incoming call, or end the current session.
- **TALK** — press and hold to record; release to send.

When both devices are reachable, CALL establishes a lightweight session and TALK behaves like push-to-talk audio. If the other device is offline or does not answer, TALK becomes a store-and-forward voice message. The voice message is synchronized when the paired device reconnects.

There is no text chat.

## Connectivity model

HappyTalkie uses the Wear OS Data Layer API. Google Play services routes traffic:

- directly over Bluetooth when the phone and watch are nearby;
- through Google's network relay when Bluetooth is unavailable and both devices have Internet access (for example, phone on mobile data and Pixel Watch on Wi-Fi).

A Wi-Fi-only Pixel Watch is still physically offline when it has neither Bluetooth to the paired phone nor usable Wi-Fi. No application can overcome that hardware/network limitation. HappyTalkie's offline behavior is therefore to queue voice messages until connectivity returns.

## V0.1 scope

- Android phone app
- Wear OS watch app
- identical application ID and signing identity on both devices
- CALL / ANSWER / END signaling
- high-priority incoming-call notification, sound, and vibration
- long-press TALK recording
- voice transfer with persistent DataItems/Assets
- queued delivery after reconnection
- automatic playback for voice received during an active session
- notification for voice messages received outside an active session
- GitHub Actions debug APK build

V0.1 is **not full-duplex VoIP**. Audio is half-duplex: hold TALK, speak, release, then the clip is delivered and played on the other side.

## Development signing

The project uses one repository development keystore for both modules so phone and watch APKs keep the same signature across CI runs. This key is **development-only** and must never be reused for a Play Store production release.

## Build and install

See [docs/DEPLOY.md](docs/DEPLOY.md).

## Package

`com.xiaolong.happytalkie`

## License

MIT
