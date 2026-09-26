# HappyTalkie

HappyTalkie is a deliberately simple Android + Wear OS companion app for a paired phone and Pixel Watch.

It has two communication modes:

- **CALL** — ring the paired device, answer, then speak naturally over a live two-way audio stream.
- **TALK** — hold to record and release to send a persistent voice message.

There is no text chat.

## Why two modes?

CALL is for synchronous conversation. After ANSWER, HappyTalkie opens a bidirectional Wear OS Data Layer channel and streams microphone audio in both directions.

TALK is asynchronous. It works without a call, is stored in a simple phone/watch voice conversation history, and can be replayed later. Data Layer DataItems/Assets allow TALK messages to synchronize after temporary disconnection.

## Connectivity

HappyTalkie uses Wear OS Data Layer:

- Bluetooth when phone and watch are nearby;
- Wi-Fi / Google Play services connectivity when the watch is away from the phone but online.

A Wi-Fi-only Pixel Watch cannot communicate while it has neither Bluetooth to its paired phone nor usable Wi-Fi. HappyTalkie cannot change that hardware limitation.

## 0.2.0

- real two-way CALL audio after ANSWER
- persistent TALK voice-message history
- replay any saved TALK message
- polished phone and Pixel Watch UI
- HappyTalkie launcher and in-app branding
- incoming call ringing/vibration
- foreground microphone service for active calls
- Android phone + Wear OS APKs from GitHub Actions

## Signing

Wear OS Data Layer requires the phone and watch apps to have the same package name and matching signatures.

- local debug builds use your persistent Android debug keystore;
- GitHub Actions caches a CI debug keystore;
- no signing private key is stored in this repository.

## Build and install

See [docs/DEPLOY.md](docs/DEPLOY.md).

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Package

`com.xiaolong.happytalkie`

## License

MIT
