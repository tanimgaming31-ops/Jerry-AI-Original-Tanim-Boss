# Confirmed Android commands and optional accessibility

This is original implementation code. Runtime compatibility with Android phones
and third-party apps is not yet verified. Commands support a bounded English
grammar, not arbitrary natural-language tasks. No Gemini or background microphone
service is included.

## Execution contract

1. The user supplies a command by typing or invoking Android's speech-recognition
   activity. Recognition only fills the text field. Screen text cannot issue commands.
2. The user chooses a launchable target app when needed, previews the exact command,
   and explicitly confirms in Jerry. All actions, including taps/gestures which may
   activate sensitive controls, require confirmation. Numbers/text are shown before
   opening dialer/composer. Changes invalidate pending previews.
3. The coordinator consumes the immutable target/command token once, within 30
   seconds. Cancelled, expired, replaced, or reused tokens cannot execute anything.
4. A supported official intent is tried first. An explicit denial/failure is not
   an invitation to bypass it with accessibility. Only unsupported operations can
   reach the optional fallback.
5. Fallback requires the OS-bound service and separate process-local session consent.
   It verifies that the phone is unlocked and the selected app is launchable and
   not Jerry, settings, permission-controller, installer, or recognized system-security
   UI. The protected-target policy is conservative, not an exhaustive sensitivity
   classifier for arbitrary apps; every command still needs confirmation. After launching the target, every operation rechecks foreground package and
   session state. Password flags and text/numeric password input variations are excluded.
6. One action is dispatched, then its result is observed with a bounded wait. No
   automatic retries occur. A mutex serializes service actions. Tree inspection is
   bounded to 512 nodes; ambiguous targets fail instead of choosing arbitrarily.

The accessibility service is exported only behind Android's system signature
`BIND_ACCESSIBILITY_SERVICE` permission, with no custom command receiver or external
command IPC. It cannot grant permissions, unlock the phone, root/shell the device,
or capture screenshots. Event callbacks do not record screen contents. Access is
used transiently for confirmed commands and verification, not a continuous screen
history. Enabling the service alone leaves the session disabled.

## Observable result limits

| Command | First choice / fallback | What is actually verified |
| --- | --- | --- |
| open app | Launcher intent | Foreground package if opted-in service can observe it; otherwise request is unverified |
| home | Android Home intent | Intent acceptance only; user checks launcher |
| back | Accessibility global Back | Visible target UI change; leaving target is reported unverified |
| search QUERY | Target app's ACTION_SEARCH | Intent acceptance only; actual search results unverified |
| search QUERY fallback | Eligible accessible search field | Query text equals requested text; user submits search manually |
| tap LABEL | Exact text/content-description, nearest clickable parent | Unique visible match and an observed UI change; no proof of external effects |
| type TEXT | ACTION_SET_TEXT on focused/unique editable field | Replacement text matches; no submit/send command is issued by Jerry |
| scroll DIRECTION | Node directional/forward/backward scroll action | One eligible container and observed visible change |
| swipe DIRECTION | Android dispatchGesture within the app's interior bounds | Gesture callback plus observed visible change; callback alone is insufficient |
| dial/call NUMBER | ACTION_DIAL | Intent accepted; Jerry never claims a call was placed |
| message NUMBER: BODY | ACTION_SENDTO with smsto URI | Intent accepted; Jerry never claims a message was sent |

Only ordinary 3–15 digit numbers with an optional leading plus are accepted.
USSD/service codes, pause characters, URI parameters, schemes, and control
characters are rejected. Text entry is bounded to 300 characters, search queries
to 200, labels to 100, and commands to 500. Search-field fallback requires a clear
English search hint/description or recognized search view ID; other languages
and custom renderers may need manual interaction.

An observed UI change is not proof of a message, purchase, call, or server-side
operation. A target app can react to text replacement or swipes in its own way;
confirm only intended actions. Accessibility nodes cannot provide transactional
isolation: screens can change between a check and Android dispatch. The service
rechecks immediately before acting, stops on scope changes, and reports uncertainty.
It does not claim atomic screen ownership or guaranteed effect prevention after
Android has accepted a gesture/action. Do not use it on sensitive screens you
cannot review. Lock/permission/security screens and unavailable content are not
worked around.

## Stop and failure behavior

Stop cancels waiting, invalidates the pending confirmation and disables the session.
Android may already have accepted an action; cancellation cannot undo it. Consent
revocation/disconnection stops further operations. Timeouts, missing handlers,
multiple matches, inaccessible windows, rejected actions, protected editors and
unobservable results have explicit messages. Outcomes are `OBSERVED`, `UNVERIFIED`,
`FAILED`, or `CANCELLED`; none is a generic unconditional success.

No command, phone number, message, or screen text is logged or persisted. Pending
commands are not saved across process death, and credentials are never collected.
The selected Android speech provider and apps launched by intents have their own
privacy and permission behavior. Jerry does not receive microphone, network,
contacts, direct-call, SMS, or broad installed-package permissions.
