# Deploy HappyTalky

HappyTalky installs as two APKs with the same application ID:

- `HappyTalky-phone-debug.apk` -> Android phone
- `HappyTalky-watch-debug.apk` -> Pixel Watch

Using the same application ID is intentional. Wear OS Data Layer also requires matching signatures.

## Fastest path: rolling debug release

Successful CI builds publish direct APK assets in a rolling prerelease:

- PR builds: `debug-pr-<PR number>`
- main: `debug-main`

Each release contains:

- `HappyTalky-phone-debug.apk`
- `HappyTalky-watch-debug.apk`
- `debug-dist.json`

The metadata records the exact source commit. Phone and watch APKs from one release are built together and use the same CI debug signing identity.

GitHub Actions artifacts remain available as a secondary path.

## Rename cutover

HappyTalky 0.3.0 uses the new package `com.xiaolong.happytalky`. It intentionally does not upgrade the old `com.xiaolong.happytalkie` install. Remove the old phone/watch app after installing the renamed build so two launcher entries and two Data Layer endpoints cannot be confused.

## Install on the Android phone

Download `HappyTalky-phone-debug.apk` on the phone and open it.

Android may ask you to allow that browser/file manager to install unknown apps. The system package installer must still confirm the installation.

For ADB:

~~~text
adb install -r HappyTalky-phone-debug.apk
~~~

If Android reports a signing mismatch from an older experimental build:

~~~text
adb uninstall com.xiaolong.happytalky
adb install HappyTalky-phone-debug.apk
~~~

## Install on Pixel Watch over Wireless debugging

On the Pixel Watch:

1. Enable **Developer options**.
2. Enable **ADB debugging**.
3. Enable **Wireless debugging**.
4. Choose **Pair new device** and note the pairing IP/port and code.

Pair:

~~~text
adb pair WATCH_IP:PAIR_PORT
~~~

Then use the separate debug connection port shown on the watch:

~~~text
adb connect WATCH_IP:DEBUG_PORT
adb -s WATCH_IP:DEBUG_PORT install -r HappyTalky-watch-debug.apk
~~~

If the existing watch build has a different signature:

~~~text
adb -s WATCH_IP:DEBUG_PORT uninstall com.xiaolong.happytalky
adb -s WATCH_IP:DEBUG_PORT install HappyTalky-watch-debug.apk
~~~

### Phone-only debugging

A phone can act as the ADB client if it has an Android ADB client installed.

The same Wear OS pairing flow applies:

1. Watch -> **Wireless debugging -> Pair new device**.
2. Pair from the phone ADB client using the pairing port/code.
3. Connect to the watch's separate debug port.
4. Install/update `HappyTalky-watch-debug.apk`.

An ordinary third-party Android app cannot silently sideload an arbitrary APK onto the watch. Wear OS still requires the platform's install authorization/confirmation path.

## First launch

Open HappyTalky once on both devices and grant:

- Microphone
- Notifications

For incoming CALL, Android may also control whether full-screen call notifications are permitted. If full-screen presentation is unavailable, the high-priority call notification still exposes Answer/Decline.

## Behavior to verify

### Nearby CALL

1. Keep phone and watch paired and nearby.
2. Confirm the UI reports **Nearby · direct**.
3. Tap **CALL**.
4. Verify the receiving device rings and shows Answer/Decline.
5. Verify the caller can **CANCEL** before answer.
6. Answer.
7. Verify live two-way audio.
8. On phone, toggle **Speaker** on/off.
9. Verify either side can **END**.
10. Verify AEC/NS behavior by speaking with both devices in the same room.

### Incoming CALL while app is backgrounded

1. Background HappyTalky on the receiving device.
2. Start CALL from the peer.
3. Verify a call-style notification appears.
4. Verify Answer and Decline work.
5. If the OS allows full-screen call intents, verify the incoming UI appears over the lock screen.
6. Ignore one call and verify it times out rather than ringing forever.

### Route change / reconnect

1. Start a live CALL on the preferred nearby route.
2. Change network conditions so the route briefly disappears.
3. Verify UI changes to **Reconnecting**.
4. Restore a valid route inside the grace period.
5. Verify live audio returns without creating a new call.
6. Repeat while keeping the route unavailable; verify the call eventually ends and TALK is recommended.

### Remote Wi-Fi

When the peer is remotely reachable and the local active route is Wi-Fi:

- UI reports **Remote · Wi-Fi**;
- CALL is available;
- TALK remains the safer fallback for an unstable link.

### Cellular / uncertain remote route

For a remote peer while the local active route is cellular, or when the remote route cannot be classified strongly enough:

- CALL is disabled for new sessions;
- TALK remains available;
- UI recommends TALK.

This is intentional: the current live implementation uses a continuous Data Layer `ChannelClient`, while TALK uses persistent DataItem/Asset synchronization.

### TALK privacy and history

1. Hold **TALK**.
2. Speak.
3. Release.
4. On the receiving device, verify a notification appears.
5. Verify the audio **does not auto-play**.
6. Tap the saved message to play it.
7. On phone, long-press a TALK bubble to enter multi-selection.
8. Select one or more messages and delete them from the temporary selection bar.
9. Select all messages and verify the bulk delete path clears the history.
10. On Watch, open **Inbox** and verify saved TALK messages can be explicitly played there.

### Offline TALK

1. Make the peer unavailable.
2. Verify CALL is disabled.
3. Record a TALK.
4. Verify it is saved/queued rather than discarded.
5. Restore connectivity.
6. Verify the Data Layer synchronizes the TALK.

## Build locally

Current build baseline:

- Android Gradle Plugin 9.1.1
- Gradle 9.3.1
- compile SDK 37.0
- target SDK 36
- JDK 17
- Compose BOM 2026.09.00
- phone Material 3
- Wear Compose Material 3 1.7.0

The CI installs the Android 37 preview platform package explicitly while targetSdk remains 36.

Build:

~~~text
gradle :core:testDebugUnitTest :mobile:assembleDebug :wear:assembleDebug
~~~

Outputs:

~~~text
mobile/build/outputs/apk/debug/mobile-debug.apk
wear/build/outputs/apk/debug/wear-debug.apk
~~~

## Visual regression

CI renders Compose screenshot previews for both platforms before publishing debug APKs.

The screenshot artifacts are:

- `HappyTalky-phone-ui-screenshots`
- `HappyTalky-watch-ui-screenshots`

Use these to catch clipping, overlap, disabled-state errors, and small-round-screen regressions before installing on hardware.
