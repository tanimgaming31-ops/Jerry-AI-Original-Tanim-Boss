// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.actions

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.app.SearchManager
import android.content.pm.PackageManager
import android.net.Uri
import com.tanimboss.jerryai.automation.AutomationConnection
import kotlinx.coroutines.delay

class AndroidActions(private val context: Context) : ActionBackend {
    private val pm get() = context.packageManager

    @Suppress("DEPRECATION")
    fun apps(): List<AppTarget> = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { AppTarget(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.packageName }.filterNot { isProtected(it.packageName) || it.packageName == context.packageName }
        .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))

    fun isProtected(packageName: String): Boolean {
        val settings = pm.resolveActivity(Intent(android.provider.Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return protectedTarget(packageName, context.packageName, settings)
    }

    fun locked(): Boolean = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    override suspend fun perform(command: Command, target: AppTarget?): Attempt {
        if (locked()) return handled(Outcome.FAILED, "Unlock the phone yourself before issuing a command.")
        if (target != null && (isProtected(target.packageName) || target.packageName == context.packageName)) {
            return handled(Outcome.FAILED, "Automation of Jerry, Android security, permission, or settings screens is not supported.")
        }
        val intent = when (command) {
            Command.OpenApp -> target?.let { pm.getLaunchIntentForPackage(it.packageName) }
                ?: return handled(Outcome.FAILED, "Select an installed launchable app.")
            Command.Home -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            is Command.Search -> {
                if (target == null) return handled(Outcome.FAILED, "Select the app to search.")
                val search = Intent(Intent.ACTION_SEARCH).putExtra(SearchManager.QUERY, command.query).setPackage(target.packageName)
                if (pm.resolveActivity(search, PackageManager.MATCH_DEFAULT_ONLY) == null) return Attempt.Unsupported
                search
            }
            is Command.Dial -> Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", command.number, null))
            is Command.Message -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", command.number, null)).putExtra("sms_body", command.body)
            else -> return Attempt.Unsupported
        }
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            when (command) {
                Command.OpenApp -> {
                    if (AutomationConnection.allowed.value && AutomationConnection.service?.awaitPackage(target!!.packageName) == true) {
                        handled(Outcome.OBSERVED, "The selected app is now foreground. No other task is claimed complete.")
                    } else handled(Outcome.UNVERIFIED, "Android accepted the app-launch request. Foreground state could not be verified.")
                }
                is Command.Dial -> handled(Outcome.UNVERIFIED, "Dialer requested. No call was placed by Jerry; review the number and use the dialer's call button.")
                is Command.Message -> handled(Outcome.UNVERIFIED, "Message composer requested. Jerry did not send the message; review recipient and text in the messaging app.")
                is Command.Search -> handled(Outcome.UNVERIFIED, "Android accepted the app search request. Search results are not independently verified.")
                Command.Home -> {
                    delay(250)
                    handled(Outcome.UNVERIFIED, "Android accepted the Home intent. Inspect the launcher to verify navigation.")
                }
                else -> handled(Outcome.FAILED, "Unsupported action.")
            }
        } catch (_: ActivityNotFoundException) {
            handled(Outcome.FAILED, "No installed app handles this Android intent.")
        } catch (_: SecurityException) {
            handled(Outcome.FAILED, "Android denied the request. No permission or security restriction was bypassed.")
        }
    }
    private fun handled(outcome: Outcome, detail: String) = Attempt.Handled(ActionResult(outcome, detail))
}
