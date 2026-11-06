# Physical Android test checklist — Phase 1

All physical-phone and actual Gemini checks below are **NOT RUN** in Codex Cloud.
Record phone/OEM/Android API, app commit/APK checksum, chosen model, voice/language,
provider access, test result and limitations. Never record API keys or private audio.
A compiled APK or mock WebSocket test is not proof of these behaviors.

## Setup, data and regression

- Install over the existing app only with the same application ID and matching
  signing certificate. Compare the artifact SIGNING_CERTIFICATE.txt or apksigner
  output first. Fresh GitHub runners normally generate different debug keys unless
  stable signing was separately configured securely. Never uninstall/clear data to
  work around a signature mismatch. Preserve the installed app/data and report the
  signing blocker; private signing keys must never enter source/artifacts.
  With compatible signing, verify theme and prior preferences remain.
- Open all seven pages, original Actions flow, diagnostics and ownership/legal UI.
  Recheck confirmed launcher, Back/Home, search, tap, scroll/swipe and text actions
  with harmless data; preserve confirmation/security gates and truthful results.
- Run connectedDebugAndroidTest on the authorized phone. Six scenarios compile:
  existing navigation/theme/preview/permissions and Voice screen/isolated Keystore.
  The Keystore test uses a unique test directory/alias, never the user's real key.
- Save/read/remove a test provider credential. Check encryption, tamper/corruption,
  app upgrade, device restart and secure-storage failure. Failed reads must not
  erase encrypted data. Verify no keys in backups, logs, source or main APK.

## Gemini and microphone

- Select a currently supported Live model available in your free-tier account.
  Check valid/invalid/empty key, revoked key, unsupported model/voice, quota/rate
  limit, DNS/TLS failure and no network. Never label provider errors as success.
- Verify model discovery and a real setupComplete before microphone capture.
  Deny microphone, deny permanently, revoke while active and use OS privacy toggle.
- Test real bidirectional PCM: intelligible input, response audio, microphone
  indicator, routing, volume, focus, headphones/speaker and supported sample rates.
  Review audio underrun/overflow behavior; no unbounded queues or simultaneous mics.
- Test speaker echo/noise, AEC supported/unsupported, short/quiet speech, long turns,
  manual Interrupt, speech barge-in during generation and after generation ends
  but playback remains queued. Old audio must not resume or mix with the new reply.

## Lifecycle and recovery

- Start/Stop repeatedly; Stop while loading the key, connecting, thinking, speaking
  and reconnecting. Mute during speech/playback; unmute must not play stale turns.
- Leave the app, lock screen, Home, open another app, invoke the external Actions
  recognizer, rotate, lose/gain focus, receive a call, and kill/relaunch process.
  Phase 1 must stop on background/focus loss and never auto-start after relaunch.
- Drop Wi-Fi/mobile data; test up to three retries, setup timeout, server close and
  GoAway. Microphone pauses while reconnecting; no captured audio is replayed.
  A resumable handle is memory-only; context continuation is not guaranteed.
- Test rapid Stop/Start and stale socket/mic/focus callbacks; no old session may
  capture, play, overwrite state or revive a stopped connection.

## Languages, voices and persona

- Verify each language family requested in the inventory (Bengali, English, Hindi/
  Hinglish, Urdu, Arabic, Tagalog, Indonesian, Tamil, Telugu, Kannada, Malayalam,
  Marathi, Gujarati, Punjabi, Odia). Requests are prompt-based and model-dependent;
  record unsupported recognition/output instead of claiming universal support.
- Test every selected provider voice and six persona modes with real output.
- Verify persistent settings, English/Bengali final-input mode switching and the
  documented new-context restart. Test Stop/Mute commands in both languages.
  Provider output text must never execute device commands or change modes itself.
- Verify partial/final input/output captions are readable, bounded and cleared on
  Stop. No persistent chat/history claim should appear.

Phase 1 acceptance remains pending until relevant real provider and phone checks
pass. Phases 2–7 remain in docs/FEATURE_BACKLOG.json; do not mark them complete.
