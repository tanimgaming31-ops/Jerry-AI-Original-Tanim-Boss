// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.actions

/** Conservative deny list plus the device's publicly resolved Settings package. */
fun protectedTarget(packageName: String, ownPackage: String, settingsPackage: String?): Boolean =
    packageName == ownPackage || packageName == "android" || packageName == "com.android.systemui" ||
        packageName == settingsPackage || listOf("permissioncontroller", "packageinstaller", "securitycenter", "settings")
            .any { packageName.contains(it, ignoreCase = true) }
