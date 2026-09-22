# Deploy HappyTalkie

HappyTalkie installs as two APKs with the same application ID:

- `mobile-debug.apk` -> Android phone
- `wear-debug.apk` -> Pixel Watch

The devices are different, so using the same application ID is intentional. Wear OS Data Layer also requires their signatures to match.

## Fastest path: GitHub Actions APKs

1. Open the repository on GitHub.
2. Open **Actions -> Android CI**.
3. Open the latest successful run.
4. Download both artifacts:
   - **HappyTalkie-mobile-debug**
   - **HappyTalkie-wear-debug**
5. Extract the ZIP files.

The CI workflow caches its Android debug keystore. Always install phone and watch APKs produced by the same repository build lineage.

## Install on the Android phone

Enable **Developer options** and **USB debugging** on the phone, connect it to the computer, then:

~~~text
adb devices
adb install -r mobile-debug.apk
~~~

If Android reports a signing mismatch from an earlier experimental build, uninstall the old HappyTalkie first, then reinstall:

~~~text
adb uninstall com.xiaolong.happytalkie
adb install mobile-debug.apk
~~~

## Install on Pixel Watch over Wi-Fi

On the Pixel Watch:

1. Enable **Developer options**.
2. Enable **ADB debugging**.
3. Enable **Wireless debugging**.
4. Choose **Pair new device** and note the pairing IP/port and code.

On the computer:

~~~text
adb pair WATCH_IP:PAIR_PORT
~~~

Enter the pairing code shown on the watch.

Then use the debug IP/port shown in Wireless debugging:

~~~text
adb connect WATCH_IP:DEBUG_PORT
adb devices
adb -s WATCH_IP:DEBUG_PORT install -r wear-debug.apk
~~~

If a previous HappyTalkie build has a different signature:

~~~text
adb -s WATCH_IP:DEBUG_PORT uninstall com.xiaolong.happytalkie
adb -s WATCH_IP:DEBUG_PORT install wear-debug.apk
~~~

## First launch

Open HappyTalkie once on both devices and grant:

- Microphone
- Notifications

The UI intentionally contains only two primary controls:

- **CALL**
- **TALK**

## Behavior

### CALL

Phone -> Watch and Watch -> Phone use the same flow:

1. Tap **CALL**.
2. The peer rings/vibrates.
3. On the receiving device, open HappyTalkie and tap **ANSWER**.
4. During a session, the same button becomes **END**.

If the peer cannot be reached, HappyTalkie shows that it is offline and suggests leaving a voice message.

### TALK

1. Press and hold **TALK**.
2. Speak.
3. Release **TALK**.

If a CALL session is active, the clip is delivered as push-to-talk audio.

If no session is active, the clip is stored as a voice message. Data Layer keeps the item until the paired peer can synchronize it.

## Test the Wi-Fi-only Pixel Watch case

First verify nearby Bluetooth operation.

Then test the actual remote scenario:

1. Make sure the Pixel Watch is connected to a saved Wi-Fi network.
2. Make sure the phone has Internet access.
3. Break the direct Bluetooth path by moving out of range or temporarily disabling Bluetooth.
4. Wake the watch and wait for Wi-Fi to be active.
5. Tap **CALL** on the phone.
6. Confirm that the watch rings.
7. Test TALK in both directions.

Google Play services can transition from Bluetooth to its cloud relay rather than maintaining a continuous low-latency socket. V0.1 therefore treats CALL as signaling and TALK as short audio clips rather than full-duplex VoIP.

## Offline test

1. Disconnect the watch from both phone Bluetooth and Wi-Fi.
2. Tap **CALL** on the phone: it should fail cleanly rather than hanging indefinitely.
3. Hold **TALK**, record a message, and release.
4. Reconnect the watch to Wi-Fi.
5. Confirm that the voice message arrives and plays.

## Build locally

Requirements:

- Android Studio with Android SDK 36
- JDK 17
- Gradle 8.13 if building from the command line without a wrapper

From Android Studio, open the repository root and build both `mobile` and `wear` debug variants. Local debug builds use your persistent `~/.android/debug.keystore`, so both modules retain matching signatures across rebuilds.

Command-line build with Gradle 8.13:

~~~text
gradle :core:testDebugUnitTest :mobile:assembleDebug :wear:assembleDebug
~~~

Outputs:

~~~text
mobile/build/outputs/apk/debug/mobile-debug.apk
wear/build/outputs/apk/debug/wear-debug.apk
~~~

## Current limitation

A Wi-Fi-only Pixel Watch cannot receive anything when it has:

- no Bluetooth connection to the paired phone, and
- no usable Wi-Fi connection.

HappyTalkie cannot change that hardware constraint. TALK messages are designed to survive it by synchronizing after connectivity returns.
