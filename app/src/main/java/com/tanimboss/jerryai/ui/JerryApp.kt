// Copyright (c) 2026 Tanim Boss. See LICENSE.
package com.tanimboss.jerryai.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tanimboss.jerryai.actions.ActionsViewModel
import com.tanimboss.jerryai.automation.AutomationConnection
import com.tanimboss.jerryai.BuildConfig
import com.tanimboss.jerryai.JerryViewModel
import com.tanimboss.jerryai.catalog.FeatureCatalog
import com.tanimboss.jerryai.catalog.ImplementationState
import com.tanimboss.jerryai.diagnostics.readPermissionDiagnostics
import com.tanimboss.jerryai.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B60), secondary = Color(0xFF426660),
    background = Color(0xFFF4FAF7), surface = Color(0xFFF4FAF7),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF74E0CF), secondary = Color(0xFFA9CCC3),
    background = Color(0xFF0C1918), surface = Color(0xFF0C1918),
)
private enum class Page(val title: String) { HOME("Home"), FEATURES("Features"), SETTINGS("Settings"), DIAGNOSTICS("Diagnostics"), ABOUT("About"), ACTIONS("Actions") }

@Composable
fun JerryApp(viewModel: JerryViewModel) {
    val actionsViewModel = viewModel<ActionsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dark = when (state.settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
        var selected by rememberSaveable { mutableStateOf(Page.HOME.name) }
        val page = Page.entries.firstOrNull { it.name == selected } ?: Page.HOME
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("JERRY / ORIGINAL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Jerry AI Assistant", style = MaterialTheme.typography.headlineSmall)
                }
                // Two short rows stay usable on narrow phones and at large font sizes.
                Page.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { destination ->
                            FilterChip(
                                selected = page == destination,
                                onClick = { selected = destination.name },
                                label = { Text(destination.title) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                key(page) {
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        when (page) {
                            Page.HOME -> Home()
                            Page.FEATURES -> Features()
                            Page.SETTINGS -> {
                                Text("Appearance", style = MaterialTheme.typography.headlineSmall)
                                Text("Choose a theme. This preference stays on this device.")
                                if (state.loading) Text("Loading settings…")
                                ThemeMode.entries.forEach { theme ->
                                    FilterChip(
                                        selected = state.settings.theme == theme,
                                        enabled = !state.loading,
                                        onClick = { viewModel.setTheme(theme) },
                                        label = { Text("${theme.label} theme") },
                                    )
                                }
                                Text("Voice, persona, API key, and memory settings arrive with their feature phases. No API key is collected in this build.")
                            }
                            Page.DIAGNOSTICS -> Diagnostics()
                            Page.ABOUT -> About()
                            Page.ACTIONS -> ActionsScreen(actionsViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InformationCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body)
        }
    }
}

@Composable
private fun Home() {
    Text("Your assistant, built independently.", style = MaterialTheme.typography.headlineMedium)
    InformationCard("Foundation build", "Explore settings, diagnostics and Actions. Supported commands use Android intents first and optional, confirmed accessibility automation. Android device verification is still pending.")
    InformationCard("Voice is not active", "Gemini, wake word and background listening are unavailable. Actions can accept a command through Android speech input and require preview/confirmation before execution.")
    val features = FeatureCatalog.features
    val partial = features.count { it.implementation == ImplementationState.PARTIAL }
    val planned = features.count { it.implementation == ImplementationState.PLANNED }
    Text("${features.size} initial feature groups · $partial partial groups · $planned planned", style = MaterialTheme.typography.bodyMedium)
    Text("Android device verification is pending. Automated build evidence is recorded in the repository validation report.")
}

@Composable
private fun Features() {
    Text("Feature inventory", style = MaterialTheme.typography.headlineSmall)
    Text("Initial owner-approved baseline. Historical completeness is unverified. Partial implementation does not mean the full feature works.")
    FeatureCatalog.features.forEach { feature ->
        InformationCard(
            "${feature.id}. ${feature.title}",
            "${feature.implementation.label} · Phase ${feature.phase}\n${feature.limitation}\nDevice evidence: ${feature.deviceEvidence ?: "Not tested"}",
        )
    }
}

@Composable
private fun Diagnostics() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    // Refresh after returning from Android settings, not just after navigation.
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val permissions = remember(revision) { readPermissionDiagnostics(context) }
    fun openSettings(intent: Intent) {
        try { context.startActivity(intent); settingsError = null }
        catch (_: ActivityNotFoundException) { settingsError = "This device does not provide that settings screen." }
        catch (_: SecurityException) { settingsError = "Android blocked access to that settings screen." }
    }
    Text("Permission diagnostics", style = MaterialTheme.typography.headlineSmall)
    Text("Android ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\nApp ${BuildConfig.VERSION_NAME}")
    permissions.forEach { InformationCard(it.title, "${it.state.label}\n${it.reason}") }
    val serviceConnected by AutomationConnection.connected.collectAsStateWithLifecycle()
    val automationAllowed by AutomationConnection.allowed.collectAsStateWithLifecycle()
    InformationCard("Accessibility service", "Declared, user-enabled only. Connected: $serviceConnected. Session automation allowed: $automationAllowed. Configure it on the Actions screen.")
    InformationCard("Screen capture", "Unavailable. Future capture requires Android consent; no screenshots are taken in this build.")
    InformationCard("Background listening", "Unavailable. No microphone service is registered or running in this build.")
    Button(onClick = { revision++ }) { Text("Refresh diagnostics") }
    OutlinedButton(onClick = { openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) }) {
        Text("Open app settings")
    }
    OutlinedButton(onClick = { openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Open accessibility settings") }
    settingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun About() {
    Text("Original contributions", style = MaterialTheme.typography.headlineSmall)
    Text("Original developer: Tanim Boss\nOwner: Tanim Boss\nPublisher: Tanim Boss")
    InformationCard("Privacy in this build", "Only your theme preference is persisted. Commands and accessible screen text are processed in memory for confirmed actions, not retained or logged. Android speech input and launched third-party apps may process data under their own policies. Jerry has no microphone, network, contacts or direct-call permission. Backups and device transfer are excluded by configuration.")
    InformationCard("TANIM BOSS EXCLUSIVE OWNERSHIP LICENSE", "Original materials: Copyright © 2026 Tanim Boss. All rights reserved. Copying, modification, redistribution, or commercial use requires separate authorization from Tanim Boss. Third-party components remain under their own licenses. See LICENSE and THIRD_PARTY_NOTICES.md in the source distribution.")
    InformationCard("Third-party software", "AndroidX and Material components: Apache License 2.0. Kotlin: Apache License 2.0. kotlinx.coroutines: Apache License 2.0. Full notices and license texts are included in the source distribution; no third-party ownership is claimed.")
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    OutlinedButton(onClick = { showLicenses = !showLicenses }) {
        Text(if (showLicenses) "Hide licenses" else "Read full licenses")
    }
    if (showLicenses) {
        val licenses = remember {
            (listOf("LICENSE", "THIRD_PARTY_NOTICES.md") + context.assets.list("licenses").orEmpty().sorted().map { "licenses/$it" })
                .joinToString("\n\n") { path -> context.assets.open(path).bufferedReader().use { it.readText() } }
        }
        Text(licenses, style = MaterialTheme.typography.bodySmall)
    }
    Text("Version ${BuildConfig.VERSION_NAME} · Personal sideload development build")
}
