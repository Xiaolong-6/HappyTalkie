# Debug distribution

HappyTalky CI publishes installable debug APKs as GitHub prereleases after a successful build.

## Latest main build

- Release tag: `debug-main`
- Phone: `HappyTalky-phone-debug.apk`
- Watch: `HappyTalky-watch-debug.apk`
- Metadata: `debug-dist.json`

## Pull requests

Each PR publishes its own rolling prerelease:

`debug-pr-<PR number>`

Each new successful build updates that same tag and its APKs; it does not create a tag per commit. The tag points to the commit actually built (the PR merge commit for PR builds).

After a same-repository PR is merged into `main`, CI automatically deletes its `debug-pr-<number>` prerelease and tag. Cleanup also runs after Android CI finishes, so a build racing with a merge cannot leave a recreated prerelease behind. Older merged PR debug tags are swept as well. Open PRs, PRs closed without merging, formal releases, human-created releases, and `debug-main` are retained.

Publishing and cleanup share a concurrency group. Publishing rechecks that the PR is still open and the build matches its current head. Fork PRs build and upload Actions artifacts but do not publish releases. Cleanup always runs the script from `main` and never executes code from PR artifacts.

After merging, download the matching phone/watch pair from `debug-main`. Actions APK and screenshot artifacts remain available according to GitHub retention settings. You can rerun **Clean merged PR debug releases** manually to retry a failed cleanup. API errors fail visibly; they are not treated as missing releases.

The phone and watch APKs in one release are always built from the same commit and use the CI debug signing identity.

## Installation model

Phone APKs can be downloaded directly on Android and handed to the system package installer.

**Current `main` does not contain an in-app Watch APK updater.** For Wear OS, ordinary apps cannot silently install arbitrary APKs on a paired watch. A future debug updater may transfer the matching Wear APK from phone to watch and launch the watch system installer, but the user would still need to confirm installation through the platform flow.

For the supported manual install paths, see [docs/DEPLOY.md](../docs/DEPLOY.md).

For production-like one-device management, use Google Play internal/closed testing, where phone/desktop Play Store can remotely request installation on a paired Wear OS device.
