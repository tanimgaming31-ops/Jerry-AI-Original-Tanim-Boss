# Confirmed-actions validation report — 0.2.0

Date: 6 November 2026. This report describes the current cloud build only.
**No Android phone/emulator behavior or hosted GitHub Actions run is verified.**
The original Phase 0 report is retained in VALIDATION_PHASE_0.md.

## Implemented code

- Original Actions UI and bounded command parser, with typed or external Android
  speech-recognition input. Recognition only fills text; it cannot execute actions.
- Preview/confirmation for every action; immutable action/target, 30-second expiry,
  single-use tokens, cancel/replacement and no automatic action retries.
- Official-first Android launcher, ACTION_SEARCH, Home, dialer and SMS-composer
  intents; explicit handler/security failures do not trigger accessibility bypass.
- Optional OS-bound AccessibilityService plus separate session consent; selected
  foreground-app scope, keyguard/protected-target checks, bounded traversal and
  serialized dispatch. OS enablement alone leaves automation disarmed.
- Unique-label taps, directional swipes/scrolls, Back, and text replacement in a
  focused/unique eligible field. Search fallback only fills a clearly identified
  search field, requiring manual submission. Password flags and password input
  variations are excluded, and no screenshot capability is requested.
- Observed UI/field changes versus unverified requests, explicit failures, timeouts
  and cancellation. An accepted operation or changed UI does not prove message
  delivery, a call, a transaction or completed search results.
- Updated service diagnostics, privacy disclosure, 24-group inventory, architecture,
  command documentation, device checklist, CI artifact checks and tests.

## Executed checks

| Check | Actual result |
| --- | --- |
| testDebugUnitTest | **35 executed, 35 passed, 0 failures/errors/skips** |
| lintDebug | Passed; **0 issues** in generated XML report |
| assembleDebug | Passed; 0.2.0-confirmed-actions development APK built |
| assembleDebugAndroidTest | Passed; **4 device scenarios compiled, 0 executed** |
| Packaged manifest/capability checks | Passed: package/API levels; no network, microphone, contact, direct-call, SMS or broad package permission; only expected accessibility service behind BIND_ACCESSIBILITY_SERVICE; expected metadata/gesture/content flags; no screenshot/key-filter/touch-exploration/magnification capability |
| Packaged licenses/privacy flags | Passed: bundled license files match source; backup and cleartext flags verified |
| APK signing | apksigner verify passed; Android debug signer, not release signing |
| Workflow validation | actionlint 1.7.7 passed; no hosted CI execution claimed |
| Shell syntax / whitespace | bash -n and git diff --check passed |
| Device availability | adb devices reported no connected Android devices |

The JVM suites are: 22 command/parser/confirmation/routing/target-policy tests,
4 editor/search-policy tests, 4 real DataStore tests and 5 permission-state tests.
They cover all action types requiring confirmation, cancellation/expiry/reuse,
immutable targets, official API priority, disabled-consent rejection, refusal to
fall back after explicit Android denial, malformed/service-code rejection,
password variations, conservative search semantics and protected-package rules.
They **do not execute Android intents, AccessibilityNodeInfo operations, gestures,
service lifecycle or speech recognition**. Those require device evidence.

Executed against the final source configuration:

```bash
source scripts/cloud-env.sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
python3 scripts/verify-foundation-apk.py
```

Java 17, Android SDK 36, Build Tools 36.0.0, Gradle 8.13, AGP 8.13.2 and Kotlin
2.2.21 were used. No new runtime dependency or embedded credential was added.
Generated reports are under app/build/reports/ (ignored build outputs).

Main APK: `app/build/outputs/apk/debug/app-debug.apk` (28,705,365 bytes).

SHA-256: `104860f353f6b5412991e0af8b862a1526a9116415d0fba5a11fe479e6b6e526`

## Limitations and missing work

All actual phone behavior is **NOT TESTED**: service enable/disconnect and consent
reset, speech provider results, target-app layouts, intent handler availability,
permission/security screens, locks, gestures, editable fields, ambiguous targets,
scope changes, timeouts, UI rendering and lifecycle. Follow DEVICE_TESTING.md.
No compatibility claim is made for any WhatsApp/YouTube/Spotify/Instagram version.

The Android API can accept a request without exposing a reliable completion
signal. Such outcomes are marked UNVERIFIED. UI snapshot differences are limited
observations; they cannot prove external effects. Accessible nodes and gestures
are not atomic with screen changes. Cancellation cannot undo an already accepted
operation, so unverified sensitive actions must not be blindly retried.

Direct call placement, direct message sending, contacts lookup, search-result
completion verification, screenshots, multi-step tasks, Gemini Live, real-time
audio, wake word, owner recognition, background microphone, multilingual personas,
chat/history, smart memory and backup/restore are not implemented. There are
9 partial feature groups and 15 planned groups; historical completeness is still
unverified. See ACTIONS.md and FEATURE_INVENTORY.md for exact boundaries.

Only Jerry-AI-Original-Tanim-Boss was edited. No other repository was changed or
used as a code/asset template. At the time of this implementation report no commit, push, merge, release or
publication had been performed. The separately authorized feature-branch
publication and its clean verification are recorded in PUBLICATION_VERIFICATION.md. Prior cloud-draft persistence remained unavailable because that draft
was uneditable; reusable local setup scripts and CLOUD_SETUP_PROPOSAL.json remain
in the source bundle. No new platform configuration was needed for this increment.
