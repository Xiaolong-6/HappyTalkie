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
- identical application ID on both devices
- CALL / ANSWER / END signaling
- high-priority incoming-call notification, sound, and vibration
- long-press TALK recording
- voice transfer with persistent DataItems/Assets
- queued delivery after reconnection
- automatic playback of received voice
- GitHub Actions debug APK build

V0.1 is **not full-duplex VoIP**. Audio is half-duplex: hold TALK, speak, release, then the clip is delivered and played on the other side.

## Signing

Wear OS Data Layer requires the phone and watch apps to have both the same package name and matching signatures.

- Local Android Studio/Gradle builds use the normal persistent Android debug keystore on your computer, so both APKs match across local rebuilds.
- GitHub Actions caches one CI debug keystore so normal CI rebuilds also remain compatible.
- No signing private key is stored in the repository.

For a future Play Store release, use a real release key stored outside source control.

## Build and install

See [docs/DEPLOY.md](docs/DEPLOY.md).

## Package

`com.xiaolong.happytalkie`

## License

MIT
