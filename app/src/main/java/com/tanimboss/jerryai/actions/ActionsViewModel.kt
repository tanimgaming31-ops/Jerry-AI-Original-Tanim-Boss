// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.actions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanimboss.jerryai.automation.AccessibilityBackend
import com.tanimboss.jerryai.automation.AutomationConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// Commands, phone numbers and target selection stay in memory, not saved-state or preferences.
data class ActionsUiState(
    val apps: List<AppTarget> = emptyList(), val target: AppTarget? = null, val input: String = "",
    val preview: Preview? = null, val busy: Boolean = false, val result: ActionResult? = null,
)

class ActionsViewModel(application: Application) : AndroidViewModel(application) {
    private val official = AndroidActions(application)
    private val coordinator = ActionCoordinator(official, AccessibilityBackend(), { AutomationConnection.allowed.value })
    private val mutableState = MutableStateFlow(ActionsUiState())
    val state = mutableState.asStateFlow()
    private var execution: Job? = null
    init { refreshApps() }
    fun refreshApps() {
        if (mutableState.value.busy) return
        cancelPreview()
        try {
            val apps = official.apps()
            mutableState.update { state -> state.copy(apps = apps, target = apps.firstOrNull { it.packageName == state.target?.packageName }) }
        } catch (_: RuntimeException) { report("The installed-app list could not be read.") }
    }
    fun select(target: AppTarget) { if (!mutableState.value.busy) { cancelPreview(); mutableState.update { it.copy(target = target) } } }
    fun edit(value: String) { if (!mutableState.value.busy) { cancelPreview(); mutableState.update { it.copy(input = value.take(500), result = null) } } }
    fun report(detail: String) { mutableState.update { it.copy(result = ActionResult(Outcome.FAILED, detail)) } }
    fun cancelPreview() { coordinator.gate.cancel(); mutableState.update { it.copy(preview = null) } }
    fun preview() {
        if (mutableState.value.busy) return
        cancelPreview()
        val state = mutableState.value
        val command = CommandParser.parse(state.input) ?: return report("Unsupported command. Use the command examples; numbers must be ordinary phone numbers, not service codes.")
        val needsTarget = command !is Command.Dial && command !is Command.Message && command != Command.Home
        if (needsTarget && state.target == null) return report("Select the app first.")
        val description = when (command) {
            Command.OpenApp -> "Open the selected app"
            Command.Back -> "Navigate Back in the selected app"
            Command.Home -> "Open Android Home"
            is Command.Search -> "Search for: ${command.query}. If no search intent exists, only fill an eligible search field; you submit it yourself."
            is Command.Tap -> "Tap exactly: ${command.label}. This may activate a sensitive control, send content, or place a call. Confirm only if you intend that effect."
            is Command.Type -> "Replace the focused or unique editable field with: ${command.text}. No submit action will be issued; the target app may react to text changes."
            is Command.Scroll -> "Scroll ${command.direction.name.lowercase()} in the selected app"
            is Command.Swipe -> "Swipe ${command.direction.name.lowercase()} inside the selected app. Swipes can trigger app-specific actions."
            is Command.Dial -> "Open the dialer for ${command.number}. You place the call yourself."
            is Command.Message -> "Open a message composer to ${command.number} with text: ${command.body}. You send it yourself."
        }
        val target = if (needsTarget) state.target else null
        val staged = coordinator.gate.stage(command, target, description)
        mutableState.update { it.copy(preview = staged, result = null) }
    }
    fun confirm(token: String) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(preview = null, input = "", busy = true, result = null) }
        execution = viewModelScope.launch {
            try {
                val result = withTimeout(8_000) { coordinator.confirm(token) }
                mutableState.update { it.copy(result = result) }
            } catch (_: TimeoutCancellationException) {
                mutableState.update { it.copy(result = ActionResult(Outcome.UNVERIFIED, "Timed out waiting. An action may already have been accepted; inspect the app before retrying.")) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                mutableState.update { it.copy(result = ActionResult(Outcome.UNVERIFIED,
                    "Action result could not be checked. It may already have started; inspect the target app before retrying.")) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }
    fun stop() {
        coordinator.gate.cancel(); execution?.cancel()
        AutomationConnection.consent(false)
        mutableState.update { it.copy(preview = null, input = "", result = ActionResult(Outcome.CANCELLED,
            "Stopped waiting and disabled the automation session. An action already accepted by Android cannot be undone; inspect the target app.")) }
    }
    override fun onCleared() { AutomationConnection.consent(false); coordinator.gate.cancel(); super.onCleared() }
}
