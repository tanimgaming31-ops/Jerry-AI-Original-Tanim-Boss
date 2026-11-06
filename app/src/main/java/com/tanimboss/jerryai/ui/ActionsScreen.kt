// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tanimboss.jerryai.actions.ActionsViewModel
import com.tanimboss.jerryai.automation.AutomationConnection

@Composable
fun ActionsScreen(viewModel: ActionsViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connected by AutomationConnection.connected.collectAsStateWithLifecycle()
    val allowed by AutomationConnection.allowed.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf(false) }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (text.isNullOrBlank()) viewModel.report("Speech recognition returned no command.") else viewModel.edit(text)
        } else viewModel.report("Speech input was cancelled or failed. No command was executed.")
    }
    Text("Supported commands", style = MaterialTheme.typography.headlineSmall)
    Text("Android intents first. Optional accessibility can interact with a selected app only after your confirmation. App layouts and accessible labels determine what works.")
    Text("Accessibility service: ${if (connected) "connected" else "not connected"}")
    OutlinedButton(onClick = {
        viewModel.cancelPreview()
        try { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        catch (_: ActivityNotFoundException) { viewModel.report("Accessibility settings are not available on this device.") }
        catch (_: SecurityException) { viewModel.report("Android blocked this settings screen.") }
    }, enabled = !state.busy) { Text("Enable service in Android settings") }
    Text("Disclosure: enabling this service lets Jerry read accessible screen text and perform taps, text entry and gestures for confirmed commands. It does not capture screenshots, run continuous listening, bypass permissions, or retain screen contents. Use only on screens you trust.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(checked = allowed, enabled = connected && !state.busy, onCheckedChange = {
            viewModel.cancelPreview(); AutomationConnection.consent(it)
        })
        Text("Allow automation this session")
    }
    Text("Consent resets on service disconnect or process restart. Turn it off here, or disable Jerry's service in Android settings at any time.")
    Box {
        OutlinedButton(onClick = { choosing = true }, enabled = !state.busy) {
            Text(state.target?.let { "Target: ${it.label}" } ?: "Choose target app")
        }
        DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
            state.apps.forEach { app ->
                DropdownMenuItem(text = { Text("${app.label}\n${app.packageName}") }, onClick = { viewModel.select(app); choosing = false })
            }
        }
    }
    OutlinedButton(onClick = { viewModel.refreshApps() }, enabled = !state.busy) { Text("Refresh installed apps") }
    OutlinedTextField(value = state.input, onValueChange = viewModel::edit, enabled = !state.busy,
        label = { Text("Command") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
    Text("Examples: open app · back · home · search music · tap Search · swipe up · scroll down · type Hello · dial +15551234567 · message +15551234567: Hello")
    Text("Voice uses your phone's separate speech-recognition app, which may process audio online under its own privacy policy. Jerry does not record audio. Recognition only fills the command; preview and confirmation are still required.")
    OutlinedButton(onClick = {
        viewModel.cancelPreview()
        try {
            speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say one supported Jerry command"))
        } catch (_: ActivityNotFoundException) { viewModel.report("No Android speech-recognition app is installed. Type a command instead.") }
        catch (_: SecurityException) { viewModel.report("Speech input was denied by Android. Type a command instead.") }
    }, enabled = !state.busy) { Text("Speak command") }
    Button(onClick = viewModel::preview, enabled = !state.busy && state.input.isNotBlank()) { Text("Preview command") }
    if (state.busy) { Text("Waiting for Android result… Return here to read the result."); LinearProgressIndicator(Modifier.fillMaxWidth()) }
    OutlinedButton(onClick = viewModel::stop) { Text("Stop / disable automation session") }
    state.result?.let { Text("${it.outcome.label}: ${it.detail}") }
    state.preview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::cancelPreview,
            title = { Text("Confirm one action") },
            text = { Text("${preview.target?.let { "Target: ${it.label} (${it.packageName})\n\n" } ?: ""}${preview.description}\n\nOnly confirm if you intend this action and its possible effects. Confirmation expires after 30 seconds and cannot be reused. Android security and permission screens are excluded.") },
            confirmButton = { TextButton(onClick = { viewModel.confirm(preview.token) }) { Text("Confirm action") } },
            dismissButton = { TextButton(onClick = viewModel::cancelPreview) { Text("Cancel") } },
        )
    }
}
