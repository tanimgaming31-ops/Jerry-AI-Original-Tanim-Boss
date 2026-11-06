# Seven-phase owner-approved development plan

Canonical inventory: ../FEATURE_INVENTORY.md. Machine-readable backlog:
FEATURE_BACKLOG.json. Historical initial 24-group inventory remains in
FEATURE_INVENTORY.md in this directory; it is not the canonical 54-group numbering.
No persisted user data used those IDs; no user-memory migration was performed.

| Phase | Groups | Implementation/acceptance boundary |
| --- | --- | --- |
| 1 | 01,02,03,08,09,10 | Current increment: Live wire client, foreground audio, interruption, states, requested languages/voices/personas and secure configuration. Local/wire tests and build required; real provider and phone tests pending. Never mark complete based on compilation alone. |
| 2 | 04–07 | Independently select licensed offline Hey Jerry engine/model after redistribution/performance review; user-started microphone FGS with notification Stop/Mute; Android-limited background/recovery and local phone commands. Test offline, lock/screen-off, revocation, battery/OEM policies and no hidden recording. |
| 3 | 11–18 | Streamed text chat, Room history/memory, opt-in learning, encrypted backup/restore, summaries, selected-document retrieval and planning. Additive migrations must preserve all existing records, validate/rollback and obtain explicit user confirmation for destructive conflicts. |
| 4 | 19–28 | Spoken-name launcher, supported phone controls, contacts/ambiguity/dialer, app-specific WhatsApp/IMO/Messenger/YouTube/Spotify, notification access/replies, capture and camera. Test real handlers/layouts, clone/work-profile limits, recipient confirmation and observed outcomes. |
| 5 | 29–36 | Extend confirmed automation with bounded macros/task progress, only safe idempotent retries, alias learning without permission escalation, user-initiated clipboard/SAF files, routines and OS alarms/reminders. Test cancellation, partial failure, sensitive actions and OS scheduling limits. |
| 6 | 37–44 | Consented screen understanding, selected PDFs/docs, QR/barcodes, user-corrected receipts/expenses, captions, translation, sourced weather/news and learning. Verify actual file/camera reads, source references, retention and language/model support. |
| 7 | 45–54 | Original animated home/avatar assets/floating shortcut, complete settings and permission dashboard, structured diagnostics, owner/privacy controls, validated data migration/backup, ownership notices, version/update and honest feature statuses. No protected models/assets or covert surveillance. |

Every increment must preserve existing functionality, license notices and data,
add meaningful unit tests and device scenarios, run Gradle/lint/APK checks, record
actual executed versus unrun results, and publish only an authorized feature branch.
No main merge or production release without explicit approval. Phone-only runtime
testing is required for real provider/hardware behavior. Free tools do not imply
free/unlimited Gemini usage; no paid fallback/account changes are automated.

Unfinished requirements are never dropped. Current statuses distinguish source
implementation, partial work, plans, unsupported/failed behavior and real device
evidence. No entry is marked working/device-verified without actual device evidence.
