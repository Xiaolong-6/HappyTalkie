# Wear CALL / TALK follow-up closure

Baseline: current `main` after conversation, device-identity, Priority CALL, and TEXT/timeline work.

This file replaces the original documentation-only backlog captured by PR #9.

## Closed in later architecture work

### CALL history visibility

Closed by the unified conversation timeline.

Wear now interleaves TEXT, TALK, and persisted CALL events by timestamp instead of hiding CALL history under a separate section.

## Closed in this PR

### TALK swipe-delete confirmation

Wear keeps swipe-left delete for TALK and now shows a short, glanceable:

`TALK deleted ✓`

confirmation after a successful delete action.

The confirmation is transient and does not alter the read state of other messages.

### Wear incoming CALL platform path

The old Wear path incorrectly treated full-screen intent notifications as available.

Wear OS does not support `setFullScreenIntent()` or the `USE_FULL_SCREEN_INTENT` permission.

The Wear build therefore:

- no longer declares `USE_FULL_SCREEN_INTENT`;
- no longer asks the user for full-screen-intent access;
- does not call `setFullScreenIntent()` for Watch notifications;
- keeps high-priority `CATEGORY_CALL` notification actions for Answer / Decline;
- keeps the dedicated in-app call screen when HappyTalky is already foregrounded;
- publishes active/outgoing Watch CALL as a Wear `OngoingActivity` for a one-tap return path.

Priority auto-answer remains explicitly gated on the Watch activity being resumed/visible. It does not silently start microphone capture from background.

### Abnormal live-call DISCONNECTED outcome

Reconnect timeout now uses a distinct protocol terminal signal:

`/happytalky/call/disconnected`

This prevents the remote endpoint from treating an abnormal reconnect failure as a normal `COMPLETED` call.

Both endpoints can now persist `DISCONNECTED` for the same abnormal terminal event.

A pure outcome policy test covers:

- normal active remote END -> `COMPLETED`;
- abnormal active remote disconnect -> `DISCONNECTED`;
- normal pre-answer remote END -> `CANCELLED_BY_PEER`.

## Validation

Required before merging PR #9:

- core unit tests;
- Phone debug build;
- Wear debug build;
- existing Phone/Wear screenshot render;
- visual check of 192 dp Wear Inbox after the delete-confirmation change;
- verify the Wear manifest contains no `USE_FULL_SCREEN_INTENT`;
- verify normal CALL END still records `COMPLETED`.
