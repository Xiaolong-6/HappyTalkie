# AGENTS.md

## Product invariants

- Keep the primary UI child-friendly: CALL and TALK only.
- Text messaging is allowed as a secondary conversation capability; CALL and TALK remain the primary child-facing actions.
- CALL is transient real-time signaling.
- Locked Priority CALL is a direct Phone-to-Watch request: no ordinary-CALL delay, no Watch decline/end path, and no claim of background microphone capture that Android forbids.
- TALK must support store-and-forward delivery when the peer is temporarily offline.
- Phone and Wear OS apps must retain the same application ID and matching signatures.
- Never commit signing private keys, tokens, passwords, or service credentials.

## Build

Use JDK 17, compile SDK 37, AGP 9.1.1 and Gradle 9.3.1 (matching the checked-in build and CI configuration).

Primary validation:

~~~text
gradle :core:testDebugUnitTest :mobile:assembleDebug :wear:assembleDebug
~~~

## Change policy

For new protocol paths, keep them under `/happytalky`.
Conversation metadata belongs in the shared Room timeline; large audio payloads stay in app-private files/Data Layer Assets.
For voice DataItems, use unique paths so offline messages cannot overwrite one another.
Any change to CALL/TALK state transitions should be tested on both roles because most behavior is intentionally shared in `core`.
