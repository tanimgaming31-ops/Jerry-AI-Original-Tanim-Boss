# Verified development phases

All phases use independent original code and assets. The first task delivers only
Phase 0. Historical completeness is unverified; never invent missing approved
specifications. Preserve IDs 1–24 and append new approved capabilities with their
specification provenance rather than silently renaming or dropping groups.

| Phase | Groups | Deliverable and acceptance gate |
| --- | --- | --- |
| 0 — Foundation | 21–24 (partial) | Original five-screen UI, theme persistence, read-only permission diagnostics, privacy/license information, catalog, JVM tests, lint and debug APK. Phone installation/navigation/process recreation remain a separate gate. |
| 1 — Foreground assistant | 1–3, 7–8 | Verify current Gemini Live API/model support; phone-entered key; real audio sessions, interruption/cancel/reconnect, language/persona configuration, text chat/history. Test transport failure states, audio focus, timeout, cancellation and on-phone turn taking. |
| 2 — Background voice | 4–6 | Choose an offline wake-word engine after checking license, model redistribution, device performance and training needs; explicit owner-voice enrollment; foreground microphone service and stop notification. Test airplane-mode wake word, false triggers, lock screen, denied/revoked permissions, interruptions, OEM battery restrictions and recovery without hidden recording. |
| 3 — Supported integrations | 9–14, 18 | Confirmed contact/calling/messaging actions; YouTube search/media controls; phone controls; launcher; web/weather; Spotify/Instagram adapters. Specify exact supported actions and versions first. Verify recipients and outcomes, unavailable apps, denied permissions, locked device, unavailable APIs and unsupported operations. Do not infer full control from app launch. |
| 4 — Assisted automation | 15–17 | Explicitly enabled accessibility service; consented screen observation/capture; bounded multi-step executor. Verify confirmation, cancellation, untrusted screen content, secure-window restrictions, layout/version changes and partial task failure. Never report an unobserved action as completed. |
| 5 — Memory | 19–20 | Inspectable/editable/deletable memory; explicit learning and retention; encrypted local backup/restore via user-selected files. Test wrong password, truncated/corrupt backup, schema versions, interrupted restore, credential exclusion and full deletion. Cloud sync is out of scope until separately specified. |

Groups 21–24 expand in every phase: settings, permissions, privacy, diagnostics,
and recovery are ongoing requirements, not completed by the foundation subset.
Before implementing a later phase, record concrete actions, API contracts,
permission needs, test cases and device acceptance criteria. Gemini, accessibility,
and third-party app behavior are subject to platform and provider constraints.

## Reporting and release discipline

- Separate implementation, local JVM/build checks, hosted CI, emulator, and phone evidence.
- Record tests executed, passed, failed, skipped, and unrun. Compilation is not a runtime test.
- Update the catalog and inventory together; attach device model, Android version, date and limitations to device evidence.
- Keep device logs free of API keys and personal message/audio content.
- Never read, modify, import, or use old Jerry/Maya/Zoya or Tech GPTx source as a template.
- Do not push, merge, publish, or create a release without the owner's authorization.
- Preserve third-party licenses and review new model/library redistribution terms.

## Confirmed-actions increment (0.2.0)

Owner-authorized increment delivered ahead of complete Phases 3/4: official
launcher/search/Home/dialer/SMS-composer intents; delegated foreground speech
input; one-action confirmation gates; optional scoped accessibility Back/tap/
swipe/scroll/text and search-field filling; bounded result checks. See ACTIONS.md.
This does not complete voice, app-specific integrations, screenshots, contacts,
actual call/message delivery, or multi-step task automation. All Android behavior
still requires device testing; no phase is promoted based on compilation alone.
