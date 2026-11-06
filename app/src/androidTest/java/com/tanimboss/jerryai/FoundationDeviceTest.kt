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
        compose.onNodeWithText("Jerry AI Original", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("More Tools", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Feature inventory").assertIsDisplayed()
        compose.onNodeWithText("Home", useUnmergedTree = true).performClick()
        compose.onNodeWithText("⚙", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Settings & Personalization").assertIsDisplayed()
        compose.onNodeWithText("Permissions", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithText("Permission diagnostics").assertIsDisplayed()
        compose.onNodeWithText("Profile", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Original contributions").assertIsDisplayed()
        compose.onNodeWithText("Tools", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Supported commands").assertIsDisplayed()
        compose.onNodeWithText("Chat", useUnmergedTree = true).performClick()
        compose.onNodeWithText("AI Chat is not ready yet").assertIsDisplayed()
        compose.onNodeWithText("Memory", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Memory Bank • planned").assertIsDisplayed()
    }

    @Test fun themePreferencePersistsWithNeonInterface() {
        compose.onNodeWithText("⚙", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Theme", useUnmergedTree = true).performClick()
        try {
            compose.onNode(hasText("Dark system preference", substring = true)).performClick()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("Theme", useUnmergedTree = true).performClick()
            compose.onNodeWithText("● Dark system preference").assertExists()
        } finally {
            compose.onNode(hasText("System system preference", substring = true)).performClick()
        }
    }

    @Test fun cancellingActionPreviewDoesNotExecute() {
        compose.onNodeWithText("Tools", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Command").performScrollTo().performTextInput("dial 5551234")
        compose.onNodeWithText("Preview command").performScrollTo().performClick()
        compose.onNodeWithText("Confirm one action").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Confirm one action").assertDoesNotExist()
        compose.onNodeWithText("Command").assertTextContains("dial 5551234")
    }

    @Suppress("DEPRECATION")
    @Test fun voiceBuildDeclaresOnlyExpectedPermissions() {
        val context = compose.activity
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()
        val forbidden = setOf(
            "android.permission.READ_CONTACTS",
            "android.permission.CALL_PHONE", "android.permission.SEND_SMS", "android.permission.FOREGROUND_SERVICE_MICROPHONE",
        )
        assertTrue("Unexpected permission: ${permissions intersect forbidden}", (permissions intersect forbidden).isEmpty())
        val required=setOf("android.permission.INTERNET", "android.permission.RECORD_AUDIO", "android.permission.MODIFY_AUDIO_SETTINGS")
        assertTrue(permissions.containsAll(required))
        assertTrue((permissions - required - "com.tanimboss.jerryai.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION").isEmpty())
    }
}
