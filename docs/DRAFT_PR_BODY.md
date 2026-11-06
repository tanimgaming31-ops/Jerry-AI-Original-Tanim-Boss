Jerry AI Original now has an independently developed Kotlin/Compose foundation
and a confirmation-gated Android command system. Official intents take priority
over an optional user-enabled accessibility service; no Gemini Live or background
voice capability is claimed.

- Original six-screen UI, persistent themes, permission/service diagnostics, and
  an explicit 24-group feature inventory with missing/device-unverified status.
- Single-use confirmations, protected target/editor checks, scoped tap/swipe/scroll/
  text actions, and truthful observed/unverified/failure/cancellation reporting.
- Official launcher/search/Home/dialer/SMS-composer intents; no direct call or send.
- Tanim Boss license limited to original owned contributions, preserved upstream
  notices, source-review findings/hashes, and no old project source imported.
- Feature-branch/PR/manual GitHub Actions builds with APK, checksum, feature status,
  downloadable artifact URL in the run summary, and 30-day retention.

Validation: a clean Java 17/Android API 36 build passed 35 JVM tests (0 failures,
errors or skips), Android lint (0 issues), debug APK and instrumentation APK builds,
packaged permission/capability/license checks, signing verification and actionlint.
Four instrumentation scenarios compiled but were not run: no Android device is
attached. Dedicated app integrations, screenshots, multi-step tasks, memory,
Gemini/streaming/wake-word/background voice remain unimplemented. See
FEATURE_STATUS.md and docs/DEVICE_TESTING.md. No production release is requested.
