// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tanimboss.jerryai.actions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Process-local only. Enabling the OS service never arms automation by itself. */
object AutomationConnection {
    var service: JerryAccessibilityService? = null
        private set
    private val mutableConnected = MutableStateFlow(false)
    private val mutableAllowed = MutableStateFlow(false)
    val connected = mutableConnected.asStateFlow()
    val allowed = mutableAllowed.asStateFlow()
    fun consent(allow: Boolean) { mutableAllowed.value = allow && service != null }
    internal fun attach(value: JerryAccessibilityService) {
        service = value; mutableAllowed.value = false; mutableConnected.value = true
    }
    internal fun detach(value: JerryAccessibilityService) {
        if (service === value) { service = null; mutableAllowed.value = false; mutableConnected.value = false }
    }
}

class AccessibilityBackend : ActionBackend {
    override suspend fun perform(command: Command, target: AppTarget?): Attempt {
        val service = AutomationConnection.service
            ?: return Attempt.Handled(ActionResult(Outcome.FAILED, "Accessibility service is disconnected. Enable it manually in Android settings."))
        return Attempt.Handled(service.execute(command, target))
    }
}

private class AutomationFailure(message: String) : Exception(message)

class JerryAccessibilityService : AccessibilityService() {
    private val mutex = Mutex()
    override fun onServiceConnected() { AutomationConnection.attach(this) }
    // No continuous observation, event logging, learned commands, or content retention.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { AutomationConnection.consent(false) }
    override fun onUnbind(intent: Intent?): Boolean { AutomationConnection.detach(this); return super.onUnbind(intent) }
    override fun onDestroy() { AutomationConnection.detach(this); super.onDestroy() }

    private fun checkSession() {
        if (AutomationConnection.service !== this || !AutomationConnection.allowed.value) throw AutomationFailure("Automation session is disabled or disconnected.")
        if (AndroidActions(this).locked()) throw AutomationFailure("Phone is locked. Unlock it yourself; Jerry cannot bypass the lock screen.")
    }

    suspend fun awaitPackage(packageName: String): Boolean {
        repeat(20) {
            if (AutomationConnection.service !== this || !AutomationConnection.allowed.value) return false
            if (rootInActiveWindow?.packageName?.toString() == packageName) return true
            delay(100)
        }
        return false
    }

    private fun root(target: AppTarget): AccessibilityNodeInfo {
        checkSession()
        val node = rootInActiveWindow ?: throw AutomationFailure("No accessible foreground window. Screen content may be protected or unavailable.")
        if (node.packageName?.toString() != target.packageName) throw AutomationFailure("Foreground app differs from the confirmed target. Stopped; inspect the target app before retrying.")
        return node
    }

