# Jerry AI Original — feature status

Owner: Tanim Boss. App version: 0.2.0-confirmed-actions. Updated: 8 November 2026 (feature/jerry-local-smart-memory-v1 draft only).
Branch: `feature/jerry-original-phase1`. The branch name does **not** mean Gemini
Live Phase 1 is complete. This publication preserves the foundation and confirmed
Android-command implementation only.

**Local verification:** clean build passed, 35 JVM tests passed (0 failed/errors/
skipped), lint reported 0 issues, and both APKs compiled. See
docs/PUBLICATION_VERIFICATION.md for the packaged checks and evidence limits.
**Device evidence:** none. Four instrumentation scenarios are compiled, not run.
**GitHub Actions:** configured for this feature branch, pull requests and manual
runs; use the specific run's result/artifact as evidence, not this configuration.
**Production release:** none. Artifacts are development-signed debug APKs.

## Publication attempt

The feature branch and reviewed source are committed **locally**. The authorized
Git push was rejected with HTTP 403 using both the existing Git credential route
and the existing GitHub CLI credential helper. Remote main remains
`f38fccd02892223bd2b3c3df8b5ace06722c3b69`; no remote feature branch was created.
GitHub API access is also denied by the environment network policy. Consequently,
a Draft PR, hosted Actions run, uploaded Actions artifact and GitHub artifact
download URL are **not created or verified**. See docs/PUBLICATION_HANDOFF.md.
This status must not be interpreted as a successful remote publication.

## Initial 24-group inventory

Historical completeness is unverified; the supplied inventory is an initial
baseline. Nine groups have partial code, fifteen are not implemented. No complete
feature group or Android-phone compatibility is claimed.

| ID | Feature | Implementation | Exact boundary / missing work |
| --- | --- | --- | --- |
| 1 | Gemini Live Voice Assistant | Not implemented | Planned; no working implementation or runtime evidence. |
| 2 | Real-time Audio Streaming | Not implemented | Planned; no working implementation or runtime evidence. |
| 3 | Voice Interruption and Recovery | Not implemented | Planned; no working implementation or runtime evidence. |
| 4 | Offline Wake Word Detection | Not implemented | Planned; no working implementation or runtime evidence. |
| 5 | Owner Voice Recognition | Not implemented | Planned; no working implementation or runtime evidence. |
| 6 | Background Voice Service | Not implemented | Planned; no working implementation or runtime evidence. |
| 7 | Multilingual Voice and Personas | Not implemented | Planned; no working implementation or runtime evidence. |
| 8 | Text Chat and Conversation History | Not implemented | Planned; no working implementation or runtime evidence. |
| 9 | WhatsApp Contact and Messaging Assistant | Not implemented | Planned; no working implementation or runtime evidence. |
| 10 | YouTube Search and Playback Controls | Not implemented | Planned; no working implementation or runtime evidence. |
| 11 | Contact Search and Calling | Partial; device unverified | Confirmed Android dialer intent only; no contact lookup, direct call, or call-completion verification. |
| 12 | Android Phone Controls | Partial; device unverified | Confirmed Home intent and optional accessibility Back; broader phone controls are not implemented. |
| 13 | Application Launcher | Partial; device unverified | Launch selected installed apps using Android launcher intents; foreground verification requires opted-in accessibility. Device testing pending. |
| 14 | Web Search and Weather | Not implemented | Planned; no working implementation or runtime evidence. |
| 15 | Accessibility Voice Automation | Partial; device unverified | Foreground Android speech command input, confirmation gates, and optional selected-app tap/swipe/scroll/text automation. Search fallback fills a search field only. Device testing pending. |
| 16 | Screen Observation and Screenshot | Partial; device unverified | Ephemeral accessible node inspection for confirmed commands only. Screenshots and general screen observation are not implemented. |
| 17 | Multi-Step Task Automation | Not implemented | Planned; no working implementation or runtime evidence. |
| 18 | Spotify and Instagram Integration | Not implemented | Planned; no working implementation or runtime evidence. |
| 19 | Smart Memory and Personal Learning | Partial; branch only, device unverified | Opt-in local note save, ranked Bengali/English text search; no AI semantic learning, cloud sync or voice integration. |
| 20 | Memory Backup and Restore | Partial; branch only, device unverified | Explicit Android document export and validated merge-only import; no cloud backup or replacement of old notes. |
| 21 | Settings and UI Customization | Partial; device unverified | Theme preferences and original UI foundation only; broader customization is planned. |
| 22 | Android Permission Management | Partial; device unverified | Read-only permission diagnostics and settings links only; feature-specific permission flows are planned. |
| 23 | Security and Privacy Controls | Partial; device unverified | Confirmation gates, opt-in service/session, protected-screen and password checks; no network or sensitive runtime permissions. Device testing and key storage remain outstanding. |
| 24 | Diagnostics and Error Recovery | Partial; device unverified | Action rejection/timeout/cancellation and observed-vs-unverified outcomes; voice-session recovery is not implemented. |

## Implemented command path

Original six-screen UI and themes, diagnostics, typed/external foreground speech
input, bounded command grammar, immutable single-use 30-second confirmations,
official Android intents before optional accessibility fallback, manually enabled
service plus session opt-in, exact-label tap, Back, bounded swipe/scroll, protected
text entry and search-field filling. Dial/call opens the dialer; message opens an
SMS composer. Jerry does not directly call or send. UI changes and accepted
intents do not prove external completion. Search fallback does not submit.

## Required Android testing

Install/launch, rendering, lifecycle/theme persistence, recognizer availability,
service enablement/disconnect and consent reset, target-app version compatibility,
lock/security/password rejection, gesture dispatch, field changes, ambiguity,
timeout/cancel/scope changes and OEM backup behavior are all **unverified**.
See docs/DEVICE_TESTING.md and docs/ACTIONS.md. JVM tests exercise parsing, safety
policy, routing and storage; they do not execute Android APIs or gestures.

Gemini Live, background voice, streaming, wake word, owner recognition, personas,
chat/history, dedicated WhatsApp/YouTube/Spotify/Instagram adapters, weather,
contacts, screenshots, multi-step tasks, advanced/AI smart memory and cloud backup/restore remain missing. A small local note store and manual export/import exist only on this unmerged feature branch; Android phone behavior is unverified. Original contributions use the Tanim Boss proprietary license;
third-party licenses/notices remain intact. See docs/SOURCE_REVIEW.md.
