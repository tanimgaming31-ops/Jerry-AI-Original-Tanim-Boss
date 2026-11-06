# Pink–gold Jerry Home Screen (design preview)

This original Jetpack Compose implementation is based on the user's supplied two-phone visual reference.
It **does not** embed the reference screenshot as a fake app interface, copy external artwork, or pretend planned features work.

## Actual implementation

- Native dark aubergine/black, pink-and-gold gradients, rounded neon panels, animated original glowing orb and sound-wave decoration.
- Branding: Jerry AI Original; Tanim Boss Owner & Publisher.
- Live status comes from `VoiceViewModel.state` through `HomeLiveStatus.kt`. It never reports "Connected" or "Running" while idle/connecting/error.
- Gemini Live Voice opens the existing foreground-only Voice screen with real model/key settings.
- App-opening shortcuts navigate to the existing confirmation-based Actions screen.
- Chat and Memory open honest planned-feature information pages; remaining unimplemented quick-action buttons open the Feature Inventory.
- Wake word and Foreground Voice Service are visibly marked "Not implemented"; no microphone, background service or screen capture is started by displaying Home.
- Five bottom tabs: Home, Chat, Memory, Tools and Profile; Settings and Diagnostics are available at the top.

## Accessibility and device testing still required

The screenshot is a visual design goal, **not device evidence**. Compare rendering on a narrow Android phone, long labels, large font scale, dark/light preference, TalkBack and touch targets.
The wave is decorative, not claimed to be microphone amplitude. Verify navigation and state changes on an actual Android device.
Gemini Live network/provider behavior, wake word and background voice are not validated by UI build checks.
The old Jerry, Maya and Zoya repositories are untouched, and no unrelated data model or package ID is changed.

## Build instructions

Run the existing GitHub Actions workflow for `feature/jerry-pink-gold-home`.
The workflow runs JVM unit tests, lint, APK assembly, instrumentation APK compilation and packaged security checks.
A green result means source compiled and checks passed, **not that the Android UI and provider services were exercised on a real phone**.
