# Jerry AI Original

**Assistant:** Jerry · **Original owner/publisher:** Tanim Boss
**Application ID:** `com.tanimboss.jerryai` · Android 10+ · Kotlin/Compose

Version **0.3.0-live-voice** adds the Phase 1 source implementation to the existing
confirmed-command app. **Real Gemini service and Android-phone acceptance remain
unverified.** Compilation and mock tests are not proof of an actual conversation.
All 54 approved groups are tracked in [FEATURE_INVENTORY.md](FEATURE_INVENTORY.md),
[FEATURE_STATUS.md](FEATURE_STATUS.md), and [the structured backlog](docs/FEATURE_BACKLOG.json).
Wake word “Hey Jerry”, microphone FGS, background/offline voice, Room memories and
remaining phases are planned, not claimed working.

## Foreground Gemini Live

Open **Voice**, enter your own Gemini API key, choose a currently supported Live
model ID (or explicitly discover advertised models), save preferences and press
**Start foreground voice**. Permission and successful Gemini setup are required
before capture. Model, voice, language and free quota availability are controlled
by Google; check your account and do not assume usage is free. There is no automatic
paid-model fallback or embedded key. Keys use Android Keystore-backed AES-GCM in
noBackupFilesDir; raw audio/captions and resumption handles stay in memory.

The original client streams PCM16 mono at 16 kHz and plays 24 kHz responses. Local
activity detection and manual interruption clear old playback and signal Gemini.
Mute stops capture; Stop releases audio/socket; leaving the app stops the session.
Retry is bounded and captured audio is never replayed. Focus-loss/permission/model/
quota/network failures are explicit. See [voice architecture](docs/VOICE_ARCHITECTURE.md)
and [privacy](PRIVACY.md) for boundaries, transient data and model-dependent behavior.

Five provider voice names and six persona modes are offered. Listed language
families are requested in the system instruction, not universally guaranteed.
Exact input-transcript controls include “Jerry, switch to friendly mode”,
“জেরি, বন্ধুত্বপূর্ণ মোড চালু করো”, “Jerry, stop listening”, and
“জেরি শোনা বন্ধ করো”. They require provider input transcripts/turn boundaries;
use the Stop button when offline. Persona changes restart a fresh Live context.
Gemini has **no device tools** and cannot bypass action confirmations.

## Preserved Android commands

Open **Actions**, choose a target, type or invoke Android's external speech
recognition, preview and confirm every action. Recognizer output never auto-executes.

- `open app`, `home`, `back`, `search music`, `tap Search`, `type Hello`
- `swipe up/down/left/right`, `scroll up/down/left/right`
- `dial +15551234567` / `call ...` opens the dialer; Jerry does not directly call.
- `message +15551234567: Hello` opens an SMS composer; Jerry does not directly send.

Official APIs/intents take priority. Optional fallback requires manually enabling
Jerry in Android Accessibility settings plus separate session opt-in. No permission
or lockscreen bypass is attempted. Search fallback only fills an eligible search
field; the user submits it. Exact/unique selectors and bounded verification reject
ambiguous or inaccessible screens. An observed UI change is not message delivery
or other external completion. [Supported actions and limits](docs/ACTIONS.md).
Existing themes, confirmation/safety gates, diagnostics and data are retained.

## Build and tests

Requires Java 17, Android SDK API 36, Build Tools 36.0.0 and platform-tools.

```bash
# Managed Linux cloud only; Android Studio users configure their own JDK/SDK.
./scripts/setup-cloud.sh
source scripts/cloud-env.sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
python3 scripts/verify-foundation-apk.py
```

Main APK: `app/build/outputs/apk/debug/app-debug.apk` (development signing only).
Tests/HTML/lint reports: `app/build/reports/`. Latest verification:
[docs/PHASE1_VERIFICATION.md](docs/PHASE1_VERIFICATION.md). Instrumentation compilation
is not execution. Run `./gradlew connectedDebugAndroidTest` only with an authorized
physical phone; follow [TEST_CHECKLIST.md](TEST_CHECKLIST.md) and
[device testing](docs/DEVICE_TESTING.md). Never put real credentials in tests/CI.

The existing workflow builds this development branch and PRs, runs tests/lint,
checks packaged identity/permissions/licenses, and uploads the debug APK with a
checksum, status and test reports. Use the **actual new run** artifact URL; successful
baseline run 42258390538 is not evidence of the new voice implementation. Manual
dispatch may depend on default-branch workflow availability. No main merge or
production release is automated.

## Ownership and independent development

Original code, UI and assets are independently developed for Tanim Boss under
[LICENSE](LICENSE). [Third-party notices](THIRD_PARTY_NOTICES.md) and full license
texts remain intact. The old Jerry README was read only for functionality; no old
Tech GPTx/Jerry implementation or protected assets were copied. Maya, Zoya, AYRA
and Maya AI Keyboard are untouched. Current work uses the existing checkout and
feature/jerry-original-full-features; no extra Git worktree is needed.

[Architecture](docs/ARCHITECTURE.md) · [Seven-phase plan](docs/DEVELOPMENT_PLAN.md) ·
[Changelog](CHANGELOG.md) · [Privacy](PRIVACY.md) · [Historical reports](docs/VALIDATION.md)