    /** Limit work and never inspect invisible/password content. Node objects are not retained. */
    private fun nodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val stack = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        val visited = HashSet<AccessibilityNodeInfo>()
        val visible = mutableListOf<AccessibilityNodeInfo>()
        while (stack.isNotEmpty() && visited.size < 512) {
            val node = stack.removeLast()
            if (!visited.add(node)) continue
            if (node.isVisibleToUser) visible += node
            if (node.childCount > 512) throw AutomationFailure("Screen tree exceeds the supported size. Use the app directly.")
            for (i in 0 until node.childCount) node.getChild(i)?.let(stack::add)
        }
        if (stack.isNotEmpty()) throw AutomationFailure("Screen tree exceeds the supported size. Use the app directly.")
        return visible
    }

    private fun AccessibilityNodeInfo.protected(): Boolean = protectedEditor(isPassword, inputType)

    private fun fingerprint(target: AppTarget): Int {
        val current = root(target)
        return nodes(current).map { node ->
            val bounds = Rect().also(node::getBoundsInScreen)
            listOf(node.viewIdResourceName, node.className, if (node.protected()) "[protected]" else node.text,
                if (node.protected()) "[protected]" else node.contentDescription, bounds.toShortString(), node.isChecked, node.isSelected)
        }.hashCode()
    }

    private suspend fun observeChange(target: AppTarget, before: Int, description: String): ActionResult {
        repeat(12) {
            delay(100)
            checkSession()
            if (rootInActiveWindow?.packageName?.toString() != target.packageName) {
                return ActionResult(Outcome.UNVERIFIED, "$description was accepted, but foreground app changed. Check the target app; external effects are not verified.")
            }
            if (fingerprint(target) != before) return ActionResult(Outcome.OBSERVED, "Visible UI changed after $description. Message delivery, calls, and other external effects are not independently verified.")
        }
        return ActionResult(Outcome.UNVERIFIED, "$description was accepted but no visible change could be verified. Inspect the app before retrying.")
    }

    suspend fun execute(command: Command, target: AppTarget?): ActionResult = mutex.withLock {
        var dispatched = false
        try {
            checkSession()
            val app = target ?: throw AutomationFailure("Select a target app before previewing the command.")
            val actions = AndroidActions(this)
            if (actions.isProtected(app.packageName) || app.packageName == packageName || actions.apps().none { it.packageName == app.packageName }) {
                throw AutomationFailure("Target is not an eligible installed app. Security and permission screens cannot be automated.")
            }
            val launch = packageManager.getLaunchIntentForPackage(app.packageName)
                ?: throw AutomationFailure("Target app cannot be launched.")
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            if (!awaitPackage(app.packageName)) throw AutomationFailure("Target app did not become accessible in time. No UI action was performed.")
            checkSession()
            val before = fingerprint(app)
            val visible = nodes(root(app))
            when (command) {
                Command.Back -> {
                    if (!performGlobalAction(GLOBAL_ACTION_BACK)) throw AutomationFailure("Android rejected Back.")
                    dispatched = true
                    observeChange(app, before, "Back")
                }
                is Command.Tap -> {
                    val matching = visible.filter { !it.protected() && it.isEnabled &&
                        (it.text?.toString()?.equals(command.label, true) == true || it.contentDescription?.toString()?.equals(command.label, true) == true) }
                    val clickable = matching.mapNotNull { start ->
                        var node: AccessibilityNodeInfo? = start
                        var result: AccessibilityNodeInfo? = null
                        repeat(5) {
                            val current = node
                            if (current != null && current.isVisibleToUser && current.isEnabled && !current.protected() && current.isClickable) {
                                if (result == null) result = current
                            }
                            node = current?.parent
                        }
                        result
                    }.distinct()
                    if (clickable.size != 1) throw AutomationFailure("Tap needs exactly one visible clickable match. Found ${clickable.size}; use a more specific label or the app directly.")
                    root(app) // Recheck scope immediately before dispatch.
                    val selected = clickable.single()
                    if (!selected.refresh() || !selected.isVisibleToUser || !selected.isEnabled || selected.protected()) throw AutomationFailure("Tap target changed or became protected.")
                    if (!selected.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw AutomationFailure("Target rejected the tap.")
                    dispatched = true
                    observeChange(app, before, "tap")
                }
                is Command.Type -> setText(app, command.text, searchOnly = false) { dispatched = true }
                is Command.Search -> setText(app, command.query, searchOnly = true) { dispatched = true }
                is Command.Scroll -> {
                    val id = when (command.direction) {
                        Direction.UP -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id
                        Direction.DOWN -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id
                        Direction.LEFT -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
                        Direction.RIGHT -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
                    }
                    val legacy = when (command.direction) {
                        Direction.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                        Direction.UP -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                        else -> id
                    }
                    val matches = visible.filter { it.isEnabled && it.isScrollable && it.actionList.any { action -> action.id == id || action.id == legacy } }
                    if (matches.size != 1) throw AutomationFailure("Scroll needs one unambiguous accessible scrolling container; found ${matches.size}.")
                    val node = matches.single()
                    val actionId = if (node.actionList.any { it.id == id }) id else legacy
                    root(app)
                    if (!node.performAction(actionId)) throw AutomationFailure("Scroll was rejected or the list has reached its end.")
                    dispatched = true
                    observeChange(app, before, "scroll")
                }
                is Command.Swipe -> {
                    val bounds = Rect().also(root(app)::getBoundsInScreen)
                    val metrics = resources.displayMetrics
                    if (!bounds.intersect(0, 0, metrics.widthPixels, metrics.heightPixels) || bounds.width() < 100 || bounds.height() < 100) {
                        throw AutomationFailure("The app's visible bounds are unsuitable for a safe swipe.")
                    }
                    val x = bounds.exactCenterX(); val y = bounds.exactCenterY()
                    val left = bounds.left + bounds.width() * .25f; val right = bounds.left + bounds.width() * .75f
                    val top = bounds.top + bounds.height() * .25f; val bottom = bounds.top + bounds.height() * .75f
                    val path = Path().apply {
                        when (command.direction) {
                            Direction.UP -> { moveTo(x, bottom); lineTo(x, top) }
                            Direction.DOWN -> { moveTo(x, top); lineTo(x, bottom) }
                            Direction.LEFT -> { moveTo(right, y); lineTo(left, y) }
                            Direction.RIGHT -> { moveTo(left, y); lineTo(right, y) }
                        }
                    }
                    root(app)
                    val completed = withTimeoutOrNull(1_500) {
                        suspendCancellableCoroutine { continuation ->
                            val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 350)).build()
                            val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
                                override fun onCompleted(gestureDescription: GestureDescription?) { if (continuation.isActive) continuation.resume(true) }
                                override fun onCancelled(gestureDescription: GestureDescription?) { if (continuation.isActive) continuation.resume(false) }
                            }, null)
                            if (accepted) dispatched = true
                            if (!accepted && continuation.isActive) continuation.resume(false)
                        }
                    } ?: false
                    if (!completed) throw AutomationFailure("Android cancelled or rejected the swipe. Inspect the app before retrying.")
                    observeChange(app, before, "swipe")
                }
                else -> ActionResult(Outcome.FAILED, "No accessibility fallback exists for this command.")
            }
        } catch (failure: AutomationFailure) {
            ActionResult(if (dispatched) Outcome.UNVERIFIED else Outcome.FAILED,
                if (dispatched) "Action may already have occurred; verification stopped. ${failure.message} Inspect before retrying."
                else failure.message ?: "Automation failed.")
        } catch (_: SecurityException) {
            ActionResult(if (dispatched) Outcome.UNVERIFIED else Outcome.FAILED, "Android denied access. Verification stopped; inspect the app if an action already started. No security restriction was bypassed.")
        } catch (_: android.content.ActivityNotFoundException) {
            ActionResult(Outcome.FAILED, "The target app is no longer available.")
        }
    }

    private suspend fun setText(target: AppTarget, text: String, searchOnly: Boolean, onDispatch: () -> Unit): ActionResult {
        val editable = nodes(root(target)).filter { it.isEditable && it.isEnabled }
        if (editable.any { it.isFocused && it.protected() }) throw AutomationFailure("Password fields cannot be automated.")
        val candidates = editable.filterNot { it.protected() }.filter { node ->
            !searchOnly || searchField(node.hintText?.toString(), node.contentDescription?.toString(), node.viewIdResourceName)
        }
        val focused = candidates.filter { it.isFocused }
        val node = (if (focused.isNotEmpty()) focused else candidates).singleOrNull()
            ?: throw AutomationFailure("Text entry needs one eligible ${if (searchOnly) "search" else "focused or unique editable"} field. Password fields are excluded.")
        val identity = node.viewIdResourceName
        root(target)
        if (!node.refresh() || !node.isVisibleToUser || !node.isEditable || node.protected()) {
            throw AutomationFailure("Editable field changed or became protected. No text was entered.")
        }
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) throw AutomationFailure("The field rejected text entry.")
        onDispatch()
        repeat(10) {
            delay(100)
            val fields = nodes(root(target)).filter { it.isEditable && it.isEnabled && !it.protected() }
            val sameField = if (identity != null) fields.singleOrNull { it.viewIdResourceName == identity }
                else fields.singleOrNull { it.isFocused } ?: fields.singleOrNull()
            if (sameField?.text?.toString() == text) return ActionResult(Outcome.OBSERVED,
                if (searchOnly) "Search text is visible in the target field. Submit the search in the app; results have not been verified."
                else "Replacement text is visible. Jerry issued no submit/send command; target-app side effects are not independently verified.")
        }
        return ActionResult(Outcome.UNVERIFIED, "Text entry was accepted but the resulting field text could not be verified. Inspect before retrying.")
    }
}
