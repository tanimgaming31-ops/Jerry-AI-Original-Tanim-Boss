// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai

import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun allScreensAreReachable() {
        compose.onNodeWithText("Features", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Feature inventory").assertIsDisplayed()
        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        compose.onNodeWithText("Diagnostics", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Permission diagnostics").assertIsDisplayed()
        compose.onNodeWithText("About", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Original contributions").assertIsDisplayed()
        compose.onNodeWithText("Actions", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Supported commands").assertIsDisplayed()
        compose.onNodeWithText("Home", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Voice is not active").performScrollTo().assertIsDisplayed()
    }

    @Test fun themeAndPageSurviveActivityRecreation() {
        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Loading settings…").fetchSemanticsNodes().isEmpty() }
        try {
            compose.onNodeWithText("Dark theme").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText("Dark theme") and isSelected()).fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithText("Dark theme").assertIsSelected()
            compose.activityRule.scenario.recreate()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText("Dark theme") and isSelected()).fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithText("Dark theme").assertIsSelected()
        } finally {
            compose.onNodeWithText("System theme").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasText("System theme") and isSelected()).fetchSemanticsNodes().size == 1
            }
        }
    }

    @Test fun cancellingActionPreviewDoesNotExecute() {
        compose.onNodeWithText("Actions", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Command").performScrollTo().performTextInput("dial 5551234")
        compose.onNodeWithText("Preview command").performScrollTo().performClick()
        compose.onNodeWithText("Confirm one action").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Confirm one action").assertDoesNotExist()
        compose.onNodeWithText("Command").assertTextContains("dial 5551234")
    }

    @Suppress("DEPRECATION")
    @Test fun foundationDeclaresNoSensitiveOrNetworkPermissions() {
        val context = compose.activity
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()
        val forbidden = setOf(
            "android.permission.INTERNET", "android.permission.RECORD_AUDIO", "android.permission.READ_CONTACTS",
            "android.permission.CALL_PHONE", "android.permission.SEND_SMS", "android.permission.FOREGROUND_SERVICE_MICROPHONE",
        )
        assertTrue("Unexpected permission: ${permissions intersect forbidden}", (permissions intersect forbidden).isEmpty())
    }
}
