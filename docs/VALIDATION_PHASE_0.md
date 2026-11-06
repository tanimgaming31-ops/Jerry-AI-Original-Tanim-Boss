# Phase 0 validation report

Date: 6 November 2026. Current cloud instance only; no fresh-task restoration,
hosted CI run, emulator run, or Android phone run is claimed.

## Implemented subset

- Original Home, Features, Settings, Diagnostics, and About Compose screens.
- System/light/dark preferences backed by DataStore, unknown-value fallback,
  corrupt-preference recovery and generic storage error messages.
- Initial 24-group catalog: 20 planned groups, 4 partial foundation groups.
- Read-only permission states, settings links, on-return refresh, and privacy /
  ownership information. This is not a complete permission-management system.
- Tanim Boss license scoped to original owned materials; upstream license texts,
  including transitive Okio and Protocol Buffers notices, bundled in the APK.
- GitHub Actions workflow, cloud bootstrap, wrapper and packaged-APK verifier.

## Executed checks

| Check | Actual result |
| --- | --- |
| Java 17 | Microsoft OpenJDK 17.0.20.1 activated |
| SDK setup | Platform 36, Build Tools 36.0.0 and platform-tools installed |
| Bootstrap repeatability | scripts/setup-cloud.sh successfully ran against installed tools and verified cached downloads; a clean-machine run has not been tested |
| Gradle wrapper | 8.13 distribution SHA-256 and wrapper JAR official checksum verified |
| testDebugUnitTest | **9 executed, 9 passed, 0 failed, 0 errors, 0 skipped** |
| lintDebug | Passed; **0 issues** in generated XML report |
| assembleDebug | Passed; development-signed APK produced |
| assembleDebugAndroidTest | Passed; **3 device scenarios compiled, 0 executed** |
| APK manifest/assets verifier | Passed: expected package/API levels; only internal signature permission; no services, network or sensitive permissions; backup/cleartext flags; notices match source |
| APK signing verification | apksigner verify passed; Android debug signer, not release signing |
| Workflow validation | actionlint 1.7.7 passed; official downloaded archive checksum verified |
| Shell syntax / Git whitespace | bash -n and git diff --check passed |
| Device availability | adb devices returned no connected devices |

The nine JVM tests exercise real DataStore default settings, persistence across
storage reopen, unknown-value preservation, corrupt-file recovery, and five
permission-state cases. They do not exercise Android UI, audio, special access,
networking, or third-party apps.

Command executed on the final source configuration:

```bash
source scripts/cloud-env.sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
python3 scripts/verify-foundation-apk.py
```

Main APK: `app/build/outputs/apk/debug/app-debug.apk` (37,324,522 bytes).

SHA-256: `66d4209a1ac575ffdbccf9dda25509e087b3808a787e5c9ea7255c90e9e36c3a`

Generated reports: `app/build/reports/tests/testDebugUnitTest/index.html` and
`app/build/reports/lint-results-debug.html` (ignored build outputs).

## Missing and unverified

Gemini, real-time audio, interruption/reconnect, wake word, owner recognition,
background voice, multilingual personas, chat/history, all external integrations,
automation, screen observation, smart memory, and backup/restore remain **not
implemented**. Settings, permission management, security/privacy and recovery are
partial foundations, not completed feature groups. Historical inventory
completeness remains unverified.

All phone behavior remains **not tested**, including installation/launch,
rendering, accessibility/readability, theme persistence on Android, activity/process
recreation, settings navigation, permission-denial behavior, and OEM backup/transfer
handling. See DEVICE_TESTING.md. Three instrumentation scenarios are ready to run
on an authorized device. No runtime claims follow from compilation.

## Repository and environment state

Only the new Jerry-AI-Original-Tanim-Boss project was modified. No old project
source was imported, and no old Jerry/Maya/Zoya repository was changed. No commit,
push, merge, publication, or release was performed.

The toolchain and scripts work in this instance. Saving install_script/start_skill
to the cloud configuration returned `draft_not_editable`; persistence was **not
confirmed**. The existing draft still reads with empty script fields. Proposed
instructions are preserved in CLOUD_SETUP_PROPOSAL.json and in the transferable
source/APK bundle. To save them through the platform, open a new setup chat from
environment settings after any active publication finishes. This is separate
from the locally verified build and does not prevent local development.
