// Copyright (c) 2026 Tanim Boss. See LICENSE.
// Settings designed from the owner's exact two-phone pink / gold UI reference.
package com.tanimboss.jerryai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanimboss.jerryai.BuildConfig
import com.tanimboss.jerryai.settings.ThemeMode
import com.tanimboss.jerryai.voice.VoicePhase

@Composable
internal fun JerryNeonSettingsScreen(
    theme: ThemeMode,
    voicePhase: VoicePhase,
    keyConfigured: Boolean,
    language: String,
    onThemeChange: (ThemeMode) -> Unit,
    onOpen: (String) -> Unit,
    onNotifications: () -> Unit,
) {
    var themeDialog by remember { mutableStateOf(false) }
    val live = homeLiveStatus(voicePhase)
    Column(
        Modifier.fillMaxSize().background(JerryNeon.backgroundGradient)
            .statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹", color = JerryNeon.gold, fontSize = 36.sp,
                modifier = Modifier.clickable { onOpen("HOME") }.padding(end = 16.dp))
            Text("Settings & Personalization", color = Color.White, fontSize = 19.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("ⓘ", color = JerryNeon.gold, fontSize = 22.sp,
                modifier = Modifier.clickable { onOpen("DIAGNOSTICS") }.padding(5.dp))
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 13.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(JerryNeon.panel)
                    .border(1.dp, JerryNeon.edge, RoundedCornerShape(22.dp))
                    .clickable { onOpen("ABOUT") }
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(85.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFF5F2154), Color(0xFF241020))))
                        .border(1.dp, JerryNeon.gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { JerryRobotAvatar(Modifier.size(75.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("♛ Tanim Boss", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Owner & Publisher", color = JerryNeon.dim, fontSize = 12.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Jerry AI Original", color = JerryNeon.gold, fontSize = 11.sp)
                }
                Text("›", color = JerryNeon.gold, fontSize = 25.sp)
            }

            NeonSettingsRow("🎨", "Theme", "App appearance, colors & effects",
                "Pink–Gold Neon", onClick = { themeDialog = true })
            NeonSettingsRow("🎙", "Voice & Wake Word", "Voice, language, persona • wake word planned",
                "Hey Jerry · planned", onClick = { onOpen("VOICE") })
            NeonSettingsRow("✦", "Gemini API / Live Connection", "Manage your encrypted API key and Gemini Live",
                if (live.active) live.label else if (keyConfigured) live.label else "Set up key",
                statusColor = if (live.active) JerryNeon.green else JerryNeon.gold,
                onClick = { onOpen("VOICE") })
            NeonSettingsRow("◉", "Memory & Privacy",
                "Memory Bank is planned; privacy information is available",
                "View", onClick = { onOpen("MEMORY") })
            NeonSettingsRow("♧", "Permissions",
                "See real microphone and Accessibility permission status",
                "Check", onClick = { onOpen("DIAGNOSTICS") })
            NeonSettingsRow("♧", "Notifications", "Open Android notification preferences",
                "Manage", onClick = onNotifications)
            NeonSettingsRow("A文", "Languages",
                "Choose supported Gemini response language",
                language, onClick = { onOpen("VOICE") })
            NeonSettingsRow("⌘", "Automation",
                "Existing one-time confirmations and Accessibility consent",
                "Actions", onClick = { onOpen("ACTIONS") })
            NeonSettingsRow("☁", "Backup & Restore",
                "Memory backup is not implemented yet",
                "Planned", onClick = { onOpen("MEMORY") })
            NeonSettingsRow("ⓘ", "About Jerry",
                "App, version, credits and updates",
                "v" + BuildConfig.VERSION_NAME, onClick = { onOpen("ABOUT") })
            Spacer(Modifier.height(18.dp))
        }
    }
    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("Pink–Gold Neon Appearance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Pink–Gold Neon is the selected app-wide design. Your original Android appearance preference is preserved.")
                    ThemeMode.entries.forEach { mode ->
                        Text(
                            (if (theme == mode) "● " else "○ ") + mode.label + " system preference",
                            modifier = Modifier.fillMaxWidth().clickable {
                                onThemeChange(mode)
                                themeDialog = false
                            }.padding(7.dp),
                        )
                    }
                    Text("The neon app shell stays dark, as in the reference image. Other palette presets are not implemented.", fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun NeonSettingsRow(
    icon: String, title: String, description: String, value: String,
    statusColor: Color = JerryNeon.gold,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 62.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(JerryNeon.panel)
            .border(1.dp, JerryNeon.edge, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 22.sp, color = JerryNeon.gold, maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, fontSize = 10.sp, color = JerryNeon.dim, maxLines = 2, lineHeight = 12.sp)
        }
        Spacer(Modifier.width(5.dp))
        Text(value, fontSize = 10.sp, color = statusColor, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 105.dp))
        Text(" ›", color = JerryNeon.gold, fontSize = 19.sp)
    }
}
