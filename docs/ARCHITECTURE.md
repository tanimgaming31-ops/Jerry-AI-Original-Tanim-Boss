# Independent architecture — confirmed actions

One Kotlin/Compose application module provides Home, Features, Settings,
Diagnostics, About, and Actions screens. MainActivity owns the UI entry point.
Layouts, colors, icon, action code and documentation are original contributions;
third-party AndroidX/Kotlin/Gradle materials retain their own licenses.

## Current data flow and boundaries

| Component | Responsibility |
| --- | --- |
| JerryViewModel / settings | Immutable settings UI state and theme persistence through Preferences DataStore; unknown-value fallback, corruption recovery and generic IO-error messages |
| catalog | Stable IDs for the initial 24 feature groups; partial implementation and absent device evidence are explicit |
| diagnostics | Manifest/runtime permission states, app settings links, service connection/session status; no automatic permission requests |
| ActionsViewModel / ActionsScreen | Memory-only command input, selected target, disclosure, external Android speech result, preview/confirmation and outcome display |
| CommandParser | Bounded English command grammar and ordinary-number validation; no arbitrary task/code interpretation |
| ConfirmationGate | Immutable action+target, one pending preview, UUID capability, 30-second monotonic expiry, cancellation and single consumption |
| ActionCoordinator | Consume confirmation → official Android adapter → fallback only if unsupported and session allowed; explicit denial does not fall back |
| AndroidActions | Launchable-app enumeration via scoped package queries, keyguard/protected-target checks, launcher/search/Home/dialer/SMS-composer intents |
| JerryAccessibilityService | Android-bound opt-in service; serialize one confirmed action; selected-package checks, bounded node lookup, gestures/text and result observation |
| EditorPolicy | Password flags/input variations rejected; conservative English search-field semantics |

Voice recognition uses an external RecognizerIntent activity. Its result only fills
the command field; it never executes automatically. Gemini Live, audio streaming,
wake word, background microphone, general chat/history, smart memory, screenshot
capture, app-specific adapters and multi-step orchestration remain future work.
Future voice clients should pass requests through the same confirmation/coordinator
boundary, not call service actions directly.

## State and lifecycle

Theme preferences persist in DataStore. Commands, phone numbers, messages,
selected target, previews, screen nodes and outcomes are not saved to disk or
SavedStateHandle. Page selection uses Compose saved state. Theme corruption resets
to defaults; ordinary storage failures produce UI errors rather than false saves.

The service connection and process-local automation consent are separate state
flows. Connection starts disarmed; consent resets on disconnection, interruption,
Stop, viewmodel clearing or process restart. Event callbacks do not record content.
Service actions inspect only accessible nodes transiently during a confirmed task.
Waits are bounded, and a mutex prevents concurrent accessibility dispatch. Stopping
cancels waiting and prevents further steps but cannot reverse already accepted OS
operations. No automatic action retry exists.

## Permission and security boundaries

No INTERNET, RECORD_AUDIO, READ_CONTACTS, CALL_PHONE, SEND_SMS,
QUERY_ALL_PACKAGES or foreground microphone permission is requested. App visibility
is limited to launcher/search/settings intents. The accessibility service is
exported behind BIND_ACCESSIBILITY_SERVICE, which only Android can grant to a
binder. No external command endpoint is exported. The service is not represented
as a disability accessibility tool on API 31+, and OS enablement stays manual.

All commands require preview/confirmation, including potentially consequential
taps, text replacement and gestures. Text input rejects password flags and password
input variations. Jerry/settings/recognized permission, installer and security
packages are excluded. Keyguard, foreground package and consent are rechecked.
Unavailable or protected screen content is not worked around. No screenshot,
clipboard, root, shell, permission-grant or lockscreen bypass API is used.

Android nodes/gestures do not provide transactional screen ownership. The screen
can change after a preflight check; verification reports observed UI changes or
uncertainty, not guaranteed external effects. See ACTIONS.md for precise result
semantics and supported/unsupported operations.

Privacy disclosure explains that external recognizers and target apps have their
own data processing policies. Jerry logs no commands/audio/screen contents and
stores no API keys. Backups are disabled and storage-domain transfer exclusions
remain explicit. The debug APK is a development build, not a hardened release.
Future Gemini keys require Keystore-backed protection and exclusion from backups,
source, build artifacts, logs and exports; no provider credential is embedded now.

## 0.3.0 Phase 1 increment

The confirmed-action architecture above remains intact. New foreground voice and
secure voice preferences are documented in VOICE_ARCHITECTURE.md. Internet,
RECORD_AUDIO and MODIFY_AUDIO_SETTINGS are now required for explicit foreground
Gemini sessions; the earlier no-microphone/network description applies to 0.2.0
only. No FGS/background microphone, wake word or automatic AI device tool was added.
