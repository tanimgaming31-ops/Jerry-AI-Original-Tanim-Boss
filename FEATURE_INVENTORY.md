# Jerry AI Original — approved 54-group inventory

Source: owner master specification. This supersedes the initial 24-group numbering;
that earlier inventory is retained at docs/FEATURE_INVENTORY.md. No persisted user
data used catalog IDs, so the catalog change performs no data migration/deletion.
The structured backlog is docs/FEATURE_BACKLOG.json; no group is silently omitted.

| ID | Feature | Phase | Current status | Actual boundary |
| --- | --- | --- | --- | --- |
| 01 | Gemini Live Voice Assistant | 1 | IMPLEMENTED_DEVICE_UNVERIFIED | Original authenticated v1beta Live WebSocket, model discovery, setup, sanitized failures and memory-only resumption. Provider/key/model and physical-phone acceptance remain unverified. |
| 02 | Real-Time Bidirectional Audio | 1 | IMPLEMENTED_DEVICE_UNVERIFIED | Foreground PCM16 microphone capture, 16 kHz stream/resampling, 24 kHz playback, bounded buffers, focus and optional AEC. Hardware/network audio quality remains unverified. |
| 03 | Voice Interruptions and Barge-In | 1 | IMPLEMENTED_DEVICE_UNVERIFIED | Local activity detection, immediate playback invalidation, manual activity/barge-in signals and stale callback guards. Real Gemini cancellation/echo behavior needs phone testing. |
| 04 | Offline Wake Word | 2 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 05 | Foreground Voice Service | 2 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 06 | Background Voice Assistant | 2 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 07 | Offline Voice Commands | 2 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 08 | Advanced Voice Session Management | 1 | IMPLEMENTED_DEVICE_UNVERIFIED | Serialized session states, mute/stop, bounded retries, setup timeout, focus-loss shutdown and foreground lifecycle cleanup. OS behavior remains unverified. |
| 09 | Multilingual Voice Understanding | 1 | PARTIAL | Persistent language requests for the listed language families and provider input/output captions. Full language recognition/output coverage is model-dependent and has not been verified. |
| 10 | AI Voice and Personality Modes | 1 | IMPLEMENTED_DEVICE_UNVERIFIED | Five provider voice names, six persona modes, saved preferences and exact English/Bengali input-transcript mode switching. Provider voice availability and actual persona output remain unverified. |
| 11 | Advanced AI Text Chat | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 12 | Persistent Chat History | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 13 | Smart Memory Bank | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 14 | Personal Learning Engine | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 15 | Memory Backup and Restore | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 16 | Conversation Summaries | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 17 | Personal Knowledge Search | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 18 | AI Planning and Decision Assistant | 3 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 19 | Universal App Launcher | 4 | PARTIAL | Selected launchable app intents and legacy foreground command input retained. Universal spoken-name resolution and all app/profile/clone cases remain pending. |
| 20 | Advanced Phone Controls | 4 | PARTIAL | Confirmed Home intent and optional accessibility Back retained. Flashlight, brightness, media/volume and broader controls remain pending. |
| 21 | Contact Search and Calling | 4 | PARTIAL | Confirmed dialer intent retained; contact lookup, ambiguity resolution and actual calling are not implemented. |
| 22 | WhatsApp Voice Assistant | 4 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 23 | IMO and Messenger Assistant | 4 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 24 | YouTube Search and Playback | 4 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 25 | Spotify and Media Assistant | 4 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 26 | Notification Reader and Reply Manager | 4 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 27 | Screenshot and Screen Capture | 4 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 28 | Camera and Smart Vision | 4 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 29 | Accessibility Voice Control | 5 | PARTIAL | Existing optional user-enabled selected-app tap/swipe/scroll/text and navigation retained, with confirmation and observed/unverified outcomes. Layout compatibility remains unverified. |
| 30 | Multi-Step App Macros | 5 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 31 | Smart Task Execution Engine | 5 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 32 | Voice Command Learning | 5 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 33 | Smart Clipboard Assistant | 5 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 34 | Advanced File Manager | 5 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 35 | Smart Routines | 5 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 36 | Alarms and Reminders | 5 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 37 | Screen Understanding Assistant | 6 | PARTIAL | Only bounded transient accessibility-node checks for confirmed actions. No general screen summarizer or screenshot pipeline. |
| 38 | PDF and Document Reader | 6 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 39 | QR and Barcode Scanner | 6 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 40 | Receipt Scanner and Expense Tracker | 6 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 41 | Real-Time Voice Captions | 6 | PARTIAL | Live input/output transcription captions in the foreground session only. No continuous independent caption utility; recognition needs provider/phone tests. |
| 42 | Live Translator and Interpreter | 6 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 43 | Weather, News and Information | 6 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 44 | Interactive Learning Assistant | 6 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 45 | Original Animated Home Interface | 7 | PARTIAL | Original existing Compose home preserved and Voice screen added. Futuristic orb/waveform/transitions are planned. |
| 46 | Avatar Customization | 7 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 47 | Floating Jerry Shortcut | 7 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 48 | Advanced Settings Dashboard | 7 | PARTIAL | Existing themes/settings plus Voice model/key/language/persona configuration. Memory, notifications, security and backup dashboards expand in later phases. |
| 49 | Permission Diagnostics Dashboard | 7 | PARTIAL | Real declared/runtime microphone state, other permission states and accessibility-session diagnostics. Camera/capture/notification feature flows remain pending. |
| 50 | AI Self-Diagnostics and Recovery | 7 | PARTIAL | Sanitized Live network/auth/model/quota failures, retry states, microphone/focus handling and existing command diagnostics. Structured diagnostic export/self-repair remains pending. |
| 51 | Security and Owner Protection | 7 | PARTIAL | Keystore-backed API-key encryption, explicit microphone start, scoped confirmations/password checks and no automatic AI device tools. Owner biometrics and broader privacy alerts remain pending. |
| 52 | Backup, Restore and Data Migration | 7 | PLANNED | Not implemented. Retained in the structured backlog; no working or device-verification claim. |
| 53 | About Me, Ownership and Legal Notices | 7 | IMPLEMENTED_DEVICE_UNVERIFIED | Original ownership/About UI, correct app identity, proprietary original-material notice and preserved third-party licenses. Phone rendering still unverified. |
| 54 | Complete Feature Status, Testing and Updates | 7 | PARTIAL | All 54 groups and six truthful status categories, structured backlog, version/change documentation, local tests/build evidence and workflow. Device verification and updater are pending. |
