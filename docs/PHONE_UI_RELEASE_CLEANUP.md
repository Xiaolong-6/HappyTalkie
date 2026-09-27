# Phone conversation polish and debug release lifecycle

Based on main `677a919` (includes PR #13's CALL/text/history fixes).

## Changes

- Keep CALL and TALK as primary actions, with a smaller visual emphasis on the outlined idle CALL button. Incoming/live/priority call controls retain their existing behavior.
- Display the device name without its redundant role prefix; truncate long titles.
- Show a sender label at the start of an incoming group, after a call/outgoing message, or after five minutes. Keep every underlying timeline item.
- Render short emoji-only replies without a bubble; retain delivery/read metadata.
- Display historical calls as compact lines and make cancellation/decline ownership explicit. Preserve individual outcomes and timestamps rather than folding unlike outcomes together.
- Show actual MediaPlayer progress in voice bubbles. Tap continues to replay from the start; the track does not offer seeking.
- Replace an unavailable text field with a compact hint. Preserve PR #13's capability policy and keep draft text through temporary unavailability.
- Add screenshot scenarios matching the supplied dense history, dark/compact display, large text, and unavailable composer.

## Priority call discovery

Phone header now always exposes **Priority call**. Its dialog explains whether Watch support is unknown, auto-answer is off, the call is still inside the five-second delay, or escalation is ready. Eligible users can start the ordinary call from the dialog and request priority after the existing delay. The Watch Inbox's tiny unlabelled ON/OFF control becomes a full-width, labelled auto-answer switch. Protocol, opt-in default, delay, and foreground-only auto-answer policy remain unchanged.

## Release policy

See [debug distribution](../dist/README.md). PR APK downloads remain available under one rolling `debug-pr-N` release while the PR is open. Merge-triggered cleanup removes only reserved debug PR tags for merged same-repository PRs targeting main. Published non-prereleases and releases from other authors are protected. Formal versions and `debug-main` are never cleanup candidates.

## Validation

- `node --test .github/scripts/cleanup-debug-releases.test.cjs`
- CI: core unit tests, phone/watch debug APK builds, and native Compose screenshots.
- Manual device follow-up: replay progress, repeated replay, recording press/release/cancel, keyboard, and live phone/watch CALL. Screenshot rendering cannot verify audio or physical device interaction.
