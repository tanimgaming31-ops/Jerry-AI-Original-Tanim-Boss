// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.diagnostics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class AccessState(val label: String) {
    NOT_DECLARED("Not declared in this build"), DENIED("Not granted"),
    GRANTED("Granted"), NOT_APPLICABLE("Not required on this Android version")
}

/** Evaluate manifest membership before checking grants; absence is not a denial. */
fun permissionState(declared: Boolean, granted: Boolean, applicable: Boolean = true): AccessState = when {
    !applicable -> AccessState.NOT_APPLICABLE
    !declared -> AccessState.NOT_DECLARED
    granted -> AccessState.GRANTED
    else -> AccessState.DENIED
}

data class PermissionDiagnostic(val title: String, val state: AccessState, val reason: String)

@Suppress("DEPRECATION") // Legacy overload supports the full API 29+ range.
fun readPermissionDiagnostics(context: Context): List<PermissionDiagnostic> {
    val declared = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions.orEmpty().toSet()
    fun entry(title: String, permission: String, reason: String, applicable: Boolean = true) = PermissionDiagnostic(
        title,
        permissionState(permission in declared, context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED, applicable),
        reason,
    )
    return listOf(
        entry("Microphone", Manifest.permission.RECORD_AUDIO, "For future voice sessions. No audio is captured in this build."),
        entry("Contacts", Manifest.permission.READ_CONTACTS, "For future contact lookup. Contacts are not read in this build."),
        entry("Calls", Manifest.permission.CALL_PHONE, "For future confirmed calling actions. Calls are not placed in this build."),
        if (Build.VERSION.SDK_INT >= 33) {
            entry("Notifications", Manifest.permission.POST_NOTIFICATIONS, "For a future visible voice service.")
        } else {
            PermissionDiagnostic("Notifications", AccessState.NOT_APPLICABLE, "Runtime notification permission starts with Android 13.")
        },
    )
}
