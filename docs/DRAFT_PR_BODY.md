Jerry AI Original gains an independently developed foreground Gemini Live client,
PCM microphone/playback pipeline and explicit session controls while preserving
the existing confirmed Android command system and application ID.

- API-key header authentication, provider model discovery, setup/activity/PCM/
  transcription/resumption messages, sanitized failures and bounded retries.
- Foreground-only microphone consent, focus/AEC, bounded queues, interruption,
  mute/Stop, stale-event guards and background shutdown. No AI device tools.
- Keystore-encrypted user key, persistent voice/language/persona settings, captions,
  and exact English/Bengali input controls. No credentials embedded in source/CI.
- Owner's complete 54-group inventory and structured backlog with explicit partial/
  planned/device-unverified status; privacy, checklist, architecture and changelog.
- Preserved action handlers, themes, confirmation gates, user data and third-party
  licenses. No old Tech GPTx implementation or protected UI assets imported.

Validation: 83 JVM tests passed, 0 failures/errors/skips; both APKs built; packaged
identity/permission/capability/license checks, signing, actionlint and shell checks
passed. Lint: 0 errors, 2 visible version-update advisories; pinned OkHttp 5.3.2 is
compatible with SDK 36 whereas 5.5.1 requires SDK 37. Six instrumentation scenarios
compiled but were not run. Actual Google/model/language/audio behavior and Keystore
hardware acceptance need a physical phone and the user's own provider key.

This is a draft review, not a claim that Phase 1 acceptance or all 54 groups are
complete. Wake word, background voice, Room memories and remaining phases stay
backlogged. No production release or automatic main merge is requested.
