# Jerry AI Original

**App:** Jerry AI Assistant · **Original developer, owner, publisher:** Tanim Boss

Independent Kotlin/Compose Android project, now at **0.2.0 — confirmed actions**.
Original UI, persistent themes, feature tracking, diagnostics, and an optional
user-enabled command/accessibility layer. Android-phone behavior is **not tested**.
Gemini Live, wake word, background listening, chat/history and app-specific
integrations remain future work. In the unmerged local-memory feature branch only,
users can manually save/search notes and export/merge backups. This is not AI learning,
cloud memory or a device-tested feature.

## Local memory in the unmerged feature branch

Use the new **Memory** tab to save a 1–700 character note, find it by Bengali/English text,
or explicitly export a plain-text backup with the Android document picker. Import checks
its format, size and entries before a confirmation, then merges only missing notes.
The existing themes, Actions confirmation gates and other screens are unchanged.

The memory is **not encrypted** and the export format uses Base64 only for portability,
not secrecy. Do not save API keys, passwords or OTPs. Android phone testing is pending.

## Supported commands

Open **Actions**, choose a target app, and type a command or use **Speak command**.
Speech recognition is delegated to the phone's speech app and may process audio
online under that app's policies; it only fills the command field. Preview and
confirm every action. A confirmation is single-use and expires after 30 seconds.

- `open app`, `home`, `back`
- `search music`, `tap Search`, `type Hello`
- `swipe up/down/left/right`, `scroll up/down/left/right`
- `dial +15551234567` (or `call ...`) opens the dialer; **Jerry does not place a call**.
- `message +15551234567: Hello` opens an SMS composer; **Jerry does not send it**.

Android launcher/search/dialer/composer/Home intents take priority. For optional
Back/tap/swipe/scroll/text/search-field fallback, manually enable **Jerry confirmed
commands** in Android Accessibility settings, read its disclosure, then turn on
**Allow automation this session** in Jerry. Service enablement alone does not arm
it. Android restrictions are never bypassed. Consent resets on disconnection,
process restart, or Stop. Screenshots and continuous observation are not enabled.

Accessibility actions launch the selected app, wait for its accessible foreground
window, and perform one confirmed action. Taps need an exact, unique accessible
label. Text entry replaces a focused or unique editable field, excluding password
fields. Search fallback fills an eligible search field but does not submit it.
Ambiguous, inaccessible, protected, rejected, or timed-out actions report failures
or an unverified result. Gestures and UI changes do not prove message delivery or
other external effects. Do not retry an unverified sensitive action blindly.

See [command behavior and limits](docs/ACTIONS.md) before using automation.

## Build

Requires Java 17, Android SDK platform 36, Build Tools 36.0.0, and platform-tools.
On a configured Android development machine:

```bash
# Set JAVA_HOME to your JDK 17 and ANDROID_HOME to your SDK first.
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

On this managed Linux x86_64 cloud machine:

```bash
./scripts/setup-cloud.sh
source scripts/cloud-env.sh
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

The cloud bootstrap installs pinned, checksum-verified tools outside the checkout,
accepts SDK development terms, and preserves proxy/TLS verification. It requires
curl, unzip, Python 3, and dpkg-deb. It never writes credentials into the project.
For local Android Studio use, open this directory and choose JDK 17; configure the
SDK through Android Studio or an ignored `local.properties`.

Verify the packaged permission, service-binding and license contract with
`python3 scripts/verify-foundation-apk.py` after building.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
This APK is development-signed, not a release distribution. Keep the original
checkout in the isolated cloud task; no additional Git worktree is needed.

## Android-phone-first verification

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tanimboss.jerryai/.MainActivity
./gradlew connectedDebugAndroidTest
```

Only run the connected test command with an authorized Android device/emulator.
Do not interpret compiling the test APK as executing its tests. Follow the
[device checklist](docs/DEVICE_TESTING.md), including cold launch, denial handling,
settings links, and process recreation. Device/hosted-CI results are recorded in
[validation](docs/VALIDATION.md), without claiming unexecuted checks pass.

## Feature-branch APK builds

The `Android confirmed actions` workflow runs on this feature branch, pull requests
and manual dispatch. After a successful run, open its summary and choose
**Download debug APK artifact**. The artifact contains the debug APK, SHA256SUMS.txt,
and FEATURE_STATUS.md and is retained for 30 days. GitHub sign-in and repository
access may be required. This is not a production release or a device-test result.

## Project documents

- [Architecture and security boundaries](docs/ARCHITECTURE.md)
- [Current feature status](FEATURE_STATUS.md) and [initial inventory](docs/FEATURE_INVENTORY.md)
- [Development phases and acceptance gates](docs/DEVELOPMENT_PLAN.md)
- [Validation evidence](docs/VALIDATION.md)
- [Original-material license](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md)

GitHub Actions runs unit tests, Android lint, APK assembly, and instrumentation
APK compilation, then uploads the debug APK and reports. It does not execute
device tests. No remote workflow run is claimed before an approved push.

The proprietary license applies only to legally owned original contributions.
Third-party dependencies and the Gradle wrapper retain their own licenses.
No old Jerry, Maya, Zoya, or Tech GPTx project source or protected assets were used.
