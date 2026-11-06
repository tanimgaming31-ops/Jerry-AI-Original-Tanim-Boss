# Jerry AI Original — privacy

Owner/publisher: Tanim Boss. Version 0.3.0-live-voice, development build.

## What is processed

- Theme and voice/model/language/persona preferences are stored locally in separate
  DataStore files. Existing theme data is preserved; this increment performs no
  Room/memory migration and does not erase user memories.
- You enter your own Gemini key on the phone. It is encrypted with AES-256-GCM
  using Android Keystore; the encrypted file is in noBackupFilesDir. Keys, resume
  handles and raw provider errors are not logged, embedded in the APK, stored in
  source, or included in app backups. A rooted/compromised device cannot be assumed
  to keep client credentials secret. Remove key stops voice and removes only that
  encrypted credential; authentication failures do not automatically delete it.
- Live microphone capture begins only after Start, a valid configured model/key,
  Android microphone permission, audio focus and Gemini setupComplete. PCM is sent
  over authenticated TLS WebSocket to generativelanguage.googleapis.com. Local
  activity detection controls input turns; audio is not saved to files or replayed
  after reconnection. Input/output captions are bounded and held only in memory.
- Model discovery is a separate explicit HTTPS request to Google's models API.
  It does not capture microphone audio. No model or free quota is assumed available.
- Google processes audio/text under its own Gemini API terms and data policies.
  Review https://ai.google.dev/gemini-api/terms and your account settings, pricing
  and quota before use. This app cannot guarantee free provider usage. There is no
  automatic paid-model fallback, account purchase or billing configuration.
- Actions' optional Android speech-recognition activity is a separate speech app,
  which may process audio under its own policy. Its result only fills a command.
- Optional accessibility remains user-enabled and separately session-authorized.
  Only confirmed selected-app actions inspect accessible nodes transiently. No
  screenshots, clipboard recording, continuous screen history or automatic AI tool
  execution is added. Gemini has no device tools in this phase.

## Controls and limits

Mute stops microphone capture and suppresses response playback; unmute requires
new user input before old response audio can be played. Interrupt clears playback
and signals Gemini's activity interrupt behavior. Stop closes the connection and
releases audio. Leaving the app stops voice; rotation may retain the foreground
session. Focus loss or capture errors stop it; reconnects pause the microphone,
are limited to three attempts and never replay captured input. There is no wake
word, background microphone service or hidden startup recording in Phase 1.

English/Bengali Stop/persona commands depend on Gemini returning an input
transcript or turn boundary. They are not offline controls. Stop remains available
in the UI when the service/network cannot recognize speech.

Android permissions, the lock screen, protected fields and explicit action
confirmations are respected. No camera, contacts, direct-call, SMS, notification
listener, screenshot or overlay permission is requested by this increment. There
is no app-owned analytics backend. Internet, microphone and normal audio-settings
permissions are now required for the user-started foreground Live session.

Backup is disabled and transfer domains excluded by manifest configuration;
physical-device/OEM behavior remains unverified. Full chat/history/memory export
and deletion are future Phase 3 capabilities, not implemented here. Do not include
personal speech, private screenshots or credentials in diagnostic reports.
