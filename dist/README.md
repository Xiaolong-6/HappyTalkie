# Debug distribution

HappyTalkie CI publishes installable debug APKs as GitHub prereleases after a successful build.

## Latest main build

- Release tag: `debug-main`
- Phone: `HappyTalkie-phone-debug.apk`
- Watch: `HappyTalkie-watch-debug.apk`
- Metadata: `debug-dist.json`

## Pull requests

Each PR publishes its own rolling prerelease:

`debug-pr-<PR number>`

For example, the current UI work uses `debug-pr-4`.

The phone and watch APKs in one release are always built from the same commit and use the CI debug signing identity.

## Installation model

Phone APKs can be downloaded directly on Android and handed to the system package installer.

For Wear OS, ordinary apps cannot silently install arbitrary APKs on a paired watch. The planned debug updater will transfer the matching Wear APK from phone to watch and launch the watch system installer, so the user only needs to confirm the install on the watch.

For production-like one-device management, use Google Play internal/closed testing, where phone/desktop Play Store can remotely request installation on a paired Wear OS device.
