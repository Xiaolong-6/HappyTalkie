# AGENTS.md

## Product invariants

- Keep the primary UI child-friendly: CALL and TALK only.
- Do not add text chat.
- CALL is transient real-time signaling.
- TALK must support store-and-forward delivery when the peer is temporarily offline.
- Phone and Wear OS apps must retain the same application ID and matching signatures.
- Never commit signing private keys, tokens, passwords, or service credentials.

## Build

Use JDK 17, Android SDK 36, AGP 8.13.2 and Gradle 8.13.

Primary validation:

~~~text
gradle :core:testDebugUnitTest :mobile:assembleDebug :wear:assembleDebug
~~~

## Change policy

For new protocol paths, keep them under `/happytalkie`.
For voice DataItems, use unique paths so offline messages cannot overwrite one another.
Any change to CALL/TALK state transitions should be tested on both roles because most behavior is intentionally shared in `core`.
