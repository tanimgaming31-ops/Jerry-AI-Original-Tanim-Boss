# Phase 1 voice architecture and acceptance boundaries

## Source baseline and independence

Baseline: feature/jerry-original-phase1 at
72743c4d405f9ae7d445c78c3c3017c074f2171c, successful public Actions run
https://github.com/tanimgaming31-ops/Jerry-AI-Original-Tanim-Boss/actions/runs/42258390538.
The main-branch merge run 42259524253 also showed success but is not a new voice
build. Local f1777b7 and remote baseline source backups were made before changes.
Development occurs only on feature/jerry-original-full-features; main and old
Jerry/Maya/Zoya/AYRA/keyboard repositories are not modified.

Only the old Jerry README was consulted for functional context; no old
implementation, protected UI assets, model graphics or Tech GPTx source was
imported. The original Kotlin wire client was written against public Google API
field contracts, cross-checked with official googleapis/python-genai API types /
_live_converters.py / live.py. No SDK implementation code was copied or bundled.

## Boundaries

- VoiceTypes defines provider-neutral configuration/state/events and audio/socket
  ports. No Android action-execution tool is registered in Gemini setup.
- LiveProtocol constructs/strictly parses setup/activity/PCM/transcription/resumption
  messages; malformed or wrong-rate audio is rejected. Input is PCM16 LE mono
  16 kHz; output is PCM16 LE mono 24 kHz. Empty/odd/oversized frames are rejected.
- GeminiLiveClient uses OkHttp TLS WebSocket on the fixed Google host with the
  x-goog-api-key header, no key URL, logging interceptor or redirect. Separate
  explicit HTTPS discovery filters advertised bidiGenerateContent models. No
  model ID is assumed available; the user chooses one and tests provider support.
- LiveSession serializes lifecycle/events, owns one connection/microphone, bounded
  mic/event queues, a 100 ms preroll, local energy speech-activity gating and
  manual activity start/end with START_OF_ACTIVITY_INTERRUPTS. This is not wake
  word or owner recognition. It flushes audio immediately and coordinates actual
  response cancellation with Gemini; real interruption/echo behavior needs testing.
- AndroidVoiceAudio owns AudioRecord/AudioTrack, focus and optional device AEC/noise
  suppression. It prefers 16 kHz, falls back to supported 48/44.1 kHz capture and
  uses streaming area-average resampling. Hardware and routing are unverified;
  this is not a claim of studio-quality resampling or universal AEC support.
- Bounded epoch-tagged playback invalidates old data on interrupt; playback-head
  observation distinguishes queued hardware audio from drained output. Backpressure
  stops the session rather than silently accumulating or dropping response audio.
- VoiceSettings uses a separate DataStore file, preserving theme data. VoiceKeyVault
  stores AES-GCM ciphertext in noBackupFilesDir; the AES key stays in Keystore.
  Atomic writes preserve the old credential on save failure. Integrity/read failures
  do not erase data; a missing encryption key does not create a replacement on read.
- VoiceViewModel/VoiceScreen provide explicit Start/permission/configuration,
  model discovery, mute/interrupt/stop, persona/language and temporary captions.
  Key entry is masked and not saved-state-backed. Key replacement/removal stops
  the session. MainActivity stops voice on background, retaining configuration
  rotation only; no microphone foreground service exists until Phase 2.

## Recovery and security

Ready is required before capture. Duplicate Ready cannot start a second microphone.
Connection/capture generations invalidate late callbacks; the focus owner is
separate from reconnect generations. Mute invalidates capture frames and suppresses
old output after unmute until fresh input. Stop invalidates timers and never
restarts itself. Focus loss/auth/config/mic/playback failure stops with sanitized
errors. Network/setup/GoAway reconnects pause the mic, clear playback and make at
most three retries (1/2/4 seconds); captured audio is never replayed. A provider
resume handle remains in memory and does not guarantee context restoration.

Only exact input-transcript Stop/Mute/persona commands are interpreted. A final
transcript or turn completion can finalize them; assistant output cannot do so.
Persona changes persist and restart a fresh Live context (no silent conversation
history replay). Language is requested in the system instruction because native
multilingual models can have model-specific language settings/limitations.
All listed languages/voices need actual per-model verification. No AI-generated
output bypasses the existing Android confirmation gates.

OkHttp 5.5.1's Android AAR requires compileSdk 37, incompatible with the retained
SDK 36 / AGP 8.13.2 baseline. Version 5.3.2 matches SDK/Kotlin compatibility.
Upgrade recommendations stay visible as lint warnings; API, permission, security
checks and meaningful test assertions are retained. All third-party notices remain
intact. No key or private transcript is included in source, CI or artifacts.

## What tests prove

JVM tests cover schema/PCM/VAD/resampling/buffer semantics, all old command/security/
storage tests, new preference persistence and session orchestration under controlled
fake ports and virtual time. MockWebServer exercises real localhost WebSocket
handshake, API-key header placement, setup, PCM frames, interruption and HTTP denial.
These are not Gemini service, Google TLS, Android audio or account-availability tests.
Six instrumentation scenarios compile, including isolated Keystore/tamper checks;
none ran in cloud. TEST_CHECKLIST.md defines the required real-phone acceptance.

The response-progress deadline is 45 seconds; no-progress response generation fails
and releases audio instead of leaving Thinking indefinitely. It resets on actual
response progress and cancels on a new input/interruption/Stop. Some models omit
transcript.finished; a completed turn can finalize only exact safe input controls,
never assistant output or an actively ongoing new speech activity. A server update
that marks resumption unavailable clears the old handle.
