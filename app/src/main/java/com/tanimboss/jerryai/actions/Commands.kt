// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.actions

import java.util.UUID

data class AppTarget(val packageName: String, val label: String)
enum class Direction { UP, DOWN, LEFT, RIGHT }
sealed interface Command {
    data object OpenApp : Command
    data object Back : Command
    data object Home : Command
    data class Search(val query: String) : Command
    data class Tap(val label: String) : Command
    data class Type(val text: String) : Command
    data class Scroll(val direction: Direction) : Command
    data class Swipe(val direction: Direction) : Command
    data class Dial(val number: String) : Command
    data class Message(val number: String, val body: String) : Command
}

/** Deliberately bounded English grammar; screen contents cannot create commands. */
object CommandParser {
    /**
     * Small, explicit voice aliases. Never infer a tap, typing, calling or messaging
     * action from a translation: every resulting command still goes through the
     * existing immutable preview and confirmation gate.
     */
    private fun normalizeReadOnlyNavigation(input: String): String {
        val withoutName = Regex("^(?:জেরি|জেরী|hey jerry|jerry)\\s*[,，:।]?\\s+", RegexOption.IGNORE_CASE)
            .replaceFirst(input, "")
        return when (withoutName.lowercase()) {
        "অ্যাপ খোলো", "অ্যাপ চালু করো", "ওপেন অ্যাপ" -> "open app"
        "পেছনে যাও", "পিছনে যাও", "ব্যাক করো" -> "back"
        "হোমে যাও", "হোম খোলো" -> "home"
        "উপরে স্ক্রল করো" -> "scroll up"
        "নিচে স্ক্রল করো" -> "scroll down"
        "উপরে সোয়াইপ করো", "উপরে সোয়াইপ করো" -> "swipe up"
        "নিচে সোয়াইপ করো", "নিচে সোয়াইপ করো" -> "swipe down"
        else -> input
        }
    }

    fun parse(input: String): Command? {
        val value = normalizeReadOnlyNavigation(input.trim())
        if (value.isEmpty() || value.length > 500 || value.any { it.isISOControl() }) return null
        when (value.lowercase()) {
            "open app", "launch app" -> return Command.OpenApp
            "back", "go back" -> return Command.Back
            "home", "go home" -> return Command.Home
        }
        val verb = value.substringBefore(' ').lowercase()
        val argument = value.substringAfter(' ', "").trim()
        if (argument.isEmpty()) return null
        return when (verb) {
            "search" -> Command.Search(argument.takeIf { it.length <= 200 } ?: return null)
            "tap" -> Command.Tap(argument.takeIf { it.length <= 100 } ?: return null)
            "type" -> Command.Type(argument.takeIf { it.length <= 300 } ?: return null)
            "swipe", "scroll" -> {
                val direction = Direction.entries.firstOrNull { it.name.equals(argument, ignoreCase = true) } ?: return null
                if (verb == "swipe") Command.Swipe(direction) else Command.Scroll(direction)
            }
            "dial", "call" -> Command.Dial(phone(argument) ?: return null)
            "message" -> {
                val number = phone(argument.substringBefore(':')) ?: return null
                val body = argument.substringAfter(':', "").trim()
                if (body.isEmpty() || body.length > 300) null else Command.Message(number, body)
            }
            else -> null
        }
    }
    private fun phone(value: String): String? {
        // Never allow tel URI parameters, USSD, URI schemes, pauses, or dial codes.
        if (!value.matches(Regex("\\+?[0-9 ()-]{3,30}"))) return null
        val number = value.filter { it.isDigit() || it == '+' }
        return number.takeIf { it.count(Char::isDigit) in 3..15 }
    }
}

enum class Outcome(val label: String) {
    OBSERVED("Observed result"), UNVERIFIED("Requested; result unverified"), FAILED("Failed"), CANCELLED("Cancelled")
}
data class ActionResult(val outcome: Outcome, val detail: String)
data class Preview(val token: String, val command: Command, val target: AppTarget?, val description: String, val expiresAt: Long)

/** One pending action, immutable target, single use, monotonic expiry; confirmation is never inferred. */
class ConfirmationGate(private val now: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private var pending: Preview? = null
    @Synchronized fun stage(command: Command, target: AppTarget?, description: String): Preview =
        Preview(UUID.randomUUID().toString(), command, target, description, now() + 30_000).also { pending = it }
    @Synchronized fun consume(token: String): Preview? {
        val action = pending ?: return null
        if (action.token != token) return null
        pending = null
        return action.takeIf { now() < it.expiresAt }
    }
    @Synchronized fun cancel() { pending = null }
}

sealed interface Attempt {
    data object Unsupported : Attempt
    data class Handled(val result: ActionResult) : Attempt
}
fun interface ActionBackend { suspend fun perform(command: Command, target: AppTarget?): Attempt }

class ActionCoordinator(
    private val official: ActionBackend,
    private val accessibility: ActionBackend,
    private val automationAllowed: () -> Boolean,
    val gate: ConfirmationGate = ConfirmationGate(),
) {
    suspend fun confirm(token: String): ActionResult {
        val request = gate.consume(token) ?: return ActionResult(Outcome.FAILED, "Confirmation expired, cancelled, or already used. Preview again.")
        val standard = official.perform(request.command, request.target)
        if (standard is Attempt.Handled) return standard.result
        if (!automationAllowed()) return ActionResult(Outcome.FAILED, "No supported Android intent. Enable accessibility and allow this automation session to use the optional fallback.")
        return when (val fallback = accessibility.perform(request.command, request.target)) {
            Attempt.Unsupported -> ActionResult(Outcome.FAILED, "This command is not supported on the current screen.")
            is Attempt.Handled -> fallback.result
        }
    }
}
