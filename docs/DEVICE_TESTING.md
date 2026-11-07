# Android device acceptance checklist

**Status: NOT RUN. No Android device/emulator is attached.**

Record phone model, Android version/API, OEM build, APK SHA-256, date, tester,
actual results and limitations. Use an authorized Android 10+ phone. API 29 and a
recent API 36 device provide useful coverage; one phone does not establish all
version/OEM compatibility.

Build and install:

```bash
source scripts/cloud-env.sh # only on the configured managed cloud machine
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tanimboss.jerryai/.MainActivity
./gradlew connectedDebugAndroidTest
```

Do not run connected tests without an authorized device/emulator. Compilation is
not execution. Keep credentials, phone numbers, messages, audio and screen content
out of shared logs/screenshots.

## Basic application checks — NOT RUN

- Verify app icon/name, ownership/license screens, all seven navigation destinations,
  large fonts, narrow/landscape layout, scrolling, TalkBack and system bars.
- Test theme persistence after rotation, activity recreation, force stop and process
  relaunch. JVM storage tests do not establish on-device behavior.
- Verify permission state labels, app/accessibility settings links, missing-settings
  failure handling and refresh after return. No direct sensitive runtime permissions
  should be requested, including on first launch.
- Read bundled complete license texts offline. Validate backup/transfer exclusions
  on an appropriate controlled setup; OEM behavior remains unverified.

## Confirmed-actions increment — all items NOT RUN

Version 0.2.0 declares an optional accessibility service and supports confirmed
actions; there is no Jerry microphone or network permission. The instrumentation
APK contains **four** scenarios: six-screen navigation, theme/activity recreation,
no sensitive runtime permissions, and cancelling a command preview.

Record phone model/API, app version, target-app version, service/session state,
command, observed outcome and limitations. Use harmless test data and a benign
test target, not personal messages or paid/destructive actions.

1. Service off: confirm launcher/Home/dialer/composer requests use official handlers;
   confirm unsupported UI commands fail and no command runs on recognition alone.
2. Enable the service manually after reading disclosure. Confirm its session starts
   off. If Android restricts enablement, report that restriction; do not bypass it.
3. Opt in for the session. In a benign target, test Back, unique-label tap, each swipe/
   scroll direction, focused and unique field entry, and recognized search field
   filling. Verify entered text and no submit action. Search intents should precede
   fallback when supported by that app. Record unsupported custom-rendered layouts.
4. Verify tap ambiguity, multiple editable fields, no scroll target, missing app,
   rejected gesture, target switch, loading/splash screen and unavailable content
   produce failure/unverified outcomes rather than fabricated success.
5. Cancel, replace and expire previews; attempt no confirmation. No app action should
   occur. Check recipient/text preview before using only controlled SMS/dialer tests;
   Jerry must not place calls or send messages directly.
6. Use a harmless button labelled Send/Call in a test app to verify that generic
   sensitive-looking taps cannot execute without confirmation. Never use a real
   transaction to test this safety gate.
7. Verify password, visible-password, web-password and numeric-PIN fields reject
   text entry. Confirm settings/permission/installer/security and Jerry targets are
   excluded, and locked phones cannot execute commands.
8. Revoke consent or disconnect the service during waits, press Stop, rotate,
   recreate the activity, and kill/restart the process. Consent must reset as
   documented; no action should automatically resume or retry. Already accepted
   OS actions may finish; they cannot be rolled back.
9. Verify observed UI changes are distinguished from accepted-but-unverified
   requests. Keyboard/custom views and foreground changes must not cause false
   claims of message delivery, successful calls, or completed searches.
10. Verify speech recognition unavailable/cancelled/denied paths; external recognition
    can have network/microphone policies of its own. No audio should be captured
    or retained by Jerry, and speech results must still need confirmation.
11. Check screen-text/command retention, service metadata on API 29 versus 31+,
    large fonts, no unexpected exported command endpoint, and permission/service
    state refresh after returning from settings.

Compiling these paths and passing JVM policy tests does not establish their actual
behavior on Android. Update device evidence only after the corresponding checks run.

## Local Memory v1 acceptance (feature branch only) — NOT RUN

1. Open the new Memory page. Check the privacy warning and that notes begin empty on a fresh installation.
2. Add a Bengali memory and an English task; force-stop and restart Jerry. Both should persist.
3. Search each language; check exact and partial matches. Search must never call Gemini or ask for network permissions.
4. Export a backup using the Android document picker. Inspect the text header `JERRY_MEMORY_V1` and preserve this file privately. Base64 is **not encryption**.
5. Import the same backup and confirm: entry count must remain unchanged. Import a second valid backup and confirm that only new entries are merged.
6. Import an unrelated text file, malformed record and file over 256 KB. The app must reject them without modifying current notes.
7. Cancel import or export in the picker. No memory should change.
8. Delete one note using the confirmation dialog. All other notes must remain.
9. Test storage-unavailable/error handling: no success should be displayed for failed saves.
10. Test display at large font and on a narrow Android screen, plus old Actions, Settings and Themes.
11. Preserve a prior installation's app data and signing key. An uninstall/reinstall can erase all local memory.

No phone or emulator test has been run on this branch. Gradle compilation only checks that code can be built, not Android file pickers or persistence.
