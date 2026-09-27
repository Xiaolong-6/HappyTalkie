# Documentation map

This directory separates **current-state documentation** from **historical implementation notes** so old PR baselines do not silently become product requirements.

## Canonical current-state documents

- [../README.md](../README.md) — product overview and user-visible behavior.
- [ARCHITECTURE.md](ARCHITECTURE.md) — protocol, persistence, routing, CALL/TALK/TEXT state, Priority CALL, notifications and UI contracts.
- [DEPLOY.md](DEPLOY.md) — installation, device validation, build baseline and CI-equivalent checks.
- [UI_GUIDELINES.md](UI_GUIDELINES.md) — Android/Wear UI design baseline and project-specific interaction invariants.
- [../dist/README.md](../dist/README.md) — rolling debug release/tag lifecycle and distribution limits.
- [../AGENTS.md](../AGENTS.md) — repository invariants for development agents.

When these documents disagree with checked-in source or CI, treat the mismatch as documentation drift and fix the canonical document in the same change.

## Historical implementation notes

- [PHONE_UI_RELEASE_CLEANUP.md](PHONE_UI_RELEASE_CLEANUP.md)
- [WEAR_CALL_TALK_FOLLOWUPS.md](WEAR_CALL_TALK_FOLLOWUPS.md)

These files preserve decisions and closure context. Baseline commit IDs, PR numbers, and “before merge” checklists inside them are historical evidence, not current requirements.

## Maintenance rules

1. Update a canonical document whenever a change alters protocol semantics, permissions, user-visible CALL/TALK/TEXT behavior, build/deploy steps, debug release lifecycle, or UI interaction.
2. Label PR-specific planning/closure documents as historical once the work is merged.
3. Do not describe a library version as “latest” unless it was verified at the time of the change. Prefer “checked-in baseline” for repository dependencies.
4. Before Android or Wear UI work, re-check the current official Android documentation linked from [UI_GUIDELINES.md](UI_GUIDELINES.md).
5. Keep Phone and Watch behavior consistent where the shared protocol requires it, but do not force the same page layout onto both form factors.
